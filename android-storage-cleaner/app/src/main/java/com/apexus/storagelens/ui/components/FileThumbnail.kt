package com.apexus.storagelens.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.apexus.storagelens.domain.model.FileType
import com.apexus.storagelens.domain.model.FileTypes
import com.apexus.storagelens.ui.util.iconForFile
import java.io.File

/** Miniature pour les images et vidéos, icône de type pour le reste. */
@Composable
fun FileThumbnail(path: String, name: String, isDirectory: Boolean, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val type = if (isDirectory) null else FileTypes.of(name)
    val shape = RoundedCornerShape(8.dp)
    val fallback = @Composable {
        Box(
            Modifier.size(size).clip(shape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Icon(iconForFile(name, isDirectory), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
    if (type == FileType.IMAGE || type == FileType.VIDEO) {
        SubcomposeAsyncImage(
            model = File(path),
            contentDescription = null,
            modifier = modifier.size(size).clip(shape),
            contentScale = ContentScale.Crop,
            loading = { fallback() },
            error = { fallback() },
        )
    } else {
        Box(modifier) { fallback() }
    }
}
