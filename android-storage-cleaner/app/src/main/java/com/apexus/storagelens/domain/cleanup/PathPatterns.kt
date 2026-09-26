package com.apexus.storagelens.domain.cleanup

/** Motifs de noms/chemins partagés par les règles et la classification. */
object PathPatterns {
    private val LOG_EXTENSIONS = setOf("log", "logcat", "xlog")
    private val LOG_ROTATED = Regex(""".*\.log\.\d+(\.gz)?$""", RegexOption.IGNORE_CASE)
    private val LOG_DIRECTORIES = setOf("log", "logs", "logfiles", "applogs")

    private val TRACE_EXTENSIONS = setOf("trace", "trc", "hprof", "dmp", "dump", "mdmp", "stacktrace")
    private val TRACE_DIRECTORIES = setOf("traces", "crash", "crashes", "crashlogs", "crashreports", "tombstones", "anr", "bugreports")
    private val BUGREPORT = Regex("""^(bugreport|dumpstate)[-_].*""", RegexOption.IGNORE_CASE)

    private val TEMP_EXTENSIONS = setOf("tmp", "temp", "partial", "crdownload", "download", "part", "!ut")
    private val BACKUP_EXTENSIONS = setOf("bak", "old")

    val CACHE_DIRECTORY_NAMES = setOf("cache", ".cache", "caches", "tmp", ".tmp", "temp", ".temp", "code_cache")
    val TRASH_DIRECTORY_NAMES = setOf(".trash", ".trashes", ".recycle", ".recyclebin", "\$recycle.bin", ".local/share/trash")
    private val TRASH_DIRECTORY_PREFIX = ".trash-"

    fun extension(name: String): String {
        val dot = name.lastIndexOf('.')
        return if (dot < 0 || dot == name.length - 1) "" else name.substring(dot + 1).lowercase()
    }

    private fun segments(path: String): List<String> = path.split('/').filter { it.isNotEmpty() }

    /** Chemin relatif à la racine du volume (les segments au-dessus de la racine sont ignorés). */
    fun relativeTo(path: String, root: String): String {
        val r = root.trimEnd('/')
        return if (path == r) "" else if (path.startsWith("$r/")) path.substring(r.length + 1) else path
    }

    fun isLogFile(name: String, parentPath: String): Boolean {
        val ext = extension(name)
        if (ext in LOG_EXTENSIONS || LOG_ROTATED.matches(name)) return true
        val parent = parentPath.substringAfterLast('/').lowercase()
        return parent in LOG_DIRECTORIES && (ext == "txt" || ext == "gz" || ext == "")
    }

    fun isTraceFile(name: String, parentPath: String): Boolean {
        if (extension(name) in TRACE_EXTENSIONS) return true
        if (BUGREPORT.matches(name)) return true
        return segments(parentPath).any { it.lowercase() in TRACE_DIRECTORIES }
    }

    fun isTempFile(name: String): Boolean {
        val ext = extension(name)
        return ext in TEMP_EXTENSIONS || name.startsWith("~") || name.endsWith("~") ||
            name.startsWith(".pending-") || ext in BACKUP_EXTENSIONS
    }

    /** Les sauvegardes .bak/.old sont détectées mais jamais recommandées d'office. */
    fun isBackupFile(name: String): Boolean = extension(name) in BACKUP_EXTENSIONS

    fun isCacheDirectoryName(name: String): Boolean = name.lowercase() in CACHE_DIRECTORY_NAMES

    fun isInsideCacheDirectory(path: String): Boolean =
        segments(path).any { it.lowercase() in CACHE_DIRECTORY_NAMES || it.equals(".thumbnails", ignoreCase = true) }

    fun isTrashDirectoryName(name: String): Boolean {
        val lower = name.lowercase()
        return lower in TRASH_DIRECTORY_NAMES || lower.startsWith(TRASH_DIRECTORY_PREFIX)
    }

    /** Fichiers placés dans la corbeille système par MediaStore (Android 11+). */
    fun isMediaStoreTrashed(name: String): Boolean = name.startsWith(".trashed-")

    private val PACKAGE_NAME = Regex("""^[a-zA-Z][a-zA-Z0-9_]*(\.[a-zA-Z_][a-zA-Z0-9_]*)+$""")
    private val PACKAGE_PREFIXES = setOf(
        "com", "org", "net", "io", "de", "fr", "me", "app", "tv", "uk", "jp", "cn", "ru", "co", "in", "info", "dev", "ai", "us", "eu",
    )

    fun looksLikePackageName(name: String): Boolean =
        PACKAGE_NAME.matches(name) && name.substringBefore('.').lowercase() in PACKAGE_PREFIXES
}
