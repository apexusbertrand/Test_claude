package com.apexus.storagelens.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apexus.storagelens.R
import com.apexus.storagelens.ui.util.formatSize

/** Jauge circulaire animée : espace utilisé sur la capacité totale. */
@Composable
fun StorageRing(
    usedBytes: Long,
    totalBytes: Long,
    modifier: Modifier = Modifier,
    diameter: Dp = 180.dp,
) {
    val fraction = if (totalBytes <= 0) 0f else (usedBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
    val animated = remember { Animatable(0f) }
    LaunchedEffect(fraction) { animated.animateTo(fraction, tween(durationMillis = 900)) }

    val track = MaterialTheme.colorScheme.surfaceVariant
    val progressColor = when {
        fraction > 0.9f -> MaterialTheme.colorScheme.error
        fraction > 0.75f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val percentText = "${(fraction * 100).toInt()} %"
    val description = stringResource(R.string.a11y_storage_ring, formatSize(usedBytes), formatSize(totalBytes))

    Box(
        modifier = modifier
            .size(diameter)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 18.dp.toPx()
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2, stroke / 2)
            drawArc(track, startAngle = 135f, sweepAngle = 270f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(progressColor, startAngle = 135f, sweepAngle = 270f * animated.value, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(percentText, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.home_used), style = MaterialTheme.typography.bodySmall)
        }
    }
}
