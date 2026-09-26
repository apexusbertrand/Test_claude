package com.apexus.storagelens.domain.scan

import com.apexus.storagelens.domain.model.CategoryUsage
import com.apexus.storagelens.domain.model.FileEntry
import com.apexus.storagelens.domain.model.FileTypes
import com.apexus.storagelens.domain.model.StorageCategory
import com.apexus.storagelens.domain.cleanup.PathPatterns
import java.util.PriorityQueue
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/** Conserve les N plus gros fichiers rencontrés (tas binaire borné). */
class TopFilesCollector(private val limit: Int = 100) : ScanVisitor {
    private val heap = PriorityQueue<FileEntry>(compareBy { it.size })

    override fun onFile(file: ScannedFile) {
        synchronized(heap) {
            if (heap.size < limit) {
                heap += file.toEntry()
            } else if (file.size > heap.peek().size) {
                heap.poll()
                heap += file.toEntry()
            }
        }
    }

    fun result(): List<FileEntry> = synchronized(heap) { heap.sortedByDescending { it.size } }

    private fun ScannedFile.toEntry() = FileEntry(path, name, size, lastModified, FileTypes.of(name))
}

/** Répartit chaque fichier dans une unique catégorie du tableau de bord. */
class CategoryCollector(private val storageRoot: String) : ScanVisitor {
    private val totals = ConcurrentHashMap<StorageCategory, AtomicLong>()

    override fun onFile(file: ScannedFile) {
        totals.getOrPut(classify(file, storageRoot)) { AtomicLong() }.addAndGet(file.size)
    }

    fun result(): List<CategoryUsage> =
        totals.map { (category, bytes) -> CategoryUsage(category, bytes.get()) }
            .filter { it.bytes > 0 }
            .sortedByDescending { it.bytes }

    companion object {
        fun classify(file: ScannedFile, storageRoot: String): StorageCategory {
            val parent = PathPatterns.relativeTo(file.parentPath, storageRoot)
            return when {
                PathPatterns.isLogFile(file.name, parent) || PathPatterns.isTraceFile(file.name, parent) ->
                    StorageCategory.LOGS_TRACES
                PathPatterns.isTempFile(file.name) || PathPatterns.isInsideCacheDirectory(parent) ->
                    StorageCategory.CACHE_TEMP
                else -> StorageCategory.fromType(FileTypes.of(file.name))
            }
        }
    }
}

/** Mémorise les fichiers candidats à la détection de doublons, groupés par taille. */
class SizeIndexCollector(private val minSize: Long = 512L * 1024) : ScanVisitor {
    private val bySize = ConcurrentHashMap<Long, MutableList<ScannedFile>>()

    override fun onFile(file: ScannedFile) {
        if (file.size < minSize) return
        val list = bySize.getOrPut(file.size) { java.util.Collections.synchronizedList(ArrayList()) }
        list += file
    }

    /** Groupes d'au moins deux fichiers de même taille. */
    fun sameSizeGroups(): List<List<ScannedFile>> =
        bySize.values.map { synchronized(it) { it.toList() } }.filter { it.size >= 2 }
}
