package com.apexus.storagelens.ui.screens.cleanup

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.os.storage.StorageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
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
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
import com.apexus.storagelens.data.delete.DeletionState
import com.apexus.storagelens.domain.model.CleanupCandidate
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.model.CleanupGroup
import com.apexus.storagelens.domain.selection.Selection
import com.apexus.storagelens.domain.selection.TriState
import com.apexus.storagelens.ui.components.DeleteConfirmDialog
import com.apexus.storagelens.ui.components.DeletionProgressDialog
import com.apexus.storagelens.ui.components.EmptyState
import com.apexus.storagelens.ui.components.FileThumbnail
import com.apexus.storagelens.ui.components.InfoBanner
import com.apexus.storagelens.ui.components.RiskBadge
import com.apexus.storagelens.ui.components.deletionReportMessage
import com.apexus.storagelens.ui.util.FileActions
import com.apexus.storagelens.ui.util.descriptionRes
import com.apexus.storagelens.ui.util.formatSize
import com.apexus.storagelens.ui.util.icon
import com.apexus.storagelens.ui.util.titleRes

private const val MAX_ITEMS_PER_FOLDER = 100

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupScreen(
    onStartScan: () -> Unit,
    onOpenAssistedCache: () -> Unit,
    onOpenPermissions: () -> Unit,
    viewModel: CleanupViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deletion by viewModel.deletionState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var confirmRootClean by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAppCache() }
    val clearCacheLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.refreshAppCache()
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
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_cleanup)) }) },
        bottomBar = {
            if (state.groups.isNotEmpty()) {
                BottomAppBar {
                    Text(
                        stringResource(R.string.selection_summary, state.selectedCount, formatSize(state.selectedBytes)),
                        Modifier.padding(start = 16.dp).weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Button(
                        onClick = viewModel::requestDelete,
                        enabled = state.selectedCount > 0,
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
        if (state.result == null) {
            EmptyState(
                icon = Icons.Filled.Search,
                title = stringResource(R.string.cleanup_empty_title),
                message = stringResource(R.string.cleanup_empty_message),
                actionLabel = stringResource(R.string.home_start_scan),
                onAction = onStartScan,
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") { Summary(state, viewModel) }

            if (state.groups.isEmpty()) {
                item(key = "nothing") {
                    InfoBanner(icon = Icons.Filled.CleaningServices, text = stringResource(R.string.cleanup_nothing_found))
                }
            }

            state.groups.forEach { group ->
                groupItems(group, state, viewModel, onOpenFile = { FileActions.open(context, it) })
            }

            item(key = "app_cache") {
                AppCacheCard(
                    state = state,
                    onClearAll = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            try {
                                clearCacheLauncher.launch(Intent(StorageManager.ACTION_CLEAR_APP_CACHE))
                            } catch (e: ActivityNotFoundException) {
                                onOpenAssistedCache()
                            } catch (e: SecurityException) {
                                onOpenAssistedCache()
                            }
                        }
                    },
                    onAssisted = onOpenAssistedCache,
                    onGrant = onOpenPermissions,
                )
            }
            item(key = "system_logs") {
                SystemLogsCard(state, onClean = { confirmRootClean = true })
            }
        }
    }

    state.pendingDeletion?.let { pending ->
        DeleteConfirmDialog(pending, onConfirm = viewModel::confirmDelete, onDismiss = viewModel::dismissDelete)
    }
    (deletion as? DeletionState.Running)?.let { DeletionProgressDialog(it) }
    if (confirmRootClean) {
        AlertDialog(
            onDismissRequest = { confirmRootClean = false },
            title = { Text(stringResource(R.string.root_clean_confirm_title)) },
            text = { Text(stringResource(R.string.root_clean_confirm_text)) },
            confirmButton = {
                Button(onClick = { confirmRootClean = false; viewModel.cleanPrivileged() }) { Text(stringResource(R.string.action_clean)) }
            },
            dismissButton = { TextButton(onClick = { confirmRootClean = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun Summary(state: CleanupUiState, viewModel: CleanupViewModel) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.home_recoverable), style = MaterialTheme.typography.titleMedium)
            Text(formatSize(state.result?.recoverableBytes ?: 0), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = viewModel::selectRecommended, label = { Text(stringResource(R.string.select_recommended)) })
                AssistChip(onClick = viewModel::selectAll, label = { Text(stringResource(R.string.select_all)) })
                AssistChip(onClick = viewModel::selectNone, label = { Text(stringResource(R.string.select_none)) })
            }
        }
    }
}

private fun TriState.toToggleable() = when (this) {
    TriState.ON -> ToggleableState.On
    TriState.OFF -> ToggleableState.Off
    TriState.INDETERMINATE -> ToggleableState.Indeterminate
}

private fun LazyListScope.groupItems(
    group: CleanupGroup,
    state: CleanupUiState,
    viewModel: CleanupViewModel,
    onOpenFile: (String) -> Unit,
) {
    val expanded = group.category in state.expanded
    item(key = "group_${group.category}") {
        GroupHeader(
            group = group,
            toggleState = Selection.stateOf(group.candidates, state.selection).toToggleable(),
            selectedBytes = group.candidates.filter { it.path in state.selection }.sumOf { it.size },
            expanded = expanded,
            onToggle = { viewModel.toggleGroup(group) },
            onExpand = { viewModel.toggleExpanded(group.category) },
        )
    }
    if (!expanded) return
    Selection.byParent(group).forEach { (parent, items) ->
        item(key = "parent_${group.category}_$parent") {
            ListItem(
                modifier = Modifier.clickable { viewModel.toggleParent(group, parent) },
                leadingContent = {
                    TriStateCheckbox(
                        state = Selection.stateOf(items, state.selection).toToggleable(),
                        onClick = { viewModel.toggleParent(group, parent) },
                    )
                },
                headlineContent = {
                    Text(
                        parent.removePrefix(state.result?.root?.path.orEmpty()).ifEmpty { "/" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleSmall,
                    )
                },
                trailingContent = { Text(formatSize(items.sumOf { it.size })) },
            )
        }
        items(items.take(MAX_ITEMS_PER_FOLDER), key = { "item_${group.category}_${it.path}" }) { candidate ->
            CandidateRow(candidate, selected = candidate.path in state.selection, onToggle = { viewModel.toggleItem(candidate.path) }, onOpen = { onOpenFile(candidate.path) })
        }
        if (items.size > MAX_ITEMS_PER_FOLDER) {
            item(key = "more_${group.category}_$parent") {
                Text(
                    pluralStringResource(R.plurals.cleanup_more_items, items.size - MAX_ITEMS_PER_FOLDER, items.size - MAX_ITEMS_PER_FOLDER),
                    Modifier.padding(start = 72.dp),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

@Composable
private fun GroupHeader(
    group: CleanupGroup,
    toggleState: ToggleableState,
    selectedBytes: Long,
    expanded: Boolean,
    onToggle: () -> Unit,
    onExpand: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onExpand)) {
        Row(Modifier.padding(start = 4.dp, end = 8.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            TriStateCheckbox(state = toggleState, onClick = onToggle)
            Icon(group.category.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(horizontal = 12.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(group.category.titleRes()), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f, fill = false))
                    Spacer(Modifier.size(8.dp))
                    RiskBadge(group.category.risk)
                }
                Text(
                    pluralStringResource(R.plurals.cleanup_group_summary, group.count, group.count, formatSize(group.totalBytes), formatSize(selectedBytes)),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    stringResource(group.category.descriptionRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onExpand) {
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(if (expanded) R.string.a11y_collapse else R.string.a11y_expand),
                )
            }
        }
    }
}

@Composable
private fun CandidateRow(candidate: CleanupCandidate, selected: Boolean, onToggle: () -> Unit, onOpen: () -> Unit) {
    val detail = candidateDetail(candidate)
    ListItem(
        modifier = Modifier.clickable(onClick = if (candidate.isDirectory) onToggle else onOpen).padding(start = 24.dp),
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(checked = selected, onCheckedChange = { onToggle() })
                FileThumbnail(candidate.path, candidate.name, candidate.isDirectory)
            }
        },
        headlineContent = { Text(candidate.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (detail != null) {
            { Text(detail, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall) }
        } else {
            null
        },
        trailingContent = { Text(formatSize(candidate.size)) },
    )
}

@Composable
private fun candidateDetail(candidate: CleanupCandidate): String? = when (candidate.category) {
    CleanupCategory.APK_FILES -> when (val pkg = candidate.detail) {
        null -> stringResource(R.string.detail_apk_unknown)
        "installed" -> stringResource(R.string.detail_apk_installed)
        else -> stringResource(R.string.detail_apk_not_installed, pkg)
    }
    CleanupCategory.DUPLICATES -> candidate.detail?.let { stringResource(R.string.detail_duplicate_of, it.substringAfterLast('/')) }
    CleanupCategory.MESSAGING_MEDIA -> candidate.detail?.let { stringResource(R.string.detail_messaging_app, it) }
    CleanupCategory.ORPHAN_APP_DATA -> stringResource(R.string.detail_orphan)
    CleanupCategory.TEMP_FILES -> if (candidate.detail == "backup") stringResource(R.string.detail_backup) else null
    else -> null
}

@Composable
private fun AppCacheCard(state: CleanupUiState, onClearAll: () -> Unit, onAssisted: () -> Unit, onGrant: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Cached, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.app_cache_title), Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
                RiskBadge(CleanupCategory.CACHES.risk)
            }
            if (!state.usageAccess) {
                Text(stringResource(R.string.app_cache_need_usage), style = MaterialTheme.typography.bodyMedium)
                FilledTonalButton(onClick = onGrant) { Text(stringResource(R.string.action_grant)) }
            } else {
                val cache = state.appCache
                if (cache == null) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                } else {
                    Text(
                        pluralStringResource(R.plurals.app_cache_summary, cache.appCount, formatSize(cache.totalCacheBytes), cache.appCount),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(stringResource(R.string.app_cache_explanation), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Button(onClick = onClearAll) { Text(stringResource(R.string.app_cache_clear_all)) }
                    }
                    OutlinedButton(onClick = onAssisted) { Text(stringResource(R.string.app_cache_assisted)) }
                }
            }
        }
    }
}

@Composable
private fun SystemLogsCard(state: CleanupUiState, onClean: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.system_logs_title), Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
            }
            val privileged = state.privileged
            when {
                !state.settings.advancedRootMode -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(stringResource(R.string.system_logs_inaccessible), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                    }
                    Text(stringResource(R.string.system_logs_explanation), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.privilegedBusy -> CircularProgressIndicator(Modifier.size(24.dp))
                privileged == null || !privileged.available ->
                    Text(stringResource(R.string.system_logs_root_unavailable), style = MaterialTheme.typography.bodyMedium)
                else -> {
                    privileged.areas.forEach { area ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(area.path, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text(area.sizeBytes?.let { formatSize(it) } ?: "—", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Button(onClick = onClean) { Text(stringResource(R.string.system_logs_clean)) }
                }
            }
        }
    }
}
