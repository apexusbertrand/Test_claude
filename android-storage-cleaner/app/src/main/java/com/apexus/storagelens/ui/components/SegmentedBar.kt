package com.apexus.storagelens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.apexus.storagelens.domain.model.CategoryUsage
import com.apexus.storagelens.domain.model.StorageCategory
import com.apexus.storagelens.ui.theme.color
import com.apexus.storagelens.ui.util.formatSize
import com.apexus.storagelens.ui.util.icon
import com.apexus.storagelens.ui.util.labelRes

/** Barre horizontale segmentée par catégorie, suivie de sa légende. */
@Composable
fun CategoryBreakdown(
    categories: List<CategoryUsage>,
    totalBytes: Long,
    modifier: Modifier = Modifier,
    /** Si fourni, chaque ligne de la légende devient cliquable. */
    onCategoryClick: ((StorageCategory) -> Unit)? = null,
) {
    val total = totalBytes.coerceAtLeast(categories.sumOf { it.bytes }).coerceAtLeast(1)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            categories.forEach { usage ->
                val weight = usage.bytes.toFloat() / total
                if (weight > 0.002f) {
                    Box(
                        Modifier
                            .weight(weight)
                            .fillMaxWidth()
                            .height(20.dp)
                            .background(usage.category.color())
                    )
                }
            }
            val free = 1f - categories.sumOf { it.bytes }.toFloat() / total
            if (free > 0.002f) Box(Modifier.weight(free))
        }
        Box(Modifier.height(8.dp))
        categories.forEach { usage ->
            val clickable = if (onCategoryClick != null) {
                Modifier.clip(RoundedCornerShape(8.dp)).clickable { onCategoryClick(usage.category) }
            } else {
                Modifier
            }
            Row(
                clickable.fillMaxWidth().heightIn(min = 40.dp).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(usage.category.color()))
                Icon(
                    usage.category.icon(),
                    contentDescription = null,
                    modifier = Modifier.padding(start = 8.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    stringResource(usage.category.labelRes()),
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(formatSize(usage.bytes), style = MaterialTheme.typography.bodyMedium)
                if (onCategoryClick != null) {
                    Icon(
                        Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Box(Modifier.width(4.dp))
                }
            }
        }
    }
}
