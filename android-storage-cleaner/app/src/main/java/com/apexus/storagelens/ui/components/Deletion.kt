package com.apexus.storagelens.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.apexus.storagelens.R
import com.apexus.storagelens.data.delete.DeletionState
import com.apexus.storagelens.domain.model.DeletionReport
import com.apexus.storagelens.domain.model.RiskLevel
import com.apexus.storagelens.ui.util.formatSize
import android.os.Build
import android.text.format.Formatter

/** Élément présenté dans la confirmation de suppression. */
data class PendingItem(
    val path: String,
    val name: String,
    val size: Long,
    val risk: RiskLevel,
    val permanent: Boolean,
    val isPhotoOrVideo: Boolean = false,
    val isDirectory: Boolean = false,
)

data class PendingDeletion(val items: List<PendingItem>, val trashEnabled: Boolean) {
    val totalBytes: Long get() = items.sumOf { it.size }
    val risky: List<PendingItem> get() = items.filter { it.risk != RiskLevel.LOW }
    val permanentCount: Int get() = if (trashEnabled) items.count { it.permanent } else items.size
    val photoVideoCount: Int get() = items.count { it.isPhotoOrVideo }
    val directoryCount: Int get() = items.count { it.isDirectory }
}

@Composable
fun DeleteConfirmDialog(pending: PendingDeletion, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Delete, contentDescription = null) },
        title = {
            Text(pluralStringResource(R.plurals.confirm_delete_title, pending.items.size, pending.items.size, formatSize(pending.totalBytes)))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    !pending.trashEnabled -> Text(stringResource(R.string.confirm_delete_permanent_all))
                    pending.permanentCount == pending.items.size -> Text(stringResource(R.string.confirm_delete_permanent_all))
                    pending.permanentCount > 0 -> Text(pluralStringResource(R.plurals.confirm_delete_mixed, pending.permanentCount, pending.permanentCount))
                    else -> Text(stringResource(R.string.confirm_delete_to_trash))
                }
                if (pending.photoVideoCount > 0) {
                    val n = pending.photoVideoCount
                    val note = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        pluralStringResource(R.plurals.confirm_delete_media_system, n, n)
                    } else {
                        pluralStringResource(R.plurals.confirm_delete_media, n, n)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(note, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (pending.directoryCount > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(
                            pluralStringResource(R.plurals.confirm_delete_folders_media, pending.directoryCount),
                            Modifier.padding(start = 8.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (pending.risky.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(18.dp))
                                Text(
                                    pluralStringResource(R.plurals.confirm_delete_risky, pending.risky.size, pending.risky.size),
                                    modifier = Modifier.padding(start = 8.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            LazyColumn(Modifier.heightIn(max = 180.dp).padding(top = 8.dp)) {
                                items(pending.risky, key = { it.path }) { item ->
                                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                                        Text(item.name, Modifier.weight(1f), maxLines = 1, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                                        Text(formatSize(item.size), color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
            ) { Text(stringResource(R.string.action_delete)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
fun DeletionProgressDialog(state: DeletionState.Running) {
    AlertDialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        title = { Text(stringResource(R.string.deleting_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LinearProgressIndicator(
                    progress = { if (state.total == 0) 0f else state.done.toFloat() / state.total },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.deleting_progress, state.done, state.total), style = MaterialTheme.typography.bodySmall)
                Text(state.currentName, maxLines = 1, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {},
    )
}

/** Message de fin de suppression affiché dans un Snackbar. */
@Composable
fun deletionReportMessage(report: DeletionReport): String {
    val context = LocalContext.current
    val res = context.resources
    val main = deletionReportMessage(report) { Formatter.formatShortFileSize(context, it) }.let { (id, args) ->
        res.getQuantityString(id, report.deletedCount, *args)
    }
    val extras = buildList {
        if (report.systemTrashedCount > 0) {
            add(res.getQuantityString(R.plurals.deletion_result_system_trash, report.systemTrashedCount, report.systemTrashedCount))
        }
        if (report.refusedCount > 0) {
            add(res.getQuantityString(R.plurals.deletion_result_refused, report.refusedCount, report.refusedCount))
        }
    }
    return (listOf(main) + extras).joinToString(" · ")
}

private fun deletionReportMessage(report: DeletionReport, format: (Long) -> String): Pair<Int, Array<Any>> {
    val freed = if (report.movedToTrash) report.expectedFreedBytes else maxOf(report.measuredFreedBytes, report.expectedFreedBytes)
    return when {
        report.failures.isNotEmpty() ->
            R.plurals.deletion_result_with_failures to arrayOf(report.deletedCount, format(freed), report.failures.size)
        report.movedToTrash ->
            R.plurals.deletion_result_trash to arrayOf(report.deletedCount, format(freed))
        else ->
            R.plurals.deletion_result to arrayOf(report.deletedCount, format(freed))
    }
}
