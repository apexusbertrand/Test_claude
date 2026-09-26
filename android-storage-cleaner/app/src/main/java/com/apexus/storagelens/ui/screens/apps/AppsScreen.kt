package com.apexus.storagelens.ui.screens.apps

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
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
    // Rafraîchi à chaque retour sur l'écran : après avoir vidé un cache dans les Paramètres (mode assisté).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_apps)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
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
                    items(state.visible, key = { it.packageName }) { app ->
                        AppRow(app)
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(app: AppStorageInfo) {
    val context = LocalContext.current
    var menu by rememberSaveable { mutableStateOf(false) }
    val icon by produceState<ImageBitmap?>(null, app.packageName) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96).asImageBitmap() }.getOrNull()
        }
    }
    ListItem(
        leadingContent = {
            Box(Modifier.size(40.dp)) {
                val bitmap = icon
                if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(40.dp))
                else Icon(Icons.Filled.Android, contentDescription = null, modifier = Modifier.size(40.dp))
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
                    val details = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
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
