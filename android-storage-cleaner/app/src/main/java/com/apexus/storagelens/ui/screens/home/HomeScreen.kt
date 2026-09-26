package com.apexus.storagelens.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
import com.apexus.storagelens.domain.model.StorageVolumeInfo
import com.apexus.storagelens.ui.components.CategoryBreakdown
import com.apexus.storagelens.ui.components.InfoBanner
import com.apexus.storagelens.ui.components.SkeletonList
import com.apexus.storagelens.ui.components.StorageRing
import com.apexus.storagelens.ui.util.formatRelative
import com.apexus.storagelens.ui.util.formatSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartScan: () -> Unit,
    onOpenCleanup: () -> Unit,
    onOpenExplorer: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.a11y_more_options))
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.nav_trash)) }, leadingIcon = { Icon(Icons.Filled.Delete, null) }, onClick = { menuOpen = false; onOpenTrash() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.nav_history)) }, leadingIcon = { Icon(Icons.Filled.History, null) }, onClick = { menuOpen = false; onOpenHistory() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.nav_settings)) }, leadingIcon = { Icon(Icons.Filled.Settings, null) }, onClick = { menuOpen = false; onOpenSettings() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.nav_about)) }, leadingIcon = { Icon(Icons.Filled.Info, null) }, onClick = { menuOpen = false; onOpenAbout() })
                    }
                },
            )
        },
    ) { padding ->
        if (state.loading) {
            SkeletonList(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.permissions?.allFilesAccess == false) {
                InfoBanner(
                    icon = Icons.Filled.Lock,
                    text = stringResource(R.string.home_permission_missing),
                    warning = true,
                    actionLabel = stringResource(R.string.action_grant),
                    onAction = onOpenPermissions,
                )
            }
            if (state.stale) {
                InfoBanner(
                    icon = Icons.Filled.Refresh,
                    text = stringResource(R.string.home_result_stale),
                    actionLabel = stringResource(R.string.action_rescan),
                    onAction = { viewModel.startScan(); onStartScan() },
                )
            }

            if (state.volumes.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.volumes.forEach { volume ->
                        FilterChip(
                            selected = volume.id == state.selectedVolume?.id,
                            onClick = { viewModel.selectVolume(volume.id) },
                            label = { Text(volume.label) },
                            leadingIcon = { Icon(volumeIcon(volume), contentDescription = null) },
                        )
                    }
                }
            }

            state.selectedVolume?.let { volume -> VolumeCard(volume) }

            val result = state.resultForSelected
            if (result != null) {
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.home_breakdown), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        CategoryBreakdown(result.categories, result.volume.totalBytes)
                        if (result.inaccessibleDirectories > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.home_inaccessible_dirs, result.inaccessibleDirectories),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                Card(
                    Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(stringResource(R.string.home_recoverable), style = MaterialTheme.typography.titleMedium)
                        Text(
                            formatSize(result.recoverableBytes),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            stringResource(R.string.home_recoverable_recommended, formatSize(result.recommendedBytes)),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = onOpenCleanup) {
                                Icon(Icons.Filled.CleaningServices, contentDescription = null)
                                Text(stringResource(R.string.action_clean), Modifier.padding(start = 8.dp))
                            }
                            FilledTonalButton(onClick = onOpenExplorer) { Text(stringResource(R.string.action_explore)) }
                        }
                    }
                }
            } else {
                InfoBanner(icon = Icons.Filled.Info, text = stringResource(R.string.home_no_result))
            }

            Button(
                onClick = { viewModel.startScan(); onStartScan() },
                enabled = state.permissions?.allFilesAccess == true,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Search, contentDescription = null)
                Text(
                    stringResource(if (state.scanRunning) R.string.home_scan_running else R.string.home_start_scan),
                    Modifier.padding(start = 8.dp),
                )
            }
            state.lastScan?.let {
                Text(
                    stringResource(R.string.home_last_scan, formatRelative(it.finishedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun VolumeCard(volume: StorageVolumeInfo) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(volumeIcon(volume), contentDescription = null)
                Text(volume.label, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
            StorageRing(usedBytes = volume.usedBytes, totalBytes = volume.totalBytes)
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Stat(stringResource(R.string.home_used), formatSize(volume.usedBytes))
                Stat(stringResource(R.string.home_free), formatSize(volume.freeBytes))
                Stat(stringResource(R.string.home_total), formatSize(volume.totalBytes))
            }
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun volumeIcon(volume: StorageVolumeInfo) = when {
    volume.isPrimary -> Icons.Filled.Smartphone
    volume.label.contains("USB", ignoreCase = true) -> Icons.Filled.Usb
    else -> Icons.Filled.SdCard
}
