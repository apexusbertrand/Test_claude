package com.apexus.storagelens.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.apexus.storagelens.domain.model.RiskLevel
import com.apexus.storagelens.domain.model.StorageCategory

/** Palette fixe, lisible en thème clair comme sombre. */
fun StorageCategory.color(): Color = when (this) {
    StorageCategory.APPS -> Color(0xFF3F7EDB)
    StorageCategory.IMAGES -> Color(0xFF2EA37A)
    StorageCategory.VIDEOS -> Color(0xFFD9534F)
    StorageCategory.AUDIO -> Color(0xFFE39A2D)
    StorageCategory.DOCUMENTS -> Color(0xFF8E6CCF)
    StorageCategory.ARCHIVES -> Color(0xFF8D6E63)
    StorageCategory.APK -> Color(0xFF26A6B8)
    StorageCategory.CACHE_TEMP -> Color(0xFFC2C23A)
    StorageCategory.LOGS_TRACES -> Color(0xFFD16BA5)
    StorageCategory.OTHER -> Color(0xFF7D8A99)
    StorageCategory.SYSTEM -> Color(0xFF4A4F57)
}

@Composable
fun RiskLevel.color(): Color = when (this) {
    RiskLevel.LOW -> Color(0xFF2E7D32)
    RiskLevel.MEDIUM -> Color(0xFFB26A00)
    RiskLevel.HIGH -> MaterialTheme.colorScheme.error
}

/** Couleur de cellule de treemap dérivée du chemin (stable d'une analyse à l'autre). */
fun treemapColor(key: String, isDirectory: Boolean): Color {
    val palette = listOf(
        Color(0xFF3F7EDB), Color(0xFF2EA37A), Color(0xFFD9534F), Color(0xFFE39A2D),
        Color(0xFF8E6CCF), Color(0xFF26A6B8), Color(0xFFD16BA5), Color(0xFF7D8A99),
    )
    val base = palette[(key.hashCode() and Int.MAX_VALUE) % palette.size]
    return if (isDirectory) base else base.copy(alpha = 0.7f)
}
