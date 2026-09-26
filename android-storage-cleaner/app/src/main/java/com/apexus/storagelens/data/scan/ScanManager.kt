package com.apexus.storagelens.data.scan

import android.content.Context
import com.apexus.storagelens.data.apps.AppStorageRepository
import com.apexus.storagelens.data.db.HistoryDao
import com.apexus.storagelens.data.db.ScanHistoryEntity
import com.apexus.storagelens.data.delete.TrashRepository
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.data.storage.PermissionChecker
import com.apexus.storagelens.data.storage.StorageVolumesRepository
import com.apexus.storagelens.di.ApplicationScope
import com.apexus.storagelens.domain.cleanup.CleanupAnalyzer
import com.apexus.storagelens.domain.cleanup.ProtectedPaths
import com.apexus.storagelens.domain.cleanup.RuleContext
import com.apexus.storagelens.domain.cleanup.rules.DuplicatesRule
import com.apexus.storagelens.domain.model.CategoryUsage
import com.apexus.storagelens.domain.model.ScanPhase
import com.apexus.storagelens.domain.model.ScanResult
import com.apexus.storagelens.domain.model.ScanState
import com.apexus.storagelens.domain.model.StorageCategory
import com.apexus.storagelens.domain.scan.CategoryCollector
import com.apexus.storagelens.domain.scan.CompositeVisitor
import com.apexus.storagelens.domain.scan.FileScanner
import com.apexus.storagelens.domain.scan.RemovedEntry
import com.apexus.storagelens.domain.scan.ScanCounters
import com.apexus.storagelens.domain.scan.ScanOptions
import com.apexus.storagelens.domain.scan.SizeIndexCollector
import com.apexus.storagelens.domain.scan.TopFilesCollector
import com.apexus.storagelens.domain.scan.TreeOps
import com.apexus.storagelens.domain.selection.SystemSpace
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Orchestration de l'analyse. L'analyse tourne dans la portée applicative : quitter l'écran
 * ne l'interrompt pas, seul [cancel] le fait. Le dernier résultat est conservé en mémoire.
 */
