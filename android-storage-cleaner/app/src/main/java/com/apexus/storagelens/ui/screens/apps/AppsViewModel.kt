package com.apexus.storagelens.ui.screens.apps

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.apexus.storagelens.data.apps.AppStorageRepository
import com.apexus.storagelens.data.storage.PermissionChecker
import com.apexus.storagelens.domain.apps.AppBatch
import com.apexus.storagelens.domain.apps.AppBatchKind
import com.apexus.storagelens.domain.model.AppStorageInfo
import com.apexus.storagelens.ui.navigation.AppsRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class AppSort { TOTAL, CACHE, NAME }

enum class UsageFilter(val days: Int) { ALL(0), UNUSED_30(30), UNUSED_90(90) }

/** Résultat d'une action groupée, affiché une fois terminée. */
data class AppBatchResult(val kind: AppBatchKind, val processed: Int, val freedBytes: Long, val skipped: Int)

data class AppsUiState(
    val loading: Boolean = true,
    val usageAccess: Boolean = true,
    val apps: List<AppStorageInfo> = emptyList(),
    val sort: AppSort = AppSort.TOTAL,
    val filter: UsageFilter = UsageFilter.ALL,
    val showSystem: Boolean = false,
    val selection: Set<String> = emptySet(),
    val confirmUninstall: Boolean = false,
    val batch: AppBatch? = null,
    val batchResult: AppBatchResult? = null,
) {
    val visible: List<AppStorageInfo>
        get() {
            val now = System.currentTimeMillis()
            val filtered = apps.filter { app ->
                (showSystem || !app.isSystemApp) &&
                    (filter == UsageFilter.ALL || (app.lastUsedMillis ?: 0L) < now - TimeUnit.DAYS.toMillis(filter.days.toLong()))
            }
            return when (sort) {
                AppSort.TOTAL -> filtered.sortedByDescending { it.totalBytes }
                AppSort.CACHE -> filtered.sortedByDescending { it.cacheBytes }
                AppSort.NAME -> filtered.sortedBy { it.label.lowercase() }
            }
        }
    val totalCache: Long get() = apps.sumOf { it.cacheBytes }
    val selectedApps: List<AppStorageInfo> get() = apps.filter { it.packageName in selection }
    val selectedCache: Long get() = selectedApps.sumOf { it.cacheBytes }
    val selectedTotal: Long get() = selectedApps.sumOf { it.totalBytes }
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val repository: AppStorageRepository,
    private val permissions: PermissionChecker,
) : ViewModel() {

    private val route = runCatching { savedStateHandle.toRoute<AppsRoute>() }.getOrDefault(AppsRoute())
    private val _state = MutableStateFlow(AppsUiState(sort = if (route.sortByCache) AppSort.CACHE else AppSort.TOTAL))
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    /** Tailles au démarrage d'une action groupée, pour mesurer ce qui a été libéré. */
    private var sizesBeforeBatch: Map<String, AppStorageInfo> = emptyMap()

    fun refresh() {
        viewModelScope.launch {
            val usage = permissions.hasUsageAccess()
            val apps = repository.appsWithStorage()
            val installed = apps.map { it.packageName }.toSet()
            _state.update { it.copy(loading = false, usageAccess = usage, apps = apps, selection = it.selection intersect installed) }
        }
    }

    fun setSort(sort: AppSort) = _state.update { it.copy(sort = sort) }
    fun setFilter(filter: UsageFilter) = _state.update { it.copy(filter = filter) }
    fun setShowSystem(show: Boolean) = _state.update { it.copy(showSystem = show) }

    fun toggle(packageName: String) = _state.update {
        it.copy(selection = if (packageName in it.selection) it.selection - packageName else it.selection + packageName)
    }

    /** « Tout cocher » : coche toutes les applications affichées, ou les décoche si elles le sont déjà. */
    fun toggleAllVisible() = _state.update { s ->
        val visible = s.visible.map { it.packageName }.toSet()
        if (visible.isEmpty()) return@update s
        val allSelected = visible.all { it in s.selection }
        s.copy(selection = if (allSelected) s.selection - visible else s.selection + visible)
    }

    fun clearSelection() = _state.update { it.copy(selection = emptySet()) }

    fun startClearCache() = startBatch(AppBatchKind.CLEAR_CACHE)

    fun requestUninstall() = _state.update { it.copy(confirmUninstall = true) }
    fun dismissUninstall() = _state.update { it.copy(confirmUninstall = false) }
    fun confirmUninstall() {
        _state.update { it.copy(confirmUninstall = false) }
        startBatch(AppBatchKind.UNINSTALL)
    }

    private fun startBatch(kind: AppBatchKind) {
        val s = _state.value
        if (s.batch != null || s.selection.isEmpty()) return
        val batch = AppBatch.create(kind, s.selectedApps, context.packageName)
        sizesBeforeBatch = s.selectedApps.associateBy { it.packageName }
        if (batch.isFinished) {
            _state.update { it.copy(batchResult = AppBatchResult(kind, 0, 0, batch.skipped)) }
            return
        }
        _state.update { it.copy(batch = batch, batchResult = null) }
    }

    fun onStepLaunched() = _state.update { it.copy(batch = it.batch?.markLaunched()) }

    /** L'utilisateur est revenu de l'écran système : on passe à l'application suivante. */
    fun onStepReturned() {
        val batch = _state.value.batch ?: return
        val next = batch.next()
        if (next.isFinished) finishBatch(next) else _state.update { it.copy(batch = next) }
    }

    fun stopBatch() {
        val batch = _state.value.batch ?: return
        finishBatch(batch.copy(packages = batch.packages.take(batch.index)))
    }

    private fun finishBatch(batch: AppBatch) {
        _state.update { it.copy(batch = null) }
        viewModelScope.launch {
            val apps = repository.appsWithStorage()
            val after = apps.associateBy { it.packageName }
            val processed = batch.packages.take(batch.index)
            val freed = processed.sumOf { pkg ->
                val before = sizesBeforeBatch[pkg] ?: return@sumOf 0L
                when (batch.kind) {
                    AppBatchKind.CLEAR_CACHE -> (before.cacheBytes - (after[pkg]?.cacheBytes ?: 0L)).coerceAtLeast(0)
                    AppBatchKind.UNINSTALL -> if (pkg in after) 0L else before.totalBytes
                }
            }
            val done = when (batch.kind) {
                AppBatchKind.CLEAR_CACHE -> processed.count { pkg -> (after[pkg]?.cacheBytes ?: 0L) < (sizesBeforeBatch[pkg]?.cacheBytes ?: 0L) }
                AppBatchKind.UNINSTALL -> processed.count { it !in after }
            }
            val installed = after.keys
            _state.update {
                it.copy(
                    apps = apps,
                    selection = if (batch.kind == AppBatchKind.UNINSTALL) it.selection intersect installed else emptySet(),
                    batchResult = AppBatchResult(batch.kind, done, freed, batch.skipped),
                )
            }
        }
    }

    fun acknowledgeResult() = _state.update { it.copy(batchResult = null) }
}
