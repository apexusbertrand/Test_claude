package com.apexus.storagelens.domain.model

enum class RiskLevel { LOW, MEDIUM, HIGH }

/** Catégories de fichiers inutiles / d'espace caché, avec leur niveau de risque. */
enum class CleanupCategory(val risk: RiskLevel, val selectedByDefault: Boolean) {
    LOGS(RiskLevel.LOW, true),
    TRACES(RiskLevel.LOW, true),
    TEMP_FILES(RiskLevel.LOW, true),
    CACHES(RiskLevel.LOW, true),
    THUMBNAILS(RiskLevel.LOW, true),
    EMPTY_FOLDERS(RiskLevel.LOW, true),
    APK_FILES(RiskLevel.LOW, true),
    ORPHAN_APP_DATA(RiskLevel.MEDIUM, false),
    TRASH(RiskLevel.MEDIUM, false),
    DUPLICATES(RiskLevel.MEDIUM, false),
    MESSAGING_MEDIA(RiskLevel.HIGH, false),
    LARGE_OLD_FILES(RiskLevel.HIGH, false),
}

/**
 * Élément proposé à la suppression.
 * [recommended] indique s'il fait partie de la « sélection recommandée ».
 */
data class CleanupCandidate(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val isDirectory: Boolean,
    val category: CleanupCategory,
    val recommended: Boolean,
    val detail: String? = null,
    val groupKey: String? = null,
) {
    val parentPath: String get() = path.substringBeforeLast('/', "")
}

data class CleanupGroup(
    val category: CleanupCategory,
    val candidates: List<CleanupCandidate>,
) {
    val totalBytes: Long get() = candidates.sumOf { it.size }
    val count: Int get() = candidates.size
}
