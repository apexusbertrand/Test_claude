package com.apexus.storagelens.ui.screens.system

import android.content.Intent
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
import com.apexus.storagelens.ui.util.FileActions
import com.apexus.storagelens.ui.util.formatSize

/** Explique le contenu de « Système et autres » et donne des raccourcis pour le réduire. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemStorageScreen(
    onBack: () -> Unit,
    onOpenTrash: () -> Unit,
    viewModel: SystemStorageViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    fun open(intent: Intent) = FileActions.startSafely(context, intent)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category_system)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.systemBytes?.let {
                        Text(formatSize(it), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
                    }
                    Text(stringResource(R.string.system_intro), style = MaterialTheme.typography.bodyMedium)
                }
            }

            Text(stringResource(R.string.system_contents_title), style = MaterialTheme.typography.titleMedium)

            SystemItem(
                icon = Icons.Filled.SystemUpdate,
                title = stringResource(R.string.system_item_os_title),
                size = stringResource(R.string.system_item_os_size),
                text = stringResource(R.string.system_item_os_text),
            )
            if (state.isSamsung) {
                SystemItem(
                    icon = Icons.Filled.Memory,
                    title = stringResource(R.string.system_item_ramplus_title),
                    size = stringResource(R.string.system_item_ramplus_size),
                    text = stringResource(R.string.system_item_ramplus_text),
                    actionLabel = stringResource(R.string.system_action_device_care),
                    onAction = state.deviceCareIntent?.let { intent -> { open(intent) } },
                )
            }
            SystemItem(
                icon = Icons.Filled.PhotoLibrary,
                title = stringResource(R.string.system_item_trash_title),
                size = stringResource(R.string.system_size_variable),
                text = stringResource(R.string.system_item_trash_text),
                actionLabel = stringResource(R.string.system_action_gallery),
                onAction = state.galleryIntent?.let { intent -> { open(intent) } },
                secondaryLabel = stringResource(R.string.system_action_files),
                onSecondary = state.filesIntent?.let { intent -> { open(intent) } },
            )
            SystemItem(
                icon = Icons.Filled.Delete,
                title = stringResource(R.string.system_item_app_trash_title),
                size = formatSize(state.appTrashBytes),
                text = stringResource(R.string.system_item_app_trash_text),
                actionLabel = stringResource(R.string.system_action_app_trash),
                onAction = onOpenTrash,
            )
            if (state.isSamsung) {
                SystemItem(
                    icon = Icons.Filled.Lock,
                    title = stringResource(R.string.system_item_secure_title),
                    size = stringResource(R.string.system_size_variable),
                    text = stringResource(R.string.system_item_secure_text),
                )
            }
            SystemItem(
                icon = Icons.Filled.RestartAlt,
                title = stringResource(R.string.system_item_temp_title),
                size = stringResource(R.string.system_item_temp_size),
                text = stringResource(R.string.system_item_temp_text),
            )
            if (state.inaccessibleDirectories > 0) {
                SystemItem(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.system_item_protected_title),
                    size = stringResource(R.string.system_size_unknown),
                    text = stringResource(R.string.system_item_protected_text, state.inaccessibleDirectories),
                )
            }

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.system_realistic_title), style = MaterialTheme.typography.titleSmall)
                    Text(stringResource(R.string.system_realistic_text), style = MaterialTheme.typography.bodySmall)
                    FilledTonalButton(onClick = { open(state.storageSettingsIntent) }) {
                        Icon(Icons.Filled.Settings, contentDescription = null)
                        Text(stringResource(R.string.system_action_android_storage), Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemItem(
    icon: ImageVector,
    title: String,
    size: String,
    text: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.titleSmall)
                Text(size, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(text, style = MaterialTheme.typography.bodySmall)
            if ((actionLabel != null && onAction != null) || (secondaryLabel != null && onSecondary != null)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (actionLabel != null && onAction != null) FilledTonalButton(onClick = onAction) { Text(actionLabel) }
                    if (secondaryLabel != null && onSecondary != null) FilledTonalButton(onClick = onSecondary) { Text(secondaryLabel) }
                }
            }
        }
    }
}
