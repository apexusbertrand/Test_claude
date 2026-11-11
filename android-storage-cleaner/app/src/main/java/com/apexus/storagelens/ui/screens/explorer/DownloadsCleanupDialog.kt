package com.apexus.storagelens.ui.screens.explorer

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.apexus.storagelens.R
import com.apexus.storagelens.domain.downloads.DownloadsCleanup
import com.apexus.storagelens.ui.util.formatDate
import com.apexus.storagelens.ui.util.formatSize

private val PRESET_DAYS = listOf(7, 30, 90, 180, 365)
private const val PREVIEW_LIMIT = 100

/** Fenêtre « Vider les téléchargements » : choix de l'ancienneté, aperçu, confirmation. */
@Composable
fun DownloadsCleanupDialog(
    state: DownloadsCleanupUi,
    trashEnabled: Boolean,
    onSearch: (Int) -> Unit,
    onEditDays: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    when (state) {
        is DownloadsCleanupUi.Choosing -> ChooseDaysDialog(state.days, onSearch, onDismiss)
        is DownloadsCleanupUi.Loading -> AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(Icons.Filled.Download, contentDescription = null) },
            title = { Text(stringResource(R.string.downloads_title)) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                    Text(stringResource(R.string.downloads_searching), Modifier.padding(start = 16.dp))
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        )
        is DownloadsCleanupUi.Preview -> PreviewDialog(state, trashEnabled, onEditDays, onConfirm, onDismiss)
    }
}

@Composable
private fun ChooseDaysDialog(initialDays: Int, onSearch: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initialDays.toString()) }
    val days = text.toIntOrNull()
    val valid = days != null && days in DownloadsCleanup.MIN_DAYS..DownloadsCleanup.MAX_DAYS
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Download, contentDescription = null) },
        title = { Text(stringResource(R.string.downloads_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.downloads_choose_text))
                OutlinedTextField(
                    value = text,
                    onValueChange = { value -> text = value.filter { it.isDigit() }.take(4) },
                    label = { Text(stringResource(R.string.downloads_days_label)) },
                    suffix = { Text(stringResource(R.string.downloads_days_suffix)) },
                    singleLine = true,
                    isError = !valid,
                    supportingText = {
                        if (valid) {
                            val cutoff = DownloadsCleanup.cutoff(System.currentTimeMillis(), days ?: DownloadsCleanup.DEFAULT_DAYS)
                            Text(stringResource(R.string.downloads_cutoff, formatDate(cutoff)))
                        } else {
                            Text(stringResource(R.string.downloads_days_invalid, DownloadsCleanup.MIN_DAYS, DownloadsCleanup.MAX_DAYS))
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PRESET_DAYS.forEach { preset ->
                        FilterChip(
                            selected = days == preset,
                            onClick = { text = preset.toString() },
                            label = { Text(pluralStringResource(R.plurals.days, preset, preset)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { days?.let(onSearch) }, enabled = valid) { Text(stringResource(R.string.downloads_search)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun PreviewDialog(
    state: DownloadsCleanupUi.Preview,
    trashEnabled: Boolean,
    onEditDays: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val count = state.files.size
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Download, contentDescription = null) },
        title = {
            Text(
                if (count == 0) stringResource(R.string.downloads_none_title)
                else pluralStringResource(R.plurals.downloads_preview_title, count, count, formatSize(state.totalBytes))
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.downloads_cutoff, formatDate(state.cutoffMillis)), style = MaterialTheme.typography.bodyMedium)
                if (count == 0) {
                    Text(pluralStringResource(R.plurals.downloads_none_text, state.days, state.days))
                } else {
                    LazyColumn(Modifier.heightIn(max = 280.dp)) {
                        items(state.files.take(PREVIEW_LIMIT), key = { it.path }) { file ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(file.name, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                                    Text(formatDate(file.lastModified), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(formatSize(file.size), Modifier.padding(start = 8.dp), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                        if (count > PREVIEW_LIMIT) {
                            item(key = "more") {
                                Text(
                                    pluralStringResource(R.plurals.cleanup_more_items, count - PREVIEW_LIMIT, count - PREVIEW_LIMIT),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(if (trashEnabled) R.string.downloads_note_trash else R.string.downloads_note_permanent),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            if (count > 0) {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                ) { Text(stringResource(R.string.action_delete)) }
            } else {
                Button(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
            }
        },
        dismissButton = { TextButton(onClick = onEditDays) { Text(stringResource(R.string.downloads_change_days)) } },
    )
}
