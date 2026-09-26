package com.apexus.storagelens.domain.model

/**
 * Nœud de l'arborescence analysée.
 *
 * Pour un dossier, [size] et [fileCount] sont récursifs. Afin de maîtriser la mémoire, seuls
 * les plus gros fichiers de chaque dossier sont conservés dans [children] ; les autres sont
 * agrégés dans [hiddenFilesCount] / [hiddenFilesSize].
 */
data class FileNode(
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val fileCount: Int,
    val lastModified: Long,
    val children: List<FileNode> = emptyList(),
    val hiddenFilesCount: Int = 0,
    val hiddenFilesSize: Long = 0L,
) {
    val directoryCount: Int get() = children.count { it.isDirectory }

    /** Recherche un descendant par chemin absolu (null s'il n'est pas dans l'arbre). */
    fun find(targetPath: String): FileNode? {
        if (targetPath == path) return this
        if (!targetPath.startsWith("$path/")) return null
        return children.firstNotNullOfOrNull { if (it.isDirectory) it.find(targetPath) else if (it.path == targetPath) it else null }
    }
}

/** Fichier individuel (utilisé pour le top des plus gros fichiers). */
data class FileEntry(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val type: FileType,
)
