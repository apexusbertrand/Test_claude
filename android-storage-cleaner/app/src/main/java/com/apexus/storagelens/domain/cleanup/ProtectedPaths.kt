package com.apexus.storagelens.domain.cleanup

/**
 * Garde-fou central : décide ce qui peut être proposé et supprimé.
 *
 * - [isProtected] : jamais supprimable (racines, dossiers standards, `Android/…`, dossier de l'app).
 * - [canAutoPropose] : peut être proposé automatiquement par une règle.
 * - [canDelete] : peut être supprimé après une sélection manuelle explicite.
 */
class ProtectedPaths(
    storageRoots: List<String>,
    private val ownPackage: String,
    userExclusions: Set<String> = emptySet(),
    /** Paquets installés ; null si inconnus (tous les dossiers de paquets restent protégés). */
    private val installedPackages: Set<String>? = null,
) {
    private val roots = storageRoots.map { it.trimEnd('/') }.filter { it.isNotEmpty() }
    private val exclusions = userExclusions.map { it.trimEnd('/') }.filter { it.isNotEmpty() }

    private fun rootOf(path: String): String? =
        roots.filter { path == it || path.startsWith("$it/") }.maxByOrNull { it.length }

    private fun relative(path: String): String? {
        val root = rootOf(path) ?: return null
        return path.removePrefix(root).trimStart('/')
    }

    private fun isUnder(path: String, ancestor: String) = path == ancestor || path.startsWith("$ancestor/")

    fun isUserExcluded(path: String): Boolean = exclusions.any { isUnder(path, it) }

    fun isProtected(path: String): Boolean {
        val normalized = path.trimEnd('/')
        val rel = relative(normalized) ?: return true // hors des volumes de stockage
        if (rel.isEmpty()) return true
        val relLower = rel.lowercase()
        if (relLower in STANDARD_DIRECTORIES) return true
        if (relLower in PROTECTED_EXACT) return true
        if (relLower == "android" || relLower.startsWith("android/")) {
            val segments = rel.split('/')
            // Android, Android/data, Android/obb, Android/media : toujours protégés.
            if (segments.size <= 2) return true
            if (segments.size == 3) {
                // Dossier d'un paquet : supprimable seulement s'il s'agit d'un résidu d'app désinstallée.
                val pkg = segments[2]
                return pkg == ownPackage || installedPackages == null || pkg in installedPackages
            }
            if (segments[2] == ownPackage) {
                // Seul le cache de l'application elle-même peut être vidé.
                return !(segments.size >= 4 && segments[3] == "cache")
            }
        }
        return false
    }

    fun canAutoPropose(path: String): Boolean {
        if (isProtected(path) || isUserExcluded(path)) return false
        val rel = relative(path)?.lowercase() ?: return false
        return AUTO_FORBIDDEN_TREES.none { rel == it || rel.startsWith("$it/") }
    }

    fun canDelete(path: String): Boolean = !isProtected(path) && !isUserExcluded(path)

    companion object {
        val STANDARD_DIRECTORIES = setOf(
            "dcim", "download", "downloads", "pictures", "music", "movies", "documents", "alarms",
            "notifications", "podcasts", "ringtones", "recordings", "audiobooks", "android",
        )
        private val PROTECTED_EXACT = setOf(
            "dcim/camera", "pictures/screenshots", "dcim/screenshots",
        )
        /** Arborescences où aucune règle automatique ne doit piocher (sélection manuelle uniquement). */
        private val AUTO_FORBIDDEN_TREES = setOf("dcim/camera")
    }
}
