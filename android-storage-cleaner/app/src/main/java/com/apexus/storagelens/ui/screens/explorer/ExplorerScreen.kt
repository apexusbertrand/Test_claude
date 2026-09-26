package com.apexus.storagelens.ui.screens.explorer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.ui.state.ToggleableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
import com.apexus.storagelens.data.delete.DeletionState
import com.apexus.storagelens.domain.model.FileEntry
import com.apexus.storagelens.domain.model.FileNode
import com.apexus.storagelens.domain.model.FileType
import com.apexus.storagelens.ui.components.DeleteConfirmDialog
import com.apexus.storagelens.ui.components.DeletionProgressDialog
import com.apexus.storagelens.ui.components.SystemMediaConfirmation
import com.apexus.storagelens.ui.components.EmptyState
import com.apexus.storagelens.ui.components.FileThumbnail
import com.apexus.storagelens.ui.components.ProportionBar
import com.apexus.storagelens.ui.components.Treemap
import com.apexus.storagelens.ui.components.deletionReportMessage
import com.apexus.storagelens.ui.util.FileActions
import com.apexus.storagelens.ui.util.formatDate
import com.apexus.storagelens.ui.util.formatSize
import com.apexus.storagelens.ui.util.labelRes
import com.apexus.storagelens.ui.util.percent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(onStartScan: () -> Unit, viewModel: ExplorerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deletion by viewModel.deletionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var sortMenu by rememberSaveable { mutableStateOf(false) }
    var moreMenu by rememberSaveable { mutableStateOf(false) }

    val result = state.result
    val atRoot = state.currentNode?.path == result?.root?.path
    BackHandler(enabled = state.selection.isNotEmpty() || (!atRoot && state.tab != ExplorerTab.TOP_FILES)) {
        if (state.selection.isNotEmpty()) viewModel.clearSelection() else viewModel.navigateUp()
    }

    val finished = deletion as? DeletionState.Finished
    val finishedMessage = finished?.let { deletionReportMessage(it.report) }
    val undoLabel = stringResource(R.string.action_undo)
    LaunchedEffect(finished) {
        val report = finished?.report ?: return@LaunchedEffect
        viewModel.acknowledgeDeletion()
        val action = snackbar.showSnackbar(
            message = finishedMessage.orEmpty(),
            actionLabel = if (report.movedToTrash) undoLabel else null,
            duration = SnackbarDuration.Long,
        )
        if (action == SnackbarResult.ActionPerformed) viewModel.undo(report.batchId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.selection.isEmpty()) {
                        Text(stringResource(R.string.nav_explorer))
                    } else {
                        Text(pluralStringResource(R.plurals.selected_count, state.selection.size, state.selection.size))
                    }
                },
                navigationIcon = {
                    when {
                        state.selection.isNotEmpty() -> IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear_selection))
                        }
                        !atRoot && state.tab != ExplorerTab.TOP_FILES -> IconButton(onClick = { viewModel.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_up))
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { sortMenu = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.action_sort))
                    }
                    DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                        SortMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = { Text(stringResource(mode.labelRes())) },
                                leadingIcon = { RadioButton(selected = state.sort == mode, onClick = null) },
                                onClick = { viewModel.setSort(mode); sortMenu = false },
                            )
                        }
                    }
                    IconButton(onClick = { moreMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.a11y_more_options))
                    }
                    DropdownMenu(expanded = moreMenu, onDismissRequest = { moreMenu = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.explorer_exclude_folder)) },
                            leadingIcon = { Icon(Icons.Filled.FolderOff, null) },
                            enabled = !atRoot,
                            onClick = { viewModel.excludeCurrentFolder(); moreMenu = false },
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (state.selection.isNotEmpty()) {
                BottomAppBar {
                    Text(
                        stringResource(R.string.selection_summary, state.selection.size, formatSize(state.selectedBytes)),
                        Modifier.padding(start = 16.dp).weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = viewModel::requestDelete,
                        modifier = Modifier.padding(end = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Text(stringResource(R.string.action_delete), Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (result == null) {
            EmptyState(
                icon = Icons.Filled.Search,
                title = stringResource(R.string.explorer_empty_title),
                message = stringResource(R.string.explorer_empty_message),
                actionLabel = stringResource(R.string.home_start_scan),
                onAction = onStartScan,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = state.tab.ordinal) {
                ExplorerTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.setTab(tab) },
                        text = { Text(stringResource(tab.labelRes())) },
                    )
                }
            }
            if (state.tab != ExplorerTab.TOP_FILES) {
                Breadcrumb(state.breadcrumb, onClick = viewModel::openPath)
            }
            when (state.tab) {
                ExplorerTab.LIST -> DirectoryList(state, viewModel, onOpenFile = { FileActions.open(context, it) })
                ExplorerTab.TREEMAP -> state.currentNode?.let { node ->
                    Treemap(
                        node = node.copy(children = state.visibleChildren),
                        onOpen = { child -> if (child.isDirectory) viewModel.open(child) else FileActions.open(context, child.path) },
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                    )
                }
                ExplorerTab.TOP_FILES -> TopFiles(state, viewModel, onOpenFile = { FileActions.open(context, it) })
            }
        }
    }

    state.pendingDeletion?.let { pending ->
        DeleteConfirmDialog(pending, onConfirm = viewModel::confirmDelete, onDismiss = viewModel::dismissDelete)
    }
    (deletion as? DeletionState.Running)?.let { DeletionProgressDialog(it) }
    val systemConfirmation by viewModel.systemConfirmation.collectAsStateWithLifecycle()
    SystemMediaConfirmation(
        pending = systemConfirmation,
        onShown = viewModel::onSystemConfirmationShown,
        onResult = viewModel::onSystemConfirmationResult,
    )
}

