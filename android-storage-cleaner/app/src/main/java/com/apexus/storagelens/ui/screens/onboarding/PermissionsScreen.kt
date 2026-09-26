package com.apexus.storagelens.ui.screens.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
import com.apexus.storagelens.ui.util.FileActions

/** Explique chaque accès demandé, pourquoi, et ce qui ne fonctionnera pas sans lui. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    fromOnboarding: Boolean,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val legacyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { viewModel.refresh() }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.permissions_title)) },
                navigationIcon = {
                    if (!fromOnboarding) {
                        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.permissions_intro), style = MaterialTheme.typography.bodyMedium)

            PermissionCard(
                icon = Icons.Filled.Folder,
                title = stringResource(R.string.permission_files_title),
                why = stringResource(R.string.permission_files_why),
                without = stringResource(R.string.permission_files_without),
                granted = status.allFilesAccess,
                required = true,
                onGrant = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        FileActions.startSafely(context, *viewModel.allFilesIntents())
                    } else {
                        legacyLauncher.launch(viewModel.legacyPermissions())
                    }
                },
            )
            PermissionCard(
                icon = Icons.Filled.BarChart,
                title = stringResource(R.string.permission_usage_title),
                why = stringResource(R.string.permission_usage_why),
                without = stringResource(R.string.permission_usage_without),
                granted = status.usageAccess,
                required = false,
                onGrant = { FileActions.startSafely(context, viewModel.usageIntent()) },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PermissionCard(
                    icon = Icons.Filled.Notifications,
                    title = stringResource(R.string.permission_notif_title),
                    why = stringResource(R.string.permission_notif_why),
                    without = stringResource(R.string.permission_notif_without),
                    granted = status.notifications,
                    required = false,
                    onGrant = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                )
            }

            if (fromOnboarding) {
                Button(onClick = { viewModel.finishOnboarding(onDone) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(if (status.allFilesAccess) R.string.action_continue else R.string.permissions_continue_limited))
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    title: String,
    why: String,
    without: String,
    granted: Boolean,
    required: Boolean,
    onGrant: () -> Unit,
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(title, Modifier.padding(start = 12.dp).weight(1f), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(if (required) R.string.permission_required else R.string.permission_optional),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(why, style = MaterialTheme.typography.bodyMedium)
            Text(without, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.permission_granted), Modifier.padding(start = 8.dp), fontWeight = FontWeight.Bold)
                }
            } else {
                FilledTonalButton(onClick = onGrant) { Text(stringResource(R.string.action_grant)) }
            }
        }
    }
}
