package com.apexus.storagelens.domain.scan

import com.apexus.storagelens.domain.model.FileNode

/** Élément supprimé, utilisé pour mettre à jour l'arbre sans relancer d'analyse. */
data class RemovedEntry(val path: String, val size: Long, val fileCount: Int)

object TreeOps {

    /** Ne conserve que les entrées dont aucun ancêtre n'est lui-même supprimé. */
    fun normalize(removed: Collection<RemovedEntry>): List<RemovedEntry> {
        val paths = removed.map { it.path }.toHashSet()
        return removed.distinctBy { it.path }.filter { entry ->
            var parent = entry.path.substringBeforeLast('/', "")
            while (parent.isNotEmpty()) {
                if (parent in paths) return@filter false
                parent = parent.substringBeforeLast('/', "")
            }
            true
        }
    }

    /** Retourne une copie de l'arbre sans les éléments supprimés, tailles et compteurs ajustés. */
    fun remove(root: FileNode, removed: Collection<RemovedEntry>): FileNode {
        val entries = normalize(removed)
        if (entries.isEmpty()) return root
        return removeNormalized(root, entries.associateBy { it.path }) ?: root.copy(
            size = 0, fileCount = 0, children = emptyList(), hiddenFilesCount = 0, hiddenFilesSize = 0,
        )
    }

    private fun removeNormalized(node: FileNode, removed: Map<String, RemovedEntry>): FileNode? {
        if (node.path in removed) return null
        if (!node.isDirectory) return node
        val prefix = node.path + "/"
        val below = removed.values.filter { it.path.startsWith(prefix) }
        if (below.isEmpty()) return node

        val childPaths = node.children.map { it.path }.toHashSet()
        val hiddenRemoved = below.filter { it.path.substringBeforeLast('/') == node.path && it.path !in childPaths }
        val newChildren = node.children.mapNotNull { child ->
            if (child.isDirectory) removeNormalized(child, removed) else child.takeIf { it.path !in removed }
        }
        return node.copy(
            size = (node.size - below.sumOf { it.size }).coerceAtLeast(0),
            fileCount = (node.fileCount - below.sumOf { it.fileCount }).coerceAtLeast(0),
            children = newChildren.sortedByDescending { it.size },
            hiddenFilesCount = (node.hiddenFilesCount - hiddenRemoved.size).coerceAtLeast(0),
            hiddenFilesSize = (node.hiddenFilesSize - hiddenRemoved.sumOf { it.size }).coerceAtLeast(0),
        )
    }
}
