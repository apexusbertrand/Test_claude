package com.apexus.storagelens.data.delete

import android.content.IntentSender
import android.os.Build
import com.apexus.storagelens.data.db.CleanupHistoryEntity
import com.apexus.storagelens.data.db.HistoryDao
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.data.scan.ScanManager
import com.apexus.storagelens.data.storage.ProtectionProvider
import com.apexus.storagelens.data.storage.StorageVolumesRepository
import com.apexus.storagelens.domain.cleanup.ProtectedPaths
import com.apexus.storagelens.domain.deletion.DeletionRoute
import com.apexus.storagelens.domain.deletion.MediaRouting
import com.apexus.storagelens.domain.model.DeletionFailure
import com.apexus.storagelens.domain.model.DeletionReport
import com.apexus.storagelens.domain.model.FileType
import com.apexus.storagelens.domain.model.FileTypes
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

/** Photos/vidéos soumises à la fenêtre de confirmation d'Android. */
data class MediaConfirmationRequest(
    val toTrash: Boolean,
    val targets: List<DeletionTarget>,
    val intentSender: IntentSender,
)

/** Lot vérifié, prêt à être exécuté une fois les confirmations système obtenues. */
data class DeletionPlan(
    val batchId: String,
    val direct: List<DeletionTarget>,
    val mediaRequests: List<MediaConfirmationRequest>,
    val rejected: List<DeletionFailure>,
    val requestedCount: Int,
    val touchedRoots: List<String>,
    val freeBefore: Long,
)

sealed interface DeletionState {
    data object Idle : DeletionState
    data class Running(val done: Int, val total: Int, val currentName: String) : DeletionState
    data class Finished(val report: DeletionReport) : DeletionState
}

