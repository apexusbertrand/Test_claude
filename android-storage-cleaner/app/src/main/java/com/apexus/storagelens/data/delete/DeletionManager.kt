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

/**
 * Élément (fichier ou dossier) dont les photos/vidéos passent par la confirmation d'Android.
 * [mediaPaths] : le fichier lui-même, ou les médias indexés contenus dans le dossier.
 */
data class SystemConfirmedTarget(
    val target: DeletionTarget,
    val toTrash: Boolean,
    val mediaPaths: List<String>,
)

/** Une fenêtre de confirmation d'Android (au plus [MAX_URIS_PER_REQUEST] médias). */
data class MediaConfirmationRequest(
    val toTrash: Boolean,
    /** Éléments dont au moins un média figure dans cette demande. */
    val targetPaths: Set<String>,
    val mediaCount: Int,
    val intentSender: IntentSender,
)

/** Lot vérifié, prêt à être exécuté une fois les confirmations système obtenues. */
data class DeletionPlan(
    val batchId: String,
    val direct: List<DeletionTarget>,
    val systemTargets: List<SystemConfirmedTarget>,
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
        val index = HashMap(mediaStoreSync.findPhotosAndVideos(candidates))
        val direct = ArrayList<DeletionTarget>()
        val systemTargets = ArrayList<SystemConfirmedTarget>()
        valid.forEach { target ->
            if (target.isDirectory) {
                val inside = mediaStoreSync.findPhotosAndVideosUnder(target.path)
                index += inside
                val route = MediaRouting.routeDirectory(
                    allowTrash = target.allowTrash,
                    trashEnabled = trashEnabled,
                    systemConfirmationSupported = mediaStoreSync.systemConfirmationSupported,
                    indexedMediaCount = inside.size,
                )
                if (route == DeletionRoute.DIRECT) {
                    direct += target
                } else {
                    // Les médias déjà dans la corbeille Android n'ont pas besoin d'y être renvoyés.
                    val media = inside.filterValues { !(route == DeletionRoute.SYSTEM_TRASH && it.entry.isTrashed) }.keys.sorted()
                    if (media.isEmpty()) direct += target
                    else systemTargets += SystemConfirmedTarget(target, route == DeletionRoute.SYSTEM_TRASH, media)
                }
            } else {
                val route = MediaRouting.route(
                    name = File(target.path).name,
                    isDirectory = false,
                    allowTrash = target.allowTrash,
                    trashEnabled = trashEnabled,
                    systemConfirmationSupported = mediaStoreSync.systemConfirmationSupported,
                    index = index[target.path]?.entry,
                )
                when (route) {
                    DeletionRoute.SYSTEM_TRASH -> systemTargets += SystemConfirmedTarget(target, true, listOf(target.path))
                    DeletionRoute.SYSTEM_DELETE -> systemTargets += SystemConfirmedTarget(target, false, listOf(target.path))
                    DeletionRoute.DIRECT -> direct += target
                }
            }
        }

        // Une demande par type (corbeille / suppression), découpée pour rester sous la taille maximale.
        val requests = ArrayList<MediaConfirmationRequest>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            listOf(true, false).forEach { toTrash ->
                systemTargets.filter { it.toTrash == toTrash }
                    .flatMap { st -> st.mediaPaths.map { path -> st.target.path to path } }
                    .chunked(MAX_URIS_PER_REQUEST)
                    .forEach { chunk ->
                        requests += MediaConfirmationRequest(
                            toTrash = toTrash,
                            targetPaths = chunk.map { it.first }.toSet(),
                            mediaCount = chunk.size,
                            intentSender = mediaStoreSync.confirmationRequest(chunk.map { index.getValue(it.second).uri }, toTrash),
                        )
                    }
            }
        }

        DeletionPlan(
            batchId = UUID.randomUUID().toString(),
            direct = direct,
            systemTargets = systemTargets,
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

            // Photos/vidéos : Android les a déjà traitées si l'utilisateur a confirmé toutes les
            // fenêtres concernant l'élément. Sinon, l'élément est conservé tel quel.
            plan.systemTargets.forEach { st ->
                val approved = plan.mediaRequests.withIndex()
                    .filter { st.target.path in it.value.targetPaths }
                    .all { approvals.getOrElse(it.index) { false } }
                val handledMedia = st.mediaPaths.count { !File(it).exists() }
                if (!approved) {
                    refused += st.mediaPaths.size - handledMedia
                    if (st.toTrash) systemTrashed += handledMedia
                    return@forEach
                }
                if (st.toTrash) systemTrashed += handledMedia
                val target = st.target
                val file = File(target.path)
                if (!target.isDirectory) {
                    if (file.exists()) failures += DeletionFailure(target.path, REASON_IO)
                    else removed += RemovedEntry(target.path, target.expectedSize, target.fileCount)
                    return@forEach
                }
                // Dossier : ses médias sont traités par Android ; on s'occupe du reste de son contenu.
                val problem = if (!guard.canDelete(target.path)) REASON_PROTECTED else null
                val ok = when {
                    problem != null -> false
                    !file.exists() -> true
                    st.toTrash -> trash.moveContentsToTrash(file, plan.batchId) { MediaRouting.isInAndroidTrash(it.name) }
                        .onSuccess { moved ->
                            if (moved.isNotEmpty()) movedToTrash = true
                            deletedMedia += moved
                        }.isSuccess
                    else -> file.deleteRecursively().also { mediaStoreSync.forgetDirectory(target.path) }
                }
                if (ok) removed += RemovedEntry(target.path, target.expectedSize, target.fileCount)
                else failures += DeletionFailure(target.path, problem ?: REASON_IO)
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
        /** Nombre de médias par fenêtre de confirmation (limite de taille des transactions Android). */
        const val MAX_URIS_PER_REQUEST = 1000
        const val REASON_PROTECTED = "protected"
        const val REASON_MISSING = "missing"
        const val REASON_CHANGED = "changed"
        const val REASON_IO = "io"
    }
}
