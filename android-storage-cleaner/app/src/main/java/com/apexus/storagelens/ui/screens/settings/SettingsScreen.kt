package com.apexus.storagelens.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
import com.apexus.storagelens.data.prefs.ScheduleMode
import com.apexus.storagelens.data.prefs.ThemeMode
import com.apexus.storagelens.ui.components.SectionTitle
import com.apexus.storagelens.ui.util.formatSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var confirmRoot by rememberSaveable { mutableStateOf(false) }
    var newExclusion by rememberSaveable { mutableStateOf("") }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        val s = settings ?: return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            SectionTitle(stringResource(R.string.settings_detection))
            ChoiceRow(
                title = stringResource(R.string.settings_large_threshold),
                options = listOf(50, 100, 250, 500, 1000),
                selected = s.largeFileThresholdMb,
                label = { formatSize(it * 1024L * 1024L) },
                onSelect = viewModel::setLargeFileThreshold,
            )
            ChoiceRow(
                title = stringResource(R.string.settings_old_months),
                options = listOf(3, 6, 12, 24),
                selected = s.oldFileMonths,
                label = { pluralStringResource(R.plurals.months, it, it) },
                onSelect = viewModel::setOldFileMonths,
            )
            SwitchRow(stringResource(R.string.settings_show_hidden), stringResource(R.string.settings_show_hidden_desc), s.showHiddenFiles, viewModel::setShowHidden)
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_trash))
            SwitchRow(stringResource(R.string.settings_trash_enabled), stringResource(R.string.settings_trash_enabled_desc), s.trashEnabled, viewModel::setTrashEnabled)
            if (s.trashEnabled) {
                ChoiceRow(
                    title = stringResource(R.string.settings_retention),
                    options = listOf(3, 7, 14, 30),
                    selected = s.trashRetentionDays,
                    label = { pluralStringResource(R.plurals.days, it, it) },
                    onSelect = viewModel::setRetentionDays,
                )
            }
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_schedule))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ScheduleMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = s.scheduleMode == mode,
                            onClick = {
                                viewModel.setSchedule(mode)
                                if (mode != ScheduleMode.OFF && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            },
                            shape = SegmentedButtonDefaults.itemShape(index, ScheduleMode.entries.size),
                        ) { Text(stringResource(mode.labelRes())) }
                    }
                }
                Text(
                    stringResource(R.string.settings_schedule_desc),
                    Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (s.scheduleMode != ScheduleMode.OFF) {
                ChoiceRow(
                    title = stringResource(R.string.settings_notify_threshold),
                    options = listOf(250, 500, 1000, 2000),
                    selected = s.notifyThresholdMb,
                    label = { formatSize(it * 1024L * 1024L) },
                    onSelect = viewModel::setNotifyThreshold,
                )
            }
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_exclusions))
            Text(
                stringResource(R.string.settings_exclusions_desc),
                Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            s.exclusions.sorted().forEach { path ->
                ListItem(
                    headlineContent = { Text(path, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.removeExclusion(path) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.a11y_remove_exclusion, path))
                        }
                    },
                )
            }
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newExclusion,
                    onValueChange = { newExclusion = it },
                    label = { Text(stringResource(R.string.settings_exclusion_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = { viewModel.addExclusion(newExclusion); newExclusion = "" }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add))
                }
            }
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_appearance))
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = s.themeMode == mode,
                            onClick = { viewModel.setTheme(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                        ) { Text(stringResource(mode.labelRes())) }
                    }
                }
            }
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_advanced))
            SwitchRow(
                stringResource(R.string.settings_root_mode),
                stringResource(R.string.settings_root_mode_desc),
                s.advancedRootMode,
            ) { enabled -> if (enabled) confirmRoot = true else viewModel.setRootMode(false) }
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenPermissions),
                leadingContent = { Icon(Icons.Filled.Security, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.settings_permissions)) },
            )
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenAbout),
                leadingContent = { Icon(Icons.Filled.Info, contentDescription = null) },
                headlineContent = { Text(stringResource(R.string.nav_about)) },
            )
        }
    }

    if (confirmRoot) {
        AlertDialog(
            onDismissRequest = { confirmRoot = false },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text(stringResource(R.string.settings_root_confirm_title)) },
            text = { Text(stringResource(R.string.settings_root_confirm_text)) },
            confirmButton = { Button(onClick = { confirmRoot = false; viewModel.setRootMode(true) }) { Text(stringResource(R.string.action_enable)) } },
            dismissButton = { TextButton(onClick = { confirmRoot = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun SwitchRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        modifier = Modifier.clickable { onChange(!checked) },
        headlineContent = { Text(title) },
        supportingContent = { Text(description) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
    )
}

@Composable
private fun ChoiceRow(
    title: String,
    options: List<Int>,
    selected: Int,
    label: @Composable (Int) -> String,
    onSelect: (Int) -> Unit,
) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(label(option)) })
            }
        }
    }
}

private fun ScheduleMode.labelRes(): Int = when (this) {
    ScheduleMode.OFF -> R.string.schedule_off
    ScheduleMode.WEEKLY -> R.string.schedule_weekly
    ScheduleMode.MONTHLY -> R.string.schedule_monthly
}

private fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.SYSTEM -> R.string.theme_system
    ThemeMode.LIGHT -> R.string.theme_light
    ThemeMode.DARK -> R.string.theme_dark
}
