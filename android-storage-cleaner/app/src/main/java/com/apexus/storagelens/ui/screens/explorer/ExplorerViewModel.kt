package com.apexus.storagelens.ui.screens.explorer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apexus.storagelens.data.delete.DeletionManager
import com.apexus.storagelens.data.delete.DeletionState
import com.apexus.storagelens.data.delete.DeletionTarget
import com.apexus.storagelens.data.prefs.AppSettings
import com.apexus.storagelens.data.prefs.SettingsRepository
import com.apexus.storagelens.data.scan.ScanManager
import com.apexus.storagelens.data.storage.ProtectionProvider
import com.apexus.storagelens.domain.cleanup.ProtectedPaths
import com.apexus.storagelens.domain.model.FileEntry
import com.apexus.storagelens.domain.model.FileNode
import com.apexus.storagelens.domain.model.FileType
import com.apexus.storagelens.domain.model.RiskLevel
import com.apexus.storagelens.domain.model.ScanResult
import com.apexus.storagelens.domain.deletion.MediaRouting
import com.apexus.storagelens.ui.components.DeletionFlow
import com.apexus.storagelens.ui.components.PendingDeletion
import com.apexus.storagelens.ui.components.PendingSystemConfirmation
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

enum class ExplorerTab { LIST, TREEMAP, TOP_FILES }

enum class SortMode { SIZE, COUNT, DATE, NAME }

enum class AgeFilter(val minAgeDays: Int) { ALL(0), OLDER_30(30), OLDER_90(90), OLDER_365(365) }

/** Élément sélectionné manuellement, avec l'état observé lors de l'analyse. */
data class ManualSelection(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val fileCount: Int,
)

