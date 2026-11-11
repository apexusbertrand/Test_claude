package com.apexus.storagelens.domain.downloads

/** Fichier du dossier Téléchargements. */
data class DownloadFile(
    val path: String,
    val name: String,
    val size: Long,
    val lastModified: Long,
)

/** Sélection des fichiers du dossier Téléchargements plus anciens qu'un nombre de jours. */
object DownloadsCleanup {
    const val DEFAULT_DAYS = 30
    const val MIN_DAYS = 1
    const val MAX_DAYS = 3650
    private const val DAY_MS = 24L * 3600 * 1000

    fun clampDays(days: Int): Int = days.coerceIn(MIN_DAYS, MAX_DAYS)

    /** Date limite : un fichier modifié avant cet instant est concerné. */
    fun cutoff(nowMillis: Long, days: Int): Long = nowMillis - clampDays(days) * DAY_MS

    /** Fichiers plus anciens que [days] jours, du plus gros au plus petit. */
    fun olderThan(files: List<DownloadFile>, nowMillis: Long, days: Int): List<DownloadFile> {
        val limit = cutoff(nowMillis, days)
        return files.filter { it.lastModified in 1..<limit }.sortedByDescending { it.size }
    }
}
