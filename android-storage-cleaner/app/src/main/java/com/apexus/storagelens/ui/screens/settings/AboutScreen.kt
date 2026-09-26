package com.apexus.storagelens.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apexus.storagelens.BuildConfig
import com.apexus.storagelens.R

private enum class Capability { YES, ASSISTED, NO }

private data class CapabilityRow(val labelRes: Int, val android8to10: Capability, val android11plus: Capability)

private val CAPABILITIES = listOf(
    CapabilityRow(R.string.about_cap_shared_files, Capability.YES, Capability.YES),
    CapabilityRow(R.string.about_cap_logs_traces, Capability.YES, Capability.YES),
    CapabilityRow(R.string.about_cap_android_data, Capability.YES, Capability.NO),
    CapabilityRow(R.string.about_cap_app_cache, Capability.ASSISTED, Capability.ASSISTED),
    CapabilityRow(R.string.about_cap_system_logs, Capability.NO, Capability.NO),
    CapabilityRow(R.string.about_cap_app_sizes, Capability.YES, Capability.YES),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_about)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(R.string.app_name) + " " + BuildConfig.VERSION_NAME, style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(R.string.about_intro), style = MaterialTheme.typography.bodyMedium)

            ElevatedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.about_limits_title), style = MaterialTheme.typography.titleMedium)
                    Row {
                        Text("", Modifier.weight(2f))
                        Text(stringResource(R.string.about_col_android8), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                        Text(stringResource(R.string.about_col_android11), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    }
                    CAPABILITIES.forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(row.labelRes), Modifier.weight(2f), style = MaterialTheme.typography.bodySmall)
                            CapabilityCell(row.android8to10, Modifier.weight(1f))
                            CapabilityCell(row.android11plus, Modifier.weight(1f))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Legend(Icons.Filled.CheckCircle, capabilityColor(Capability.YES), stringResource(R.string.about_legend_yes))
                        Legend(Icons.Filled.TouchApp, capabilityColor(Capability.ASSISTED), stringResource(R.string.about_legend_assisted))
                        Legend(Icons.Filled.Block, capabilityColor(Capability.NO), stringResource(R.string.about_legend_no))
                    }
                }
            }
            Text(stringResource(R.string.about_system_explained), style = MaterialTheme.typography.bodyMedium)
            Text(stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun capabilityColor(c: Capability): Color = when (c) {
    Capability.YES -> Color(0xFF2E7D32)
    Capability.ASSISTED -> Color(0xFFB26A00)
    Capability.NO -> Color(0xFFC62828)
}

private fun capabilityIcon(c: Capability): ImageVector = when (c) {
    Capability.YES -> Icons.Filled.CheckCircle
    Capability.ASSISTED -> Icons.Filled.TouchApp
    Capability.NO -> Icons.Filled.Block
}

@Composable
private fun CapabilityCell(c: Capability, modifier: Modifier) {
    val description = stringResource(
        when (c) {
            Capability.YES -> R.string.about_legend_yes
            Capability.ASSISTED -> R.string.about_legend_assisted
            Capability.NO -> R.string.about_legend_no
        }
    )
    Box(modifier) {
        Icon(capabilityIcon(c), contentDescription = description, tint = capabilityColor(c), modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun Legend(icon: ImageVector, color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Text(label, Modifier.padding(start = 4.dp), style = MaterialTheme.typography.labelSmall)
    }
}
