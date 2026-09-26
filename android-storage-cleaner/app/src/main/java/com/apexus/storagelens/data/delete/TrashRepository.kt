package com.apexus.storagelens.data.delete

import android.content.Context
import com.apexus.storagelens.data.db.TrashDao
import com.apexus.storagelens.data.db.TrashItemEntity
import com.apexus.storagelens.domain.model.TrashItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Corbeille interne de l'application.
 *
 * Les éléments sont déplacés (simple renommage, donc instantané) dans le dossier propre à
 * l'application situé sur le même volume : `Android/data/<paquet>/files/trash`.
 * Attention : ce dossier est effacé si l'application est désinstallée.
 */
@Singleton
class TrashRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: TrashDao,
    private val mediaStoreSync: MediaStoreSync,
) {
    val items: Flow<List<TrashItem>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    /** Dossiers de corbeille de tous les volumes (exclus de l'analyse). */
    fun trashDirectories(): List<String> =
        context.getExternalFilesDirs(TRASH_DIR).filterNotNull().map { it.path }

    private fun trashDirFor(path: String): File {
        val dirs = context.getExternalFilesDirs(TRASH_DIR).filterNotNull()
        val sameVolume = dirs.firstOrNull { dir ->
            val volumeRoot = dir.path.substringBefore("/Android/data/")
            path.startsWith("$volumeRoot/")
        }
        return sameVolume ?: dirs.firstOrNull() ?: File(context.filesDir, TRASH_DIR)
    }

    suspend fun moveToTrash(source: File, batchId: String): Result<TrashItem> = withContext(Dispatchers.IO) {
        runCatching {
            val size = sizeOf(source)
            val batchDir = File(trashDirFor(source.path), batchId).apply { mkdirs() }
            val target = File(batchDir, "${UUID.randomUUID()}_${source.name}")
            moveOrCopy(source, target)
            val entity = TrashItemEntity(
                originalPath = source.path,
                trashPath = target.path,
                size = size,
                isDirectory = target.isDirectory,
                deletedAt = System.currentTimeMillis(),
                batchId = batchId,
            )
            val id = dao.insert(entity)
            entity.copy(id = id).toDomain()
        }
    }

    suspend fun restore(itemId: Long): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val entity = dao.byId(itemId) ?: throw IOException("Élément introuvable")
            restoreEntity(entity)
        }
    }

    suspend fun restoreBatch(batchId: String): Int = withContext(Dispatchers.IO) {
        dao.byBatch(batchId).count { runCatching { restoreEntity(it) }.isSuccess }
    }

    private suspend fun restoreEntity(entity: TrashItemEntity): String {
        val source = File(entity.trashPath)
        if (!source.exists()) {
            dao.delete(entity)
            throw IOException("Le fichier n'existe plus dans la corbeille")
        }
        var target = File(entity.originalPath)
        if (target.exists()) {
            val base = target.nameWithoutExtension
            val ext = target.extension.let { if (it.isEmpty()) "" else ".$it" }
            var index = 1
            while (target.exists()) {
                target = File(target.parentFile, "$base ($index)$ext")
                index++
            }
        }
        target.parentFile?.mkdirs()
        moveOrCopy(source, target)
        dao.delete(entity)
        source.parentFile?.takeIf { it.list()?.isEmpty() == true }?.delete()
        mediaStoreSync.rescan(listOf(target.path))
        return target.path
    }

    suspend fun deletePermanently(itemId: Long): Boolean = withContext(Dispatchers.IO) {
        val entity = dao.byId(itemId) ?: return@withContext false
        deleteEntity(entity)
    }

    suspend fun empty(): Int = withContext(Dispatchers.IO) {
        dao.all().count { deleteEntity(it) }
    }

    suspend fun purgeOlderThan(days: Int): Int = withContext(Dispatchers.IO) {
        val before = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(days.toLong())
        dao.olderThan(before).count { deleteEntity(it) }
    }

    private suspend fun deleteEntity(entity: TrashItemEntity): Boolean {
        val file = File(entity.trashPath)
        val ok = !file.exists() || file.deleteRecursively()
        if (ok) {
            dao.delete(entity)
            file.parentFile?.takeIf { it.list()?.isEmpty() == true }?.delete()
        }
        return ok
    }

    private fun moveOrCopy(source: File, target: File) {
        if (source.renameTo(target)) return
        // Volumes différents ou renommage refusé : copie puis suppression.
        if (!source.copyRecursively(target, overwrite = false)) throw IOException("Copie impossible")
        if (!source.deleteRecursively()) {
            target.deleteRecursively()
            throw IOException("Suppression de l'original impossible")
        }
    }

    private fun sizeOf(file: File): Long =
        if (file.isDirectory) file.walkBottomUp().filter { it.isFile }.sumOf { it.length() } else file.length()

    companion object {
        const val TRASH_DIR = "trash"
    }
}
