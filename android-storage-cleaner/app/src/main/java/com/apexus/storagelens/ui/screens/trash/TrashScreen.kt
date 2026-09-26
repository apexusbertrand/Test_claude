package com.apexus.storagelens.ui.screens.trash

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
import com.apexus.storagelens.domain.model.TrashItem
import com.apexus.storagelens.ui.components.EmptyState
import com.apexus.storagelens.ui.components.FileThumbnail
import com.apexus.storagelens.ui.components.InfoBanner
import com.apexus.storagelens.ui.util.formatRelative
import com.apexus.storagelens.ui.util.formatSize
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(onBack: () -> Unit, viewModel: TrashViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var confirmEmpty by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val message = when (event) {
                is TrashEvent.Restored -> context.resources.getQuantityString(R.plurals.trash_restored, event.count, event.count)
                is TrashEvent.Deleted -> context.resources.getQuantityString(R.plurals.trash_deleted, event.count, event.count)
                TrashEvent.Failed -> context.getString(R.string.trash_action_failed)
            }
            snackbar.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_trash)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
                actions = {
                    if (state.items.isNotEmpty()) {
                        IconButton(onClick = { confirmEmpty = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = stringResource(R.string.trash_empty_action))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (state.items.isEmpty() && !state.loading) {
            EmptyState(
                icon = Icons.Filled.Delete,
                title = stringResource(R.string.trash_empty_title),
                message = stringResource(if (state.enabled) R.string.trash_empty_message else R.string.trash_disabled_message),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item(key = "info") {
                InfoBanner(
                    icon = Icons.Filled.Info,
                    text = stringResource(R.string.trash_info, formatSize(state.totalBytes), state.retentionDays),
                    modifier = Modifier.padding(16.dp),
                )
            }
            items(state.items, key = { it.id }) { item ->
                TrashRow(item, state.retentionDays, onRestore = { viewModel.restore(item) }, onDelete = { viewModel.delete(item) })
                HorizontalDivider()
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            icon = { Icon(Icons.Filled.DeleteForever, contentDescription = null) },
            title = { Text(stringResource(R.string.trash_empty_confirm_title)) },
            text = { Text(stringResource(R.string.trash_empty_confirm_text, formatSize(state.totalBytes))) },
            confirmButton = { Button(onClick = { confirmEmpty = false; viewModel.empty() }) { Text(stringResource(R.string.trash_empty_action)) } },
            dismissButton = { TextButton(onClick = { confirmEmpty = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun TrashRow(item: TrashItem, retentionDays: Int, onRestore: () -> Unit, onDelete: () -> Unit) {
    val name = item.originalPath.substringAfterLast('/')
    val expiresAt = item.deletedAt + TimeUnit.DAYS.toMillis(retentionDays.toLong())
    ListItem(
        leadingContent = { FileThumbnail(item.trashPath, name, item.isDirectory) },
        headlineContent = { Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Column {
                Text(item.originalPath.substringBeforeLast('/'), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.trash_item_dates, formatRelative(item.deletedAt), formatRelative(expiresAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Row {
                Text(formatSize(item.size), Modifier.padding(top = 12.dp), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onRestore) { Icon(Icons.Filled.Restore, contentDescription = stringResource(R.string.a11y_restore, name)) }
                IconButton(onClick = onDelete) { Icon(Icons.Filled.DeleteForever, contentDescription = stringResource(R.string.a11y_delete_forever, name)) }
            }
        },
    )
}
