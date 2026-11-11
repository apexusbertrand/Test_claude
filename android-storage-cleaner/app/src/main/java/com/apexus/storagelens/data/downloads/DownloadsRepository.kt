package com.apexus.storagelens.data.downloads

import android.os.Environment
import com.apexus.storagelens.domain.downloads.DownloadFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import javax.inject.Inject
import javax.inject.Singleton

/** Accès au dossier Téléchargements (« Download ») du stockage principal. */
@Singleton
class DownloadsRepository @Inject constructor() {

    @Suppress("DEPRECATION") // Lecture directe autorisée par l'accès à tous les fichiers.
    fun directory(): File = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

    /** Tous les fichiers du dossier, sous-dossiers compris, sans suivre les liens symboliques. */
    suspend fun listFiles(): List<DownloadFile> = withContext(Dispatchers.IO) {
        val root = directory()
        if (!root.isDirectory) return@withContext emptyList()
        root.walkTopDown()
            .onEnter { !Files.isSymbolicLink(it.toPath()) }
            .filter { it.isFile && !Files.isSymbolicLink(it.toPath()) }
            .map { DownloadFile(it.path, it.name, it.length(), it.lastModified()) }
            .toList()
    }
}