@Composable
private fun Breadcrumb(crumbs: List<Pair<String, String>>, onClick: (String) -> Unit) {
    LazyRow(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        itemsIndexed(crumbs, key = { _, crumb -> crumb.first }) { index, (path, label) ->
            if (index > 0) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { onClick(path) }, enabled = index < crumbs.lastIndex) {
                Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun DirectoryList(state: ExplorerUiState, viewModel: ExplorerViewModel, onOpenFile: (String) -> Unit) {
    val node = state.currentNode ?: return
    val children = state.visibleChildren
    LazyColumn(Modifier.fillMaxSize()) {
        if (children.isNotEmpty()) {
            item(key = "select_all") {
                SelectAllRow(
                    paths = children.map { it.path },
                    sizes = children.map { it.size },
                    state = state,
                    onToggle = { viewModel.toggleAllNodes(children) },
                )
                HorizontalDivider()
            }
        }
        items(children, key = { it.path }) { child ->
            NodeRow(
                node = child,
                parentSize = node.size,
                selected = child.path in state.selection,
                selectable = state.canDelete(child.path),
                onToggle = { viewModel.toggle(child) },
                onClick = { if (child.isDirectory) viewModel.open(child) else onOpenFile(child.path) },
            )
            HorizontalDivider()
        }
        if (node.hiddenFilesCount > 0) {
            item(key = "hidden") {
                ListItem(
                    headlineContent = { Text(pluralStringResource(R.plurals.explorer_other_files, node.hiddenFilesCount, node.hiddenFilesCount)) },
                    supportingContent = { Text(stringResource(R.string.explorer_other_files_hint)) },
                    trailingContent = { Text(formatSize(node.hiddenFilesSize)) },
                )
            }
        }
        if (children.isEmpty() && node.hiddenFilesCount == 0) {
            item(key = "empty") {
                ListItem(headlineContent = { Text(stringResource(R.string.explorer_folder_empty)) })
            }
        }
    }
}

/** Ligne « Tout sélectionner » à trois états, en tête de liste. */
@Composable
private fun SelectAllRow(paths: List<String>, sizes: List<Long>, state: ExplorerUiState, onToggle: () -> Unit) {
    val selectable = paths.indices.filter { state.canDelete(paths[it]) }
    val selectedCount = selectable.count { paths[it] in state.selection }
    val toggleState = when {
        selectable.isEmpty() || selectedCount == 0 -> ToggleableState.Off
        selectedCount == selectable.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    val total = selectable.sumOf { sizes[it] }
    ListItem(
        modifier = Modifier.clickable(enabled = selectable.isNotEmpty(), onClick = onToggle),
        headlineContent = { Text(stringResource(R.string.select_all), style = MaterialTheme.typography.titleSmall) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.explorer_select_all_summary, selectable.size, selectable.size, formatSize(total)),
                style = MaterialTheme.typography.bodySmall,
            )
        },
        trailingContent = {
            TriStateCheckbox(state = toggleState, onClick = onToggle, enabled = selectable.isNotEmpty())
        },
    )
}

@Composable
private fun NodeRow(
    node: FileNode,
    parentSize: Long,
    selected: Boolean,
    selectable: Boolean,
    onToggle: () -> Unit,
    onClick: () -> Unit,
) {
    val fraction = percent(node.size, parentSize)
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { FileThumbnail(node.path, node.name, node.isDirectory) },
        headlineContent = { Text(node.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val details = if (node.isDirectory) {
                    pluralStringResource(R.plurals.file_count, node.fileCount, node.fileCount) + " · " + "${(fraction * 100).toInt()} %"
                } else {
                    formatDate(node.lastModified) + " · " + "${(fraction * 100).toInt()} %"
                }
                Text(details, style = MaterialTheme.typography.bodySmall)
                ProportionBar(fraction)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatSize(node.size), style = MaterialTheme.typography.labelLarge)
                Checkbox(checked = selected, onCheckedChange = { onToggle() }, enabled = selectable)
            }
        },
    )
}

