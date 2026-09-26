package com.apexus.storagelens.ui.screens.cleanup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexus.storagelens.data.apps.AppStorageRepository
import com.apexus.storagelens.data.delete.DeletionManager
import com.apexus.storagelens.data.delete.DeletionState
import com.apexus.storagelens.data.delete.DeletionTarget
import com.apexus.storagelens.data.prefs.AppSettings
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.data.privileged.PrivilegedArea
import com.apexus.storagelens.data.privileged.PrivilegedCleaner
import com.apexus.storagelens.data.scan.ScanManager
import com.apexus.storagelens.data.storage.PermissionChecker
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.model.CleanupGroup
import com.apexus.storagelens.domain.model.ScanResult
import com.apexus.storagelens.domain.selection.Selection
import com.apexus.storagelens.ui.components.PendingDeletion
import com.apexus.storagelens.ui.components.PendingItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Catégories nettoyées définitivement (sans passer par la corbeille interne). */
private val PERMANENT_CATEGORIES = setOf(
    CleanupCategory.LOGS,
    CleanupCategory.TRACES,
    CleanupCategory.TEMP_FILES,
    CleanupCategory.CACHES,
    CleanupCategory.THUMBNAILS,
    CleanupCategory.EMPTY_FOLDERS,
)

data class AppCacheInfo(val totalCacheBytes: Long, val appCount: Int)

data class PrivilegedInfo(val available: Boolean, val areas: List<PrivilegedArea>)

data class CleanupUiState(
    val result: ScanResult? = null,
    val selection: Set<String> = emptySet(),
    val expanded: Set<CleanupCategory> = emptySet(),
    val settings: AppSettings = AppSettings(),
    val pendingDeletion: PendingDeletion? = null,
    val usageAccess: Boolean = false,
    val appCache: AppCacheInfo? = null,
    val privileged: PrivilegedInfo? = null,
    val privilegedBusy: Boolean = false,
) {
    val groups: List<CleanupGroup> get() = result?.cleanupGroups.orEmpty()
    val selectedBytes: Long get() = Selection.selectedBytes(groups, selection)
    val selectedCount: Int get() = Selection.selectedItems(groups, selection).size
}

@HiltViewModel
class CleanupViewModel @Inject constructor(
    private val scanManager: ScanManager,
    private val deletionManager: DeletionManager,
    private val appStorage: AppStorageRepository,
    private val permissions: PermissionChecker,
    private val privilegedCleaner: PrivilegedCleaner,
    settingsRepository: SettingsRepository,
) : ViewModel() {

    private val local = MutableStateFlow(CleanupUiState())
    private var selectionInitializedFor: Long? = null

    val state: StateFlow<CleanupUiState> = combine(local, scanManager.result, settingsRepository.settings) { base, result, settings ->
        base.copy(result = result, settings = settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CleanupUiState())

    val deletionState: StateFlow<DeletionState> = deletionManager.state

    init {
        // Sélection initiale recalculée à chaque nouvelle analyse.
        viewModelScope.launch {
            scanManager.result.collect { result ->
                if (result == null) return@collect
                if (selectionInitializedFor != result.finishedAt) {
                    selectionInitializedFor = result.finishedAt
                    local.update { it.copy(selection = Selection.defaultSelection(result.cleanupGroups)) }
                } else {
                    // Après une suppression, on retire de la sélection ce qui n'existe plus.
                    val existing = Selection.all(result.cleanupGroups)
                    local.update { it.copy(selection = it.selection intersect existing) }
                }
            }
        }
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                if (settings.advancedRootMode && local.value.privileged == null) refreshPrivileged()
                if (!settings.advancedRootMode) local.update { it.copy(privileged = null) }
            }
        }
    }

    fun refreshAppCache() {
        viewModelScope.launch {
            val usage = permissions.hasUsageAccess()
            val info = if (usage) {
                val apps = appStorage.appsWithStorage()
                AppCacheInfo(apps.sumOf { it.cacheBytes }, apps.count { it.cacheBytes > 0 })
            } else null
            local.update { it.copy(usageAccess = usage, appCache = info) }
        }
    }

    private fun refreshPrivileged() {
        viewModelScope.launch {
            local.update { it.copy(privilegedBusy = true) }
            val available = privilegedCleaner.isAvailable()
            val areas = if (available) privilegedCleaner.measure() else emptyList()
            local.update { it.copy(privileged = PrivilegedInfo(available, areas), privilegedBusy = false) }
        }
    }

    fun cleanPrivileged() {
        viewModelScope.launch {
            local.update { it.copy(privilegedBusy = true) }
            privilegedCleaner.clean()
            val areas = privilegedCleaner.measure()
            local.update { it.copy(privileged = PrivilegedInfo(true, areas), privilegedBusy = false) }
        }
    }

    fun toggleExpanded(category: CleanupCategory) = local.update {
        it.copy(expanded = if (category in it.expanded) it.expanded - category else it.expanded + category)
    }

    fun toggleGroup(group: CleanupGroup) = local.update { it.copy(selection = Selection.toggle(group.candidates, it.selection)) }

    fun toggleParent(group: CleanupGroup, parent: String) = local.update {
        it.copy(selection = Selection.toggle(group.candidates.filter { c -> c.parentPath == parent }, it.selection))
    }

    fun toggleItem(path: String) = local.update { it.copy(selection = Selection.toggleOne(path, it.selection)) }

    fun selectRecommended() = local.update { it.copy(selection = Selection.recommended(state.value.groups)) }
    fun selectAll() = local.update { it.copy(selection = Selection.all(state.value.groups)) }
    fun selectNone() = local.update { it.copy(selection = emptySet()) }

    fun requestDelete() {
        val s = state.value
        val items = Selection.selectedItems(s.groups, s.selection).map {
            PendingItem(it.path, it.name, it.size, it.category.risk, permanent = it.category in PERMANENT_CATEGORIES)
        }
        if (items.isEmpty()) return
        local.update { it.copy(pendingDeletion = PendingDeletion(items, s.settings.trashEnabled)) }
    }

    fun dismissDelete() = local.update { it.copy(pendingDeletion = null) }

    fun confirmDelete() {
        val s = state.value
        val targets = Selection.selectedItems(s.groups, s.selection).map {
            DeletionTarget(
                path = it.path,
                expectedSize = it.size,
                expectedLastModified = if (it.isDirectory) null else it.lastModified,
                isDirectory = it.isDirectory,
                allowTrash = it.category !in PERMANENT_CATEGORIES,
                fileCount = if (it.isDirectory) s.result?.root?.find(it.path)?.fileCount ?: 0 else 1,
            )
        }
        local.update { it.copy(pendingDeletion = null) }
        viewModelScope.launch { deletionManager.delete(targets) }
    }

    fun undo(batchId: String) {
        viewModelScope.launch { deletionManager.undo(batchId) }
    }

    fun acknowledgeDeletion() = deletionManager.acknowledge()
}
