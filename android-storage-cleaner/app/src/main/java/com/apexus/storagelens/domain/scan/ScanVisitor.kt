package com.apexus.storagelens.domain.scan

import com.apexus.storagelens.domain.model.FileNode

/** Fichier rencontré pendant le parcours. */
data class ScannedFile(
    val path: String,
    val name: String,
    val parentPath: String,
    val size: Long,
    val lastModified: Long,
)

/**
 * Observateur du parcours. Les implémentations DOIVENT être thread-safe :
 * plusieurs sous-arbres sont parcourus en parallèle.
 */
interface ScanVisitor {
    fun onFile(file: ScannedFile) {}

    /** Appelé une fois le dossier entièrement parcouru (ordre post-fixe, taille agrégée). */
    fun onDirectory(node: FileNode) {}
}

class CompositeVisitor(private val visitors: List<ScanVisitor>) : ScanVisitor {
    override fun onFile(file: ScannedFile) = visitors.forEach { it.onFile(file) }
    override fun onDirectory(node: FileNode) = visitors.forEach { it.onDirectory(node) }
}