@Composable
private fun TopFiles(state: ExplorerUiState, viewModel: ExplorerViewModel, onOpenFile: (String) -> Unit) {
    val files = state.visibleTopFiles
    val maxSize = files.maxOfOrNull { it.size } ?: 1L
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(selected = state.typeFilter == null, onClick = { viewModel.setTypeFilter(null) }, label = { Text(stringResource(R.string.filter_all_types)) })
            FileType.entries.forEach { type ->
                FilterChip(selected = state.typeFilter == type, onClick = { viewModel.setTypeFilter(type) }, label = { Text(stringResource(type.labelRes())) })
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AgeFilter.entries.forEach { age ->
                FilterChip(selected = state.ageFilter == age, onClick = { viewModel.setAgeFilter(age) }, label = { Text(stringResource(age.labelRes())) })
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            if (files.isNotEmpty()) {
                item(key = "select_all") {
                    SelectAllRow(
                        paths = files.map { it.path },
                        sizes = files.map { it.size },
                        state = state,
                        onToggle = { viewModel.toggleAllEntries(files) },
                    )
                    HorizontalDivider()
                }
            }
            items(files, key = { it.path }) { entry ->
                TopFileRow(
                    entry = entry,
                    fraction = percent(entry.size, maxSize),
                    selected = entry.path in state.selection,
                    selectable = state.canDelete(entry.path),
                    onToggle = { viewModel.toggle(entry) },
                    onClick = { onOpenFile(entry.path) },
                )
                HorizontalDivider()
            }
            if (files.isEmpty()) {
                item { ListItem(headlineContent = { Text(stringResource(R.string.explorer_no_match)) }) }
            }
        }
    }
}

@Composable
private fun TopFileRow(entry: FileEntry, fraction: Float, selected: Boolean, selectable: Boolean, onToggle: () -> Unit, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = { FileThumbnail(entry.path, entry.name, isDirectory = false) },
        headlineContent = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.path.substringBeforeLast('/'), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(formatDate(entry.lastModified), style = MaterialTheme.typography.bodySmall)
                ProportionBar(fraction)
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(formatSize(entry.size), style = MaterialTheme.typography.labelLarge)
                Checkbox(checked = selected, onCheckedChange = { onToggle() }, enabled = selectable)
            }
        },
    )
}

private fun SortMode.labelRes(): Int = when (this) {
    SortMode.SIZE -> R.string.sort_size
    SortMode.COUNT -> R.string.sort_count
    SortMode.DATE -> R.string.sort_date
    SortMode.NAME -> R.string.sort_name
}

private fun ExplorerTab.labelRes(): Int = when (this) {
    ExplorerTab.LIST -> R.string.explorer_tab_folders
    ExplorerTab.TREEMAP -> R.string.explorer_tab_treemap
    ExplorerTab.TOP_FILES -> R.string.explorer_tab_top_files
}

private fun AgeFilter.labelRes(): Int = when (this) {
    AgeFilter.ALL -> R.string.filter_any_age
    AgeFilter.OLDER_30 -> R.string.filter_older_30
    AgeFilter.OLDER_90 -> R.string.filter_older_90
    AgeFilter.OLDER_365 -> R.string.filter_older_365
}
