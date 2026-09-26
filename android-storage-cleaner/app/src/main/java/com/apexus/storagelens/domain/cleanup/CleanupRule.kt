package com.apexus.storagelens.domain.cleanup

import com.apexus.storagelens.domain.model.CleanupCandidate
import com.apexus.storagelens.domain.model.CleanupCategory
import com.apexus.storagelens.domain.model.FileNode
import com.apexus.storagelens.domain.scan.ScannedFile

/** Contexte fourni aux règles. */
data class RuleContext(
    val storageRoot: String,
    val protectedPaths: ProtectedPaths,
    val installedPackages: Set<String>,
    val nowMillis: Long,
    val largeFileThresholdBytes: Long,
    val oldFileAgeMillis: Long,
    /** Renvoie le nom de paquet contenu dans un APK, ou null (fourni par la couche Android). */
    val apkPackageResolver: (String) -> String? = { null },
)

/** Règle de détection de fichiers inutiles ou d'espace caché. */
sealed interface CleanupRule {
    val category: CleanupCategory
}

/** Évaluée pour chaque fichier pendant le parcours (doit être rapide et thread-safe). */
interface FileRule : CleanupRule {
    fun match(file: ScannedFile, ctx: RuleContext): CleanupCandidate?
}

/** Évaluée pour chaque dossier une fois celui-ci entièrement parcouru. */
interface DirectoryRule : CleanupRule {
    fun match(dir: FileNode, ctx: RuleContext): CleanupCandidate?
}

/** Évaluée après le parcours complet (ex. : doublons). */
interface PostScanRule : CleanupRule {
    suspend fun find(ctx: RuleContext): List<CleanupCandidate>
}

internal fun ScannedFile.candidate(
    category: CleanupCategory,
    recommended: Boolean = category.selectedByDefault,
    detail: String? = null,
    groupKey: String? = null,
) = CleanupCandidate(path, name, size, lastModified, isDirectory = false, category, recommended, detail, groupKey)

internal fun FileNode.candidate(
    category: CleanupCategory,
    recommended: Boolean = category.selectedByDefault,
    detail: String? = null,
) = CleanupCandidate(path, name, size, lastModified, isDirectory = true, category, recommended, detail)
