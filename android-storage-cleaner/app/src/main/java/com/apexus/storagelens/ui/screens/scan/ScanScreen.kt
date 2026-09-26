package com.apexus.storagelens.ui.screens.scan

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexus.storagelens.R
import com.apexus.storagelens.domain.model.ScanPhase
import com.apexus.storagelens.domain.model.ScanState
import com.apexus.storagelens.ui.util.formatSize

@Composable
fun ScanScreen(onFinished: () -> Unit, viewModel: ScanViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is ScanState.Done || state is ScanState.Idle) onFinished()
    }

    Scaffold { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            when (val s = state) {
                is ScanState.Running -> Running(s, onCancel = viewModel::cancel)
                is ScanState.Failed -> {
                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.scan_failed), style = MaterialTheme.typography.titleLarge)
                    Text(s.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = viewModel::retry) { Text(stringResource(R.string.action_retry)) }
                    OutlinedButton(onClick = onFinished) { Text(stringResource(R.string.action_close)) }
                }
                else -> CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun Running(state: ScanState.Running, onCancel: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(0.9f, 1.1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "scale")

    Box(contentAlignment = Alignment.Center) {
        val fraction = state.fraction
        if (fraction != null) {
            CircularProgressIndicator(progress = { fraction }, modifier = Modifier.size(180.dp), strokeWidth = 10.dp)
        } else {
            CircularProgressIndicator(modifier = Modifier.size(180.dp), strokeWidth = 10.dp)
        }
        Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(56.dp).scale(scale), tint = MaterialTheme.colorScheme.primary)
    }
    Spacer(Modifier.height(24.dp))
    val phase = when (state.phase) {
        ScanPhase.LISTING -> R.string.scan_phase_listing
        ScanPhase.DUPLICATES -> R.string.scan_phase_duplicates
        ScanPhase.ANALYZING -> R.string.scan_phase_analyzing
    }
    Text(stringResource(phase), style = MaterialTheme.typography.titleLarge)
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.scan_progress, state.filesScanned, formatSize(state.bytesScanned)),
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
    )
    Spacer(Modifier.height(8.dp))
    Text(
        state.currentPath,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(32.dp))
    OutlinedButton(onClick = onCancel) { Text(stringResource(R.string.action_cancel)) }
}
