package com.apexus.storagelens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.apexus.storagelens.domain.model.FileNode
import com.apexus.storagelens.domain.treemap.Squarify
import com.apexus.storagelens.domain.treemap.TreemapRect
import com.apexus.storagelens.ui.theme.treemapColor
import com.apexus.storagelens.ui.util.formatSize
import kotlin.math.roundToInt

/** Treemap cliquable des enfants d'un dossier : la surface de chaque cellule est proportionnelle à sa taille. */
@Composable
fun Treemap(node: FileNode, onOpen: (FileNode) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val density = LocalDensity.current
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val cells = remember(node, widthPx, heightPx) {
            Squarify.layout(node.children.take(MAX_CELLS), { it.size }, TreemapRect(0f, 0f, widthPx, heightPx))
        }
        cells.forEach { cell ->
            val child = cell.item
            val rect = cell.rect
            val w = with(density) { rect.width.toDp() }
            val h = with(density) { rect.height.toDp() }
            val sizeLabel = formatSize(child.size)
            Box(
                Modifier
                    .offset { IntOffset(rect.x.roundToInt(), rect.y.roundToInt()) }
                    .size(w, h)
                    .background(treemapColor(child.path, child.isDirectory))
                    .border(1.dp, MaterialTheme.colorScheme.surface)
                    .clickable { onOpen(child) }
                    .semantics { contentDescription = "${child.name}, $sizeLabel" }
            ) {
                if (w > 56.dp && h > 32.dp) {
                    Column(Modifier.padding(4.dp)) {
                        Text(child.name, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (h > 48.dp) {
                            Text(sizeLabel, color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

private const val MAX_CELLS = 150
