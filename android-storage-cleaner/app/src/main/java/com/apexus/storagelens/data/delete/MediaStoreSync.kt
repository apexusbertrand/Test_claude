package com.apexus.storagelens.data.delete

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.apexus.storagelens.domain.deletion.MediaIndexEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Photo ou vidéo trouvée dans l'index MediaStore. */
data class IndexedMedia(val uri: Uri, val entry: MediaIndexEntry)

/** Garde MediaStore (galerie, lecteurs) cohérent et fournit la confirmation système des médias. */
@Singleton
class MediaStoreSync @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** La fenêtre de confirmation système des médias n'existe qu'à partir d'Android 11. */
    val systemConfirmationSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun rescan(paths: Collection<String>) {
        if (paths.isEmpty()) return
        paths.chunked(200).forEach { chunk ->
            MediaScannerConnection.scanFile(context, chunk.toTypedArray(), null, null)
        }
    }

    /** Retire de l'index les entrées situées sous un dossier supprimé. */
    fun forgetDirectory(directoryPath: String) {
        try {
            context.contentResolver.delete(
                MediaStore.Files.getContentUri("external"),
                "${MediaStore.MediaColumns.DATA} LIKE ?",
                arrayOf("$directoryPath/%"),
            )
        } catch (e: SecurityException) {
            rescan(listOf(directoryPath))
        } catch (e: IllegalArgumentException) {
            rescan(listOf(directoryPath))
        }
    }

    /**
     * Retrouve les photos et vidéos indexées correspondant aux chemins donnés
     * (y compris celles déjà dans la corbeille Android).
     */
    fun findPhotosAndVideos(paths: Collection<String>): Map<String, IndexedMedia> {
        if (!systemConfirmationSupported || paths.isEmpty()) return emptyMap()
        val result = HashMap<String, IndexedMedia>()
        paths.distinct().chunked(QUERY_CHUNK).forEach { chunk -> result += query(chunk) }
        return result
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun query(paths: List<String>): Map<String, IndexedMedia> {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.IS_TRASHED,
        )
        val args = Bundle().apply {
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                "${MediaStore.Files.FileColumns.DATA} IN (${paths.joinToString(",") { "?" }}) AND " +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE} IN (" +
                    "${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE},${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO})",
            )
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, paths.toTypedArray())
            putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_INCLUDE)
            putInt(MediaStore.QUERY_ARG_MATCH_PENDING, MediaStore.MATCH_INCLUDE)
        }
        val found = HashMap<String, IndexedMedia>()
        try {
            context.contentResolver.query(MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL), projection, args, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val typeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                val trashedCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.IS_TRASHED)
                while (cursor.moveToNext()) {
                    val path = cursor.getString(dataCol) ?: continue
                    val collection = if (cursor.getInt(typeCol) == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO) {
                        MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    }
                    found[path] = IndexedMedia(
                        uri = ContentUris.withAppendedId(collection, cursor.getLong(idCol)),
                        entry = MediaIndexEntry(isTrashed = cursor.getInt(trashedCol) == 1),
                    )
                }
            }
        } catch (e: SecurityException) {
            return emptyMap()
        } catch (e: IllegalArgumentException) {
            return emptyMap()
        }
        return found
    }

    /** Fenêtre système « Déplacer vers la corbeille ? » ou « Supprimer définitivement ? ». */
    @RequiresApi(Build.VERSION_CODES.R)
    fun confirmationRequest(uris: List<Uri>, toTrash: Boolean): IntentSender {
        val pendingIntent = if (toTrash) {
            MediaStore.createTrashRequest(context.contentResolver, uris, true)
        } else {
            MediaStore.createDeleteRequest(context.contentResolver, uris)
        }
        return pendingIntent.intentSender
    }

    companion object {
        private const val QUERY_CHUNK = 500
    }
}
