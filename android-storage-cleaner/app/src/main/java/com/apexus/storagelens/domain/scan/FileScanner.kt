package com.apexus.storagelens.domain.scan

import com.apexus.storagelens.domain.model.FileNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes

data class ScanOptions(
    /** Chemins (et leurs descendants) ignorés par le parcours. */
    val excludedPaths: Set<String> = emptySet(),
    /** Nombre maximum de fichiers conservés individuellement par dossier. */
    val maxFilesPerDirectory: Int = 200,
    /** Nombre de sous-arbres parcourus simultanément. */
    val parallelism: Int = 4,
)

/**
 * Parcours itératif (pile explicite, sans récursion) d'une arborescence.
 *
 * - Ne suit jamais les liens symboliques (pas de boucle ni de double comptage).
 * - Les sous-dossiers de la racine sont parcourus en parallèle, limités par un sémaphore.
 * - Annulable : l'état de la coroutine est vérifié régulièrement.
 * - Aucune dépendance Android : testable sur la JVM.
 */
class FileScanner {

    suspend fun scan(
        root: String,
        options: ScanOptions,
        visitor: ScanVisitor,
        counters: ScanCounters = ScanCounters(),
    ): FileNode = withContext(Dispatchers.IO) {
        val rootPath = Paths.get(root)
        val rootAttrs = readAttributes(rootPath)
            ?: throw IOException("Racine inaccessible : $root")
        require(rootAttrs.isDirectory) { "La racine doit être un dossier : $root" }

        val entries = listEntries(rootPath, counters)
        val rootFiles = ArrayList<FileNode>()
        val subDirectories = ArrayList<Pair<Path, BasicFileAttributes>>()
        for (entry in entries) {
            val attrs = readAttributes(entry) ?: continue
            if (attrs.isSymbolicLink || isExcluded(entry.toString(), options)) continue
            if (attrs.isDirectory) {
                subDirectories += entry to attrs
            } else if (attrs.isRegularFile) {
                rootFiles += visitFile(entry, attrs, visitor, counters)
            }
        }

        val semaphore = Semaphore(options.parallelism.coerceAtLeast(1))
        val childDirs = coroutineScope {
            subDirectories.map { (dir, attrs) ->
                async { semaphore.withPermit { walk(dir, attrs, options, visitor, counters) } }
            }.awaitAll()
        }

        val node = buildNode(
            path = rootPath.toString(),
            name = rootPath.fileName?.toString() ?: rootPath.toString(),
            lastModified = rootAttrs.lastModifiedTime().toMillis(),
            childDirs = childDirs,
            files = rootFiles,
            maxFiles = options.maxFilesPerDirectory,
        )
        visitor.onDirectory(node)
        node
    }

    private class Frame(
        val path: Path,
        val lastModified: Long,
        val entries: Iterator<Path>,
    ) {
        val childDirs = ArrayList<FileNode>()
        val files = ArrayList<FileNode>()
    }

    private suspend fun walk(
        start: Path,
        startAttrs: BasicFileAttributes,
        options: ScanOptions,
        visitor: ScanVisitor,
        counters: ScanCounters,
    ): FileNode {
        val stack = ArrayDeque<Frame>()
        stack.addLast(Frame(start, startAttrs.lastModifiedTime().toMillis(), listEntries(start, counters).iterator()))
        var processed = 0
        var result: FileNode? = null

        while (stack.isNotEmpty()) {
            if ((++processed and 0xFF) == 0) currentCoroutineContext().ensureActive()
            val top = stack.last()
            if (top.entries.hasNext()) {
                val entry = top.entries.next()
                val attrs = readAttributes(entry) ?: continue
                if (attrs.isSymbolicLink) continue
                if (attrs.isDirectory) {
                    if (isExcluded(entry.toString(), options)) continue
                    counters.currentPath = entry.toString()
                    stack.addLast(Frame(entry, attrs.lastModifiedTime().toMillis(), listEntries(entry, counters).iterator()))
                } else if (attrs.isRegularFile) {
                    if (isExcluded(entry.toString(), options)) continue
                    top.files += visitFile(entry, attrs, visitor, counters)
                }
            } else {
                stack.removeLast()
                val node = buildNode(
                    path = top.path.toString(),
                    name = top.path.fileName?.toString() ?: top.path.toString(),
                    lastModified = top.lastModified,
                    childDirs = top.childDirs,
                    files = top.files,
                    maxFiles = options.maxFilesPerDirectory,
                )
                visitor.onDirectory(node)
                val parent = stack.lastOrNull()
                if (parent == null) result = node else parent.childDirs += node
            }
        }
        return checkNotNull(result)
    }

    private fun visitFile(
        path: Path,
        attrs: BasicFileAttributes,
        visitor: ScanVisitor,
        counters: ScanCounters,
    ): FileNode {
        val size = attrs.size()
        val lastModified = attrs.lastModifiedTime().toMillis()
        val pathString = path.toString()
        val name = path.fileName.toString()
        counters.files.incrementAndGet()
        counters.bytes.addAndGet(size)
        visitor.onFile(
            ScannedFile(
                path = pathString,
                name = name,
                parentPath = path.parent?.toString().orEmpty(),
                size = size,
                lastModified = lastModified,
            )
        )
        return FileNode(pathString, name, isDirectory = false, size = size, fileCount = 1, lastModified = lastModified)
    }

    private fun listEntries(dir: Path, counters: ScanCounters): List<Path> {
        counters.directories.incrementAndGet()
        return try {
            Files.newDirectoryStream(dir).use { stream -> stream.toList() }
        } catch (e: IOException) {
            counters.inaccessibleDirectories.incrementAndGet()
            emptyList()
        } catch (e: SecurityException) {
            counters.inaccessibleDirectories.incrementAndGet()
            emptyList()
        }
    }

    private fun readAttributes(path: Path): BasicFileAttributes? = try {
        Files.readAttributes(path, BasicFileAttributes::class.java, LinkOption.NOFOLLOW_LINKS)
    } catch (e: IOException) {
        null
    } catch (e: SecurityException) {
        null
    }

    companion object {
        fun isExcluded(path: String, options: ScanOptions): Boolean =
            options.excludedPaths.any { path == it || path.startsWith("$it/") }

        internal fun buildNode(
            path: String,
            name: String,
            lastModified: Long,
            childDirs: List<FileNode>,
            files: List<FileNode>,
            maxFiles: Int,
        ): FileNode {
            val sortedFiles = files.sortedByDescending { it.size }
            val kept = sortedFiles.take(maxFiles)
            val hidden = sortedFiles.drop(maxFiles)
            val children = (childDirs + kept).sortedByDescending { it.size }
            return FileNode(
                path = path,
                name = name,
                isDirectory = true,
                size = childDirs.sumOf { it.size } + files.sumOf { it.size },
                fileCount = childDirs.sumOf { it.fileCount } + files.size,
                lastModified = lastModified,
                children = children,
                hiddenFilesCount = hidden.size,
                hiddenFilesSize = hidden.sumOf { it.size },
            )
        }
    }
}
