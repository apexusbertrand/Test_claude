package com.apexus.storagelens.ui.screens.apps

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
import com.apexus.storagelens.domain.apps.AppBatch
import com.apexus.storagelens.domain.apps.AppBatchKind
import com.apexus.storagelens.domain.model.AppStorageInfo
import com.apexus.storagelens.ui.components.EmptyState
import com.apexus.storagelens.ui.components.InfoBanner
import com.apexus.storagelens.ui.components.SkeletonList
import com.apexus.storagelens.ui.util.FileActions
import com.apexus.storagelens.ui.util.formatRelative
import com.apexus.storagelens.ui.util.formatSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(onOpenPermissions: () -> Unit, viewModel: AppsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    // Rafraîchi à chaque retour sur l'écran (après un passage par les Paramètres).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    // Action groupée : on ouvre l'écran système de chaque application, l'une après l'autre.
    val stepLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onStepReturned()
    }
    val batch = state.batch
    LaunchedEffect(batch?.index, batch?.launched) {
        val current = batch?.current ?: return@LaunchedEffect
        if (batch.launched) return@LaunchedEffect
        viewModel.onStepLaunched()
        val intent = when (batch.kind) {
            AppBatchKind.CLEAR_CACHE -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$current"))
            AppBatchKind.UNINSTALL -> Intent(Intent.ACTION_DELETE, Uri.parse("package:$current"))
        }
        try {
            stepLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            viewModel.onStepReturned()
        }
    }

    val result = state.batchResult
    val resultMessage = result?.let { r ->
        val res = when (r.kind) {
            AppBatchKind.CLEAR_CACHE -> R.plurals.apps_batch_cache_done
            AppBatchKind.UNINSTALL -> R.plurals.apps_batch_uninstall_done
        }
        val main = pluralStringResource(res, r.processed, r.processed, formatSize(r.freedBytes))
        if (r.skipped > 0) {
            val skippedRes = if (r.kind == AppBatchKind.UNINSTALL) R.plurals.apps_batch_skipped_system else R.plurals.apps_batch_skipped_no_cache
            main + " · " + pluralStringResource(skippedRes, r.skipped, r.skipped)
        } else {
            main
        }
    }
    LaunchedEffect(result) {
        if (result != null && resultMessage != null) {
            viewModel.acknowledgeResult()
            snackbar.showSnackbar(resultMessage)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.selection.isEmpty()) {
                        Text(stringResource(R.string.nav_apps))
                    } else {
                        Text(pluralStringResource(R.plurals.selected_count, state.selection.size, state.selection.size))
                    }
                },
                navigationIcon = {
                    if (state.selection.isNotEmpty()) {
                        IconButton(onClick = viewModel::clearSelection) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear_selection))
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (state.selection.isNotEmpty() && state.batch == null) {
                BottomAppBar {
                    Column(Modifier.padding(start = 16.dp).weight(1f)) {
                        Text(
                            pluralStringResource(R.plurals.apps_selected_summary, state.selection.size, state.selection.size, formatSize(state.selectedTotal)),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                        )
                        Text(
                            stringResource(R.string.apps_selected_cache, formatSize(state.selectedCache)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(onClick = viewModel::startClearCache, modifier = Modifier.padding(end = 8.dp)) {
                        Icon(Icons.Filled.Cached, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.apps_action_cache), Modifier.padding(start = 4.dp))
                    }
                    Button(
                        onClick = viewModel::requestUninstall,
                        modifier = Modifier.padding(end = 16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(stringResource(R.string.apps_action_uninstall), Modifier.padding(start = 4.dp))
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (batch != null) BatchBanner(batch, onStop = viewModel::stopBatch)
            if (!state.usageAccess) {
                InfoBanner(
                    icon = Icons.Filled.Lock,
                    text = stringResource(R.string.apps_need_usage),
                    warning = true,
                    actionLabel = stringResource(R.string.action_grant),
                    onAction = onOpenPermissions,
                    modifier = Modifier.padding(16.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppSort.entries.forEach { sort ->
                    FilterChip(selected = state.sort == sort, onClick = { viewModel.setSort(sort) }, label = { Text(stringResource(sort.labelRes())) })
                }
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                UsageFilter.entries.forEach { filter ->
                    FilterChip(selected = state.filter == filter, onClick = { viewModel.setFilter(filter) }, label = { Text(stringResource(filter.labelRes())) })
                }
                FilterChip(selected = state.showSystem, onClick = { viewModel.setShowSystem(!state.showSystem) }, label = { Text(stringResource(R.string.apps_show_system)) })
            }
            when {
                state.loading -> SkeletonList()
                state.visible.isEmpty() -> EmptyState(
                    icon = Icons.Filled.Apps,
                    title = stringResource(R.string.apps_empty_title),
                    message = stringResource(R.string.apps_empty_message),
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    if (state.sort == AppSort.CACHE) {
                        item(key = "assisted_hint") {
                            InfoBanner(
                                icon = Icons.Filled.Info,
                                text = stringResource(R.string.apps_assisted_hint, formatSize(state.totalCache)),
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    }
                    item(key = "select_all") {
                        SelectAllAppsRow(state, onToggle = viewModel::toggleAllVisible)
                        HorizontalDivider()
                    }
                    items(state.visible, key = { it.packageName }) { app ->
                        AppRow(
                            app = app,
                            selected = app.packageName in state.selection,
                            onToggle = { viewModel.toggle(app.packageName) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (state.confirmUninstall) {
        val apps = state.selectedApps
        val removable = apps.filter { !it.isSystemApp && it.packageName != context.packageName }
        AlertDialog(
            onDismissRequest = viewModel::dismissUninstall,
            icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
            title = { Text(pluralStringResource(R.plurals.apps_uninstall_confirm_title, removable.size, removable.size, formatSize(removable.sumOf { it.totalBytes }))) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.apps_uninstall_confirm_text))
                    Text(
                        removable.joinToString(", ") { it.label },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val system = apps.size - removable.size
                    if (system > 0) {
                        Text(
                            pluralStringResource(R.plurals.apps_batch_skipped_system, system, system),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = viewModel::confirmUninstall,
                    enabled = removable.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.apps_uninstall)) }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissUninstall) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Bandeau affiché pendant une action groupée : où l'on en est et quoi faire dans l'écran système. */
@Composable
private fun BatchBanner(batch: AppBatch, onStop: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val title = when (batch.kind) {
                AppBatchKind.CLEAR_CACHE -> R.string.apps_batch_cache_title
                AppBatchKind.UNINSTALL -> R.string.apps_batch_uninstall_title
            }
            Text(
                stringResource(title, (batch.index + 1).coerceAtMost(batch.total), batch.total, batch.currentLabel.orEmpty()),
                style = MaterialTheme.typography.titleSmall,
            )
            LinearProgressIndicator(
                progress = { if (batch.total == 0) 0f else batch.index.toFloat() / batch.total },
                modifier = Modifier.fillMaxWidth(),
            )
            val hint = when (batch.kind) {
                AppBatchKind.CLEAR_CACHE -> R.string.apps_batch_cache_hint
                AppBatchKind.UNINSTALL -> R.string.apps_batch_uninstall_hint
            }
            Text(stringResource(hint), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick = onStop) { Text(stringResource(R.string.apps_batch_stop)) }
        }
    }
}

@Composable
private fun SelectAllAppsRow(state: AppsUiState, onToggle: () -> Unit) {
    val visible = state.visible
    val selectedCount = visible.count { it.packageName in state.selection }
    val toggleState = when (selectedCount) {
        0 -> ToggleableState.Off
        visible.size -> ToggleableState.On
        else -> ToggleableState.Indeterminate
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        leadingContent = { TriStateCheckbox(state = toggleState, onClick = onToggle) },
        headlineContent = { Text(stringResource(R.string.apps_select_all), style = MaterialTheme.typography.titleSmall) },
        supportingContent = {
            Text(
                pluralStringResource(R.plurals.apps_select_all_summary, visible.size, visible.size, formatSize(visible.sumOf { it.cacheBytes })),
                style = MaterialTheme.typography.bodySmall,
            )
        },
    )
}

@Composable
private fun AppRow(app: AppStorageInfo, selected: Boolean, onToggle: () -> Unit) {
    val context = LocalContext.current
    var menu by rememberSaveable { mutableStateOf(false) }
    val icon by produceState<ImageBitmap?>(null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onToggle),
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = selected, onCheckedChange = { onToggle() })
                Box(Modifier.size(40.dp)) {
                    val bitmap = icon
                    if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
                    else Icon(Icons.Filled.Android, contentDescription = null, modifier = Modifier.size(40.dp))
                }
            }
        },
        headlineContent = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(
                    stringResource(R.string.apps_breakdown, formatSize(app.apkBytes), formatSize(app.dataBytes - app.cacheBytes), formatSize(app.cacheBytes)),
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    app.lastUsedMillis?.let { stringResource(R.string.apps_last_used, formatRelative(it)) } ?: stringResource(R.string.apps_last_used_unknown),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Row {
                Text(formatSize(app.totalBytes), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 12.dp))
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.a11y_app_actions, app.label))
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    val details = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.apps_clear_cache)) },
                        leadingIcon = { Icon(Icons.Filled.Cached, null) },
                        onClick = { menu = false; FileActions.startSafely(context, details) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.apps_details)) },
                        leadingIcon = { Icon(Icons.Filled.Info, null) },
                        onClick = { menu = false; FileActions.startSafely(context, details) },
                    )
                    if (!app.isSystemApp && app.packageName != context.packageName) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.apps_uninstall)) },
                            leadingIcon = { Icon(Icons.Filled.Delete, null) },
                            onClick = {
                                menu = false
                                FileActions.startSafely(context, Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
                            },
                        )
                    }
                }
            }
        },
    )
}

private fun AppSort.labelRes(): Int = when (this) {
    AppSort.TOTAL -> R.string.apps_sort_total
    AppSort.CACHE -> R.string.apps_sort_cache
    AppSort.NAME -> R.string.sort_name
}

private fun UsageFilter.labelRes(): Int = when (this) {
    UsageFilter.ALL -> R.string.apps_filter_all
    UsageFilter.UNUSED_30 -> R.string.apps_filter_unused_30
    UsageFilter.UNUSED_90 -> R.string.apps_filter_unused_90
}