/**
 * Exécute les suppressions demandées par l'utilisateur, en deux temps :
 * 1. [plan] revérifie chaque élément (garde-fou, existence, taille/date inchangées) et prépare
 *    la fenêtre de confirmation d'Android pour les photos et vidéos (Android 11+) ;
 * 2. [execute] traite le reste et comptabilise les médias que l'utilisateur a confirmés.
 * Un échec n'interrompt jamais le lot.
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

    suspend fun plan(targets: List<DeletionTarget>): DeletionPlan = withContext(Dispatchers.IO) {
        val trashEnabled = settings.current().trashEnabled
        val guard = protection.current()
        val ordered = normalize(targets)
        val roots = volumes.storageRoots()
        val touchedRoots = ordered.mapNotNull { t -> roots.filter { t.path.startsWith("$it/") }.maxByOrNull { it.length } }.distinct()
        val freeBefore = touchedRoots.sumOf { volumes.availableBytes(it) }

        val rejected = ArrayList<DeletionFailure>()
        val valid = ordered.filter { target ->
            val problem = check(target, guard)
            if (problem != null) rejected += DeletionFailure(target.path, problem)
            problem == null
        }

        val candidates = valid.filter { !it.isDirectory && MediaRouting.isPhotoOrVideo(File(it.path).name) }.map { it.path }
        val index = mediaStoreSync.findPhotosAndVideos(candidates)
        val direct = ArrayList<DeletionTarget>()
        val toTrash = ArrayList<DeletionTarget>()
        val toDelete = ArrayList<DeletionTarget>()
        valid.forEach { target ->
            val route = MediaRouting.route(
                name = File(target.path).name,
                isDirectory = target.isDirectory,
                allowTrash = target.allowTrash,
                trashEnabled = trashEnabled,
                systemConfirmationSupported = mediaStoreSync.systemConfirmationSupported,
                index = index[target.path]?.entry,
            )
            when (route) {
                DeletionRoute.SYSTEM_TRASH -> toTrash += target
                DeletionRoute.SYSTEM_DELETE -> toDelete += target
                DeletionRoute.DIRECT -> direct += target
            }
        }

        val requests = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (toTrash.isNotEmpty()) {
                    add(MediaConfirmationRequest(true, toTrash, mediaStoreSync.confirmationRequest(toTrash.map { index.getValue(it.path).uri }, toTrash = true)))
                }
                if (toDelete.isNotEmpty()) {
                    add(MediaConfirmationRequest(false, toDelete, mediaStoreSync.confirmationRequest(toDelete.map { index.getValue(it.path).uri }, toTrash = false)))
                }
            }
        }

        DeletionPlan(
            batchId = UUID.randomUUID().toString(),
            direct = direct,
            mediaRequests = requests,
            rejected = rejected,
            requestedCount = ordered.size,
            touchedRoots = touchedRoots,
            freeBefore = freeBefore,
        )
    }

    /**
     * Exécute un lot. [approvals] indique, pour chaque demande de [DeletionPlan.mediaRequests]
     * (dans l'ordre), si l'utilisateur l'a acceptée dans la fenêtre système.
     */
    suspend fun execute(plan: DeletionPlan, approvals: List<Boolean>): DeletionReport = mutex.withLock {
        withContext(Dispatchers.IO) {
            val useTrash = settings.current().trashEnabled
            val guard = protection.current()
            val failures = ArrayList(plan.rejected)
            val removed = ArrayList<RemovedEntry>()
            val deletedMedia = ArrayList<String>()
            var movedToTrash = false
            var systemTrashed = 0
            var refused = 0

            // Photos/vidéos : Android les a déjà traitées si l'utilisateur a confirmé.
            plan.mediaRequests.forEachIndexed { i, request ->
                if (approvals.getOrElse(i) { false }) {
                    request.targets.forEach { target ->
                        if (File(target.path).exists()) {
                            failures += DeletionFailure(target.path, REASON_IO)
                        } else {
                            removed += RemovedEntry(target.path, target.expectedSize, target.fileCount)
                            if (request.toTrash) systemTrashed++
                        }
                    }
                } else {
                    refused += request.targets.size
                }
            }

            plan.direct.forEachIndexed { index, target ->
                currentCoroutineContext().ensureActive()
                val file = File(target.path)
                _state.value = DeletionState.Running(index, plan.direct.size, file.name)

                // Nouvelle vérification : du temps a pu s'écouler pendant les confirmations.
                val problem = check(target, guard)
                if (problem != null) {
                    failures += DeletionFailure(target.path, problem)
                    return@forEachIndexed
                }

                val ok = if (useTrash && target.allowTrash) {
                    trash.moveToTrash(file, plan.batchId).onSuccess { movedToTrash = true }.isSuccess
                } else {
                    file.deleteRecursively()
                }

                if (ok) {
                    removed += RemovedEntry(target.path, target.expectedSize, target.fileCount)
                    if (target.isDirectory) mediaStoreSync.forgetDirectory(target.path)
                    else if (FileTypes.of(file.name) != FileType.OTHER) deletedMedia += target.path
                } else {
                    failures += DeletionFailure(target.path, REASON_IO)
                }
            }

            mediaStoreSync.rescan(deletedMedia)
            val freeAfter = plan.touchedRoots.sumOf { volumes.availableBytes(it) }
            val report = DeletionReport(
                requestedCount = plan.requestedCount,
                deletedCount = removed.size,
                measuredFreedBytes = (freeAfter - plan.freeBefore).coerceAtLeast(0),
                expectedFreedBytes = removed.sumOf { it.size },
                movedToTrash = movedToTrash,
                batchId = plan.batchId,
                failures = failures,
                systemTrashedCount = systemTrashed,
                refusedCount = refused,
            )
            if (removed.isNotEmpty()) {
                val anyTrash = movedToTrash || systemTrashed > 0
                historyDao.insertCleanup(
                    CleanupHistoryEntity(
                        performedAt = System.currentTimeMillis(),
                        itemCount = removed.size,
                        freedBytes = if (anyTrash) report.expectedFreedBytes else report.measuredFreedBytes.coerceAtLeast(report.expectedFreedBytes),
                        movedToTrash = anyTrash,
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

    private fun check(target: DeletionTarget, guard: ProtectedPaths): String? {
        val file = File(target.path)
        return when {
            !guard.canDelete(target.path) -> REASON_PROTECTED
            !file.exists() -> REASON_MISSING
            !target.isDirectory && file.length() != target.expectedSize -> REASON_CHANGED
            !target.isDirectory && target.expectedLastModified != null &&
                file.lastModified() != target.expectedLastModified -> REASON_CHANGED
            else -> null
        }
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
