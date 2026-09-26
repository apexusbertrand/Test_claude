package com.apexus.storagelens.data.delete

import com.apexus.storagelens.data.db.CleanupHistoryEntity
import com.apexus.storagelens.data.db.HistoryDao
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.data.scan.ScanManager
import com.apexus.storagelens.data.storage.ProtectionProvider
import com.apexus.storagelens.data.storage.StorageVolumesRepository
import com.apexus.storagelens.domain.model.DeletionFailure
import com.apexus.storagelens.domain.model.DeletionReport
import com.apexus.storagelens.domain.model.FileTypes
import com.apexus.storagelens.domain.model.FileType
import com.apexus.storagelens.domain.scan.RemovedEntry
import com.apexus.storagelens.domain.scan.TreeOps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Élément à supprimer, tel qu'il était au moment de l'analyse. */
data class DeletionTarget(
    val path: String,
    val expectedSize: Long,
    val expectedLastModified: Long?,
    val isDirectory: Boolean,
    /** Faux pour les logs, caches et temporaires : supprimés définitivement. */
    val allowTrash: Boolean,
    val fileCount: Int = 1,
)

sealed interface DeletionState {
    data object Idle : DeletionState
    data class Running(val done: Int, val total: Int, val currentName: String) : DeletionState
    data class Finished(val report: DeletionReport) : DeletionState
}

/**
 * Exécute les suppressions demandées par l'utilisateur. Chaque élément est revérifié
 * (garde-fou, existence, taille/date inchangées) juste avant d'être traité ; un échec
 * n'interrompt pas le lot.
 */
@Singleton
class DeletionManager @Inject constructor(
    private val trash: TrashRepository,
    private val mediaStoreSync: MediaStoreSync,
    private val settings: SettingsRepository,
    private val volumes: StorageVolumesRepository,
    private val protection: ProtectionProvider,
    private val historyDao: HistoryDao,
    private val scanManager: ScanManager,
) {
    private val _state = MutableStateFlow<DeletionState>(DeletionState.Idle)
    val state: StateFlow<DeletionState> = _state.asStateFlow()
    private val mutex = Mutex()

    suspend fun delete(targets: List<DeletionTarget>): DeletionReport = mutex.withLock {
        withContext(Dispatchers.IO) {
            val batchId = UUID.randomUUID().toString()
            val useTrash = settings.current().trashEnabled
            val guard = protection.current()
            val ordered = normalize(targets)
            val roots = volumes.storageRoots()
            val touchedRoots = ordered.mapNotNull { t -> roots.filter { t.path.startsWith("$it/") }.maxByOrNull { it.length } }.distinct()
            val freeBefore = touchedRoots.sumOf { volumes.availableBytes(it) }

            val failures = ArrayList<DeletionFailure>()
            val removed = ArrayList<RemovedEntry>()
            val deletedFiles = ArrayList<String>()
            var movedToTrash = false

            ordered.forEachIndexed { index, target ->
                currentCoroutineContext().ensureActive()
                val file = File(target.path)
                _state.value = DeletionState.Running(index, ordered.size, file.name)

                val problem = when {
                    !guard.canDelete(target.path) -> REASON_PROTECTED
                    !file.exists() -> REASON_MISSING
                    !target.isDirectory && file.length() != target.expectedSize -> REASON_CHANGED
                    !target.isDirectory && target.expectedLastModified != null &&
                        file.lastModified() != target.expectedLastModified -> REASON_CHANGED
                    else -> null
                }
                if (problem != null) {
                    failures += DeletionFailure(target.path, problem)
                    return@forEachIndexed
                }

                val ok = if (useTrash && target.allowTrash) {
                    trash.moveToTrash(file, batchId).onSuccess { movedToTrash = true }.isSuccess
                } else {
                    file.deleteRecursively()
                }

                if (ok) {
                    removed += RemovedEntry(target.path, target.expectedSize, target.fileCount)
                    if (target.isDirectory) mediaStoreSync.forgetDirectory(target.path)
                    else if (FileTypes.of(file.name) != FileType.OTHER) deletedFiles += target.path
                } else {
                    failures += DeletionFailure(target.path, REASON_IO)
                }
            }

            mediaStoreSync.rescan(deletedFiles)
            val freeAfter = touchedRoots.sumOf { volumes.availableBytes(it) }
            val report = DeletionReport(
                requestedCount = ordered.size,
                deletedCount = removed.size,
                measuredFreedBytes = (freeAfter - freeBefore).coerceAtLeast(0),
                expectedFreedBytes = removed.sumOf { it.size },
                movedToTrash = movedToTrash,
                batchId = batchId,
                failures = failures,
            )
            if (removed.isNotEmpty()) {
                historyDao.insertCleanup(
                    CleanupHistoryEntity(
                        performedAt = System.currentTimeMillis(),
                        itemCount = removed.size,
                        freedBytes = if (movedToTrash) report.expectedFreedBytes else report.measuredFreedBytes.coerceAtLeast(report.expectedFreedBytes),
                        movedToTrash = movedToTrash,
                    )
                )
                scanManager.onItemsRemoved(removed)
            }
            _state.value = DeletionState.Finished(report)
            report
        }
    }

    /** Annule un lot envoyé dans la corbeille interne. */
    suspend fun undo(batchId: String): Int {
        val restored = trash.restoreBatch(batchId)
        if (restored > 0) scanManager.markStale()
        return restored
    }

    fun acknowledge() {
        _state.value = DeletionState.Idle
    }

    private fun normalize(targets: List<DeletionTarget>): List<DeletionTarget> {
        val keep = TreeOps.normalize(targets.map { RemovedEntry(it.path, it.expectedSize, it.fileCount) }).map { it.path }.toSet()
        return targets.distinctBy { it.path }.filter { it.path in keep }
    }

    companion object {
        const val REASON_PROTECTED = "protected"
        const val REASON_MISSING = "missing"
        const val REASON_CHANGED = "changed"
        const val REASON_IO = "io"
    }
}
