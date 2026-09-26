package com.apexus.storagelens.domain.model

enum class FileType { IMAGE, VIDEO, AUDIO, DOCUMENT, ARCHIVE, APK, OTHER }

object FileTypes {
    private val byExtension: Map<String, FileType> = buildMap {
        listOf("jpg", "jpeg", "png", "gif", "webp", "heic", "heif", "bmp", "dng", "raw", "svg", "avif")
            .forEach { put(it, FileType.IMAGE) }
        listOf("mp4", "mkv", "mov", "avi", "3gp", "webm", "m4v", "wmv", "flv", "ts")
            .forEach { put(it, FileType.VIDEO) }
        listOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr", "wma", "mid", "midi")
            .forEach { put(it, FileType.AUDIO) }
        listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "odt", "ods", "odp", "txt", "rtf", "csv", "epub", "md", "json", "xml", "html", "htm")
            .forEach { put(it, FileType.DOCUMENT) }
        listOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "zst")
            .forEach { put(it, FileType.ARCHIVE) }
        listOf("apk", "xapk", "apks", "apkm")
            .forEach { put(it, FileType.APK) }
    }

    fun extensionOf(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot <= 0 || dot == name.length - 1) "" else name.substring(dot + 1).lowercase()
    }

    fun of(name: String): FileType = byExtension[extensionOf(name)] ?: FileType.OTHER
}
