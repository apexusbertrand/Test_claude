package com.apexus.storagelens.data.storage

import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import com.apexus.storagelens.domain.model.StorageVolumeInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** Volumes de stockage montés : mémoire interne, carte SD, clés USB OTG. */
@Singleton
class StorageVolumesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val storageManager = context.getSystemService(StorageManager::class.java)
    private val statsManager = context.getSystemService(StorageStatsManager::class.java)

    suspend fun volumes(): List<StorageVolumeInfo> = withContext(Dispatchers.IO) {
        volumeRoots().mapNotNull { root ->
            val volume = storageManager.getStorageVolume(root) ?: return@mapNotNull null
            if (Environment.getExternalStorageState(root) != Environment.MEDIA_MOUNTED &&
                Environment.getExternalStorageState(root) != Environment.MEDIA_MOUNTED_READ_ONLY
            ) return@mapNotNull null
            val (total, free) = capacity(volume, root)
            StorageVolumeInfo(
                id = volume.uuid ?: PRIMARY_ID,
                label = volume.getDescription(context),
                rootPath = root.path,
                totalBytes = total,
                freeBytes = free,
                isPrimary = volume.isPrimary,
                isRemovable = volume.isRemovable,
            )
        }.sortedByDescending { it.isPrimary }
    }

    suspend fun volume(id: String?): StorageVolumeInfo? {
        val all = volumes()
        return all.firstOrNull { it.id == id } ?: all.firstOrNull { it.isPrimary } ?: all.firstOrNull()
    }

    /** Espace libre instantané d'un volume (utilisé pour mesurer l'espace réellement libéré). */
    fun availableBytes(path: String): Long = try {
        StatFs(path).availableBytes
    } catch (e: IllegalArgumentException) {
        0L
    }

    fun storageRoots(): List<String> = volumeRoots().map { it.path }

    private fun volumeRoots(): List<File> {
        val roots = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            storageManager.storageVolumes.mapNotNull { it.directory }
        } else {
            // Android 8–10 : on déduit les racines des dossiers propres à l'application sur chaque volume.
            context.getExternalFilesDirs(null).filterNotNull().mapNotNull { rootFromAppDir(it) }
        }
        return roots.distinctBy { it.path }
    }

    private fun rootFromAppDir(appDir: File): File? {
        val marker = "/Android/data/"
        val path = appDir.path
        val index = path.indexOf(marker)
        return if (index > 0) File(path.substring(0, index)) else null
    }

    /**
     * Pour le volume principal, StorageStatsManager renvoie la capacité « commerciale »
     * (système inclus), plus fidèle que StatFs qui ne voit que la partition de données.
     */
    private fun capacity(volume: StorageVolume, root: File): Pair<Long, Long> {
        if (volume.isPrimary) {
            try {
                val total = statsManager.getTotalBytes(StorageManager.UUID_DEFAULT)
                val free = statsManager.getFreeBytes(StorageManager.UUID_DEFAULT)
                if (total > 0) return total to free
            } catch (e: IOException) {
                // Repli sur StatFs ci-dessous.
            }
        }
        val stat = StatFs(root.path)
        return stat.totalBytes to stat.availableBytes
    }

    companion object {
        const val PRIMARY_ID = "primary"
    }
}
