package com.apexus.storagelens.ui.screens.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
import com.apexus.storagelens.ui.components.SectionTitle
import com.apexus.storagelens.ui.util.formatDateTime
import com.apexus.storagelens.ui.util.formatSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(onBack: () -> Unit, viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_history)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item(key = "total") {
                Card(
                    Modifier.fillMaxWidth().padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.history_total_freed), style = MaterialTheme.typography.titleMedium)
                        Text(formatSize(state.totalFreed), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item(key = "cleanups_title") { SectionTitle(stringResource(R.string.history_cleanups)) }
            if (state.cleanups.isEmpty()) {
                item(key = "cleanups_empty") { ListItem(headlineContent = { Text(stringResource(R.string.history_none)) }) }
            }
            items(state.cleanups, key = { "c${it.id}" }) { entry ->
                ListItem(
                    leadingContent = { Icon(Icons.Filled.CleaningServices, contentDescription = null) },
                    headlineContent = { Text(stringResource(R.string.history_freed, formatSize(entry.freedBytes))) },
                    supportingContent = {
                        Text(
                            pluralStringResource(R.plurals.history_cleanup_detail, entry.itemCount, entry.itemCount, formatDateTime(entry.performedAt)) +
                                if (entry.movedToTrash) " · " + stringResource(R.string.history_via_trash) else "",
                        )
                    },
                )
            }
            item(key = "scans_title") { SectionTitle(stringResource(R.string.history_scans)) }
            if (state.scans.isEmpty()) {
                item(key = "scans_empty") { ListItem(headlineContent = { Text(stringResource(R.string.history_none)) }) }
            }
            items(state.scans, key = { "s${it.id}" }) { entry ->
                ListItem(
                    leadingContent = { Icon(Icons.Filled.Search, contentDescription = null) },
                    headlineContent = { Text("${entry.volumeLabel} · ${formatDateTime(entry.finishedAt)}") },
                    supportingContent = {
                        Text(
                            pluralStringResource(
                                R.plurals.history_scan_detail,
                                entry.fileCount,
                                entry.fileCount,
                                formatSize(entry.scannedBytes),
                                formatSize(entry.recoverableBytes),
                                entry.durationMs / 1000.0,
                            )
                        )
                    },
                )
            }
        }
    }
}