@Singleton
class ScanManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
    private val volumes: StorageVolumesRepository,
    private val apps: AppStorageRepository,
    private val settings: SettingsRepository,
    private val trash: TrashRepository,
    private val historyDao: HistoryDao,
    private val permissions: PermissionChecker,
) {
    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val _result = MutableStateFlow<ScanResult?>(null)
    val result: StateFlow<ScanResult?> = _result.asStateFlow()

    /** Vrai lorsque des fichiers ont été restaurés : le résultat affiché n'est plus exact. */
    private val _stale = MutableStateFlow(false)
    val stale: StateFlow<Boolean> = _stale.asStateFlow()

    private var job: Job? = null

    val isRunning: Boolean get() = job?.isActive == true

    fun start(volumeId: String? = null) {
        if (isRunning) return
        // État « en cours » publié immédiatement pour que l'écran d'analyse ne se ferme pas.
        _state.value = ScanState.Running("", 0, 0, 0f, ScanPhase.LISTING)
        job = scope.launch {
            try {
                val result = scan(volumeId) { running -> _state.value = running }
                _result.value = result
                _stale.value = false
                _state.value = ScanState.Done(result)
            } catch (e: CancellationException) {
                _state.value = ScanState.Idle
                throw e
            } catch (e: Exception) {
                _state.value = ScanState.Failed(e.message ?: e.javaClass.simpleName)
            }
        }
    }

    fun cancel() {
        job?.cancel()
        job = null
        _state.value = ScanState.Idle
    }

    /** Analyse complète d'un volume. Utilisée par l'UI et par le scan planifié. */
    suspend fun scan(volumeId: String?, onProgress: (ScanState.Running) -> Unit = {}): ScanResult = coroutineScope {
        val startedAt = System.currentTimeMillis()
        val volume = volumes.volume(volumeId) ?: error("Aucun volume de stockage disponible")
        val root = volume.rootPath ?: error("Volume sans point de montage accessible")
        val appSettings = settings.current()
        val installed = apps.installedPackages()
        val protectedPaths = ProtectedPaths(
            storageRoots = volumes.storageRoots(),
            ownPackage = context.packageName,
            userExclusions = appSettings.exclusions,
            installedPackages = installed.ifEmpty { null },
        )
        val ruleContext = RuleContext(
            storageRoot = root,
            protectedPaths = protectedPaths,
            installedPackages = installed,
            nowMillis = startedAt,
            largeFileThresholdBytes = appSettings.largeFileThresholdBytes,
            oldFileAgeMillis = appSettings.oldFileAgeMillis,
            apkPackageResolver = apps::apkPackageName,
        )

        val categories = CategoryCollector(root)
        val topFiles = TopFilesCollector(limit = 100)
        val sizes = SizeIndexCollector()
        val analyzer = CleanupAnalyzer(ruleContext)
        val counters = ScanCounters()
        val options = ScanOptions(excludedPaths = appSettings.exclusions + trash.trashDirectories())

        val usedBytes = volume.usedBytes.coerceAtLeast(1)
        val ticker = launch {
            while (isActive) {
                onProgress(
                    ScanState.Running(
                        currentPath = counters.currentPath.removePrefix(root),
                        filesScanned = counters.files.get(),
                        bytesScanned = counters.bytes.get(),
                        fraction = (counters.bytes.get().toFloat() / usedBytes).coerceIn(0f, 0.99f),
                        phase = ScanPhase.LISTING,
                    )
                )
                delay(PROGRESS_INTERVAL_MS)
            }
        }

        val tree = try {
            FileScanner().scan(root, options, CompositeVisitor(listOf(categories, topFiles, sizes, analyzer)), counters)
        } finally {
            ticker.cancel()
        }

        onProgress(ScanState.Running("", counters.files.get(), counters.bytes.get(), null, ScanPhase.DUPLICATES))
        val groups = analyzer.finish(listOf(DuplicatesRule(sizes::sameSizeGroups)))

        onProgress(ScanState.Running("", counters.files.get(), counters.bytes.get(), null, ScanPhase.ANALYZING))
        val categoryList = buildCategories(volume.isPrimary, categories.result(), volume.totalBytes, volume.freeBytes)

        val result = ScanResult(
            volume = volume,
            root = tree,
            topFiles = topFiles.result(),
            categories = categoryList,
            cleanupGroups = groups,
            inaccessibleDirectories = counters.inaccessibleDirectories.get(),
            durationMs = System.currentTimeMillis() - startedAt,
            finishedAt = System.currentTimeMillis(),
        )
        historyDao.insertScan(
            ScanHistoryEntity(
                finishedAt = result.finishedAt,
                volumeLabel = volume.label,
                totalBytes = volume.totalBytes,
                usedBytes = volume.usedBytes,
                scannedBytes = tree.size,
                fileCount = tree.fileCount,
                recoverableBytes = result.recoverableBytes,
                durationMs = result.durationMs,
            )
        )
        result
    }

    private suspend fun buildCategories(
        isPrimary: Boolean,
        fileCategories: List<CategoryUsage>,
        totalBytes: Long,
        freeBytes: Long,
    ): List<CategoryUsage> {
        val list = fileCategories.toMutableList()
        if (isPrimary && permissions.hasUsageAccess()) {
            val appsBytes = runCatching { apps.appsWithStorage().sumOf { it.totalBytes } }.getOrDefault(0L)
            if (appsBytes > 0) list += CategoryUsage(StorageCategory.APPS, appsBytes)
        }
        val measured = list.sumOf { it.bytes }
        list += CategoryUsage(StorageCategory.SYSTEM, SystemSpace.compute(totalBytes, freeBytes, measured))
        return list.filter { it.bytes > 0 }.sortedByDescending { it.bytes }
    }

    /** Met à jour le résultat après une suppression, sans relancer l'analyse. */
    suspend fun onItemsRemoved(removed: List<RemovedEntry>) {
        if (removed.isEmpty()) return
        val current = _result.value ?: return
        val normalized = TreeOps.normalize(removed)
        val removedDirs = normalized.map { it.path }
        fun isRemoved(path: String) = removedDirs.any { path == it || path.startsWith("$it/") }

        val refreshedVolume = volumes.volume(current.volume.id) ?: current.volume
        val updated = current.copy(
            volume = refreshedVolume,
            root = TreeOps.remove(current.root, normalized),
            topFiles = current.topFiles.filterNot { isRemoved(it.path) },
            cleanupGroups = current.cleanupGroups
                .map { g -> g.copy(candidates = g.candidates.filterNot { isRemoved(it.path) }) }
                .filter { it.candidates.isNotEmpty() },
        )
        _result.value = updated
        _state.update { if (it is ScanState.Done) ScanState.Done(updated) else it }
    }

    fun markStale() {
        _stale.value = true
    }

    companion object {
        private const val PROGRESS_INTERVAL_MS = 150L
    }
}