data class ExplorerUiState(
    val result: ScanResult? = null,
    val currentPath: String? = null,
    val tab: ExplorerTab = ExplorerTab.LIST,
    val sort: SortMode = SortMode.SIZE,
    val typeFilter: FileType? = null,
    val ageFilter: AgeFilter = AgeFilter.ALL,
    val selection: Map<String, ManualSelection> = emptyMap(),
    val settings: AppSettings = AppSettings(),
    val pendingDeletion: PendingDeletion? = null,
    val protectedPaths: ProtectedPaths? = null,
) {
    val currentNode: FileNode?
        get() = result?.root?.let { root -> currentPath?.let { root.find(it) } ?: root }

    val visibleChildren: List<FileNode>
        get() {
            val node = currentNode ?: return emptyList()
            val filtered = if (settings.showHiddenFiles) node.children else node.children.filterNot { it.name.startsWith(".") }
            return when (sort) {
                SortMode.SIZE -> filtered.sortedByDescending { it.size }
                SortMode.COUNT -> filtered.sortedByDescending { it.fileCount }
                SortMode.DATE -> filtered.sortedByDescending { it.lastModified }
                SortMode.NAME -> filtered.sortedBy { it.name.lowercase() }
            }
        }

    val visibleTopFiles: List<FileEntry>
        get() {
            val now = System.currentTimeMillis()
            val minAge = ageFilter.minAgeDays * 24L * 3600 * 1000
            return result?.topFiles.orEmpty().filter { entry ->
                (typeFilter == null || entry.type == typeFilter) &&
                    (ageFilter == AgeFilter.ALL || now - entry.lastModified >= minAge) &&
                    (settings.showHiddenFiles || !entry.name.startsWith("."))
            }
        }

    /** Chemins du fil d'Ariane, de la racine au dossier courant. */
    val breadcrumb: List<Pair<String, String>>
        get() {
            val scan = result ?: return emptyList()
            val root = scan.root
            val current = currentNode ?: return emptyList()
            val crumbs = mutableListOf(root.path to scan.volume.label)
            val relative = current.path.removePrefix(root.path).trim('/')
            if (relative.isNotEmpty()) {
                var acc = root.path
                relative.split('/').forEach { segment ->
                    acc = "$acc/$segment"
                    crumbs += acc to segment
                }
            }
            return crumbs
        }

    val selectedBytes: Long get() = selection.values.sumOf { it.size }

    fun canDelete(path: String): Boolean = protectedPaths?.canDelete(path) ?: false
}

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val scanManager: ScanManager,
    private val deletionManager: DeletionManager,
    private val protectionProvider: ProtectionProvider,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val local = MutableStateFlow(
        ExplorerUiState(
            currentPath = savedStateHandle.get<String>(KEY_PATH),
            tab = savedStateHandle.get<String>(KEY_TAB)?.let { runCatching { ExplorerTab.valueOf(it) }.getOrNull() } ?: ExplorerTab.LIST,
        )
    )

    val state: StateFlow<ExplorerUiState> = combine(local, scanManager.result, settingsRepository.settings) { base, result, settings ->
        base.copy(result = result, settings = settings)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    val deletionState: StateFlow<DeletionState> = deletionManager.state

    private val deletionFlow = DeletionFlow(viewModelScope, deletionManager)
    val systemConfirmation: StateFlow<PendingSystemConfirmation?> = deletionFlow.pending

    init {
        viewModelScope.launch {
            val guard = protectionProvider.current()
            local.update { it.copy(protectedPaths = guard) }
        }
    }

    fun open(node: FileNode) {
        if (!node.isDirectory) return
        setPath(node.path)
    }

    fun openPath(path: String) = setPath(path)

    /** Remonte d'un niveau ; renvoie faux si l'on est déjà à la racine. */
    fun navigateUp(): Boolean {
        val s = state.value
        val root = s.result?.root ?: return false
        val current = s.currentNode ?: return false
        if (current.path == root.path) return false
        setPath(current.path.substringBeforeLast('/'))
        return true
    }

    private fun setPath(path: String) {
        savedStateHandle[KEY_PATH] = path
        local.update { it.copy(currentPath = path) }
    }

    fun setTab(tab: ExplorerTab) {
        savedStateHandle[KEY_TAB] = tab.name
        local.update { it.copy(tab = tab) }
    }

    fun setSort(sort: SortMode) = local.update { it.copy(sort = sort) }
    fun setTypeFilter(type: FileType?) = local.update { it.copy(typeFilter = type) }
    fun setAgeFilter(age: AgeFilter) = local.update { it.copy(ageFilter = age) }

    fun toggle(node: FileNode) = toggle(ManualSelection(node.path, node.name, node.size, node.lastModified, node.isDirectory, node.fileCount))

    fun toggle(entry: FileEntry) = toggle(ManualSelection(entry.path, entry.name, entry.size, entry.lastModified, false, 1))

    private fun toggle(item: ManualSelection) {
        if (!state.value.canDelete(item.path)) return
        local.update {
            val selection = if (item.path in it.selection) it.selection - item.path else it.selection + (item.path to item)
            it.copy(selection = selection)
        }
    }

    fun clearSelection() = local.update { it.copy(selection = emptyMap()) }

    fun excludeCurrentFolder() {
        val path = state.value.currentNode?.path ?: return
        viewModelScope.launch { settingsRepository.addExclusion(path) }
    }

    fun requestDelete() {
        val s = state.value
        if (s.selection.isEmpty()) return
        val items = s.selection.values.map {
            // Suppression manuelle : risque élevé pour un dossier, moyen pour un fichier.
            PendingItem(
                it.path, it.name, it.size, if (it.isDirectory) RiskLevel.HIGH else RiskLevel.MEDIUM, permanent = false,
                isPhotoOrVideo = !it.isDirectory && MediaRouting.isPhotoOrVideo(it.name),
                isDirectory = it.isDirectory,
            )
        }
        local.update { it.copy(pendingDeletion = PendingDeletion(items, s.settings.trashEnabled)) }
    }

    fun dismissDelete() = local.update { it.copy(pendingDeletion = null) }

    fun confirmDelete() {
        val selection = state.value.selection.values.toList()
        local.update { it.copy(pendingDeletion = null) }
        val targets = selection.map {
            DeletionTarget(
                path = it.path,
                expectedSize = it.size,
                expectedLastModified = if (it.isDirectory) null else it.lastModified,
                isDirectory = it.isDirectory,
                allowTrash = true,
                fileCount = it.fileCount,
            )
        }
        deletionFlow.start(targets) { local.update { it.copy(selection = emptyMap()) } }
    }

    fun onSystemConfirmationShown() = deletionFlow.markShown()
    fun onSystemConfirmationResult(approved: Boolean) = deletionFlow.onSystemResult(approved)

    fun undo(batchId: String) {
        viewModelScope.launch { deletionManager.undo(batchId) }
    }

    fun acknowledgeDeletion() = deletionManager.acknowledge()

    companion object {
        private const val KEY_PATH = "path"
        private const val KEY_TAB = "tab"
    }
}
