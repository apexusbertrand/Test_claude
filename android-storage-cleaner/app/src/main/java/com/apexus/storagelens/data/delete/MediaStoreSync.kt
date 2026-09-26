package com.apexus.storagelens.data.delete

import android.content.Context
import android.media.MediaScannerConnection
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Garde MediaStore (galerie, lecteurs) cohérent après un déplacement ou une suppression. */
@Singleton
class MediaStoreSync @Inject constructor(
    @ApplicationContext private val context: Context,
) {
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
}
