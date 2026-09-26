package com.apexus.storagelens.domain.apps

import com.apexus.storagelens.domain.model.AppStorageInfo

enum class AppBatchKind { CLEAR_CACHE, UNINSTALL }

/**
 * Action groupée sur plusieurs applications. Android n'autorise ni à vider le cache ni à
 * désinstaller une autre application sans passer par ses propres écrans : on les ouvre donc
 * l'un après l'autre, l'utilisateur confirmant chaque étape.
 */
data class AppBatch(
    val kind: AppBatchKind,
    val packages: List<String>,
    val labels: Map<String, String>,
    val index: Int = 0,
    /** Vrai une fois l'écran système de l'étape courante ouvert (évite de le rouvrir). */
    val launched: Boolean = false,
    /** Applications ignorées (système, l'application elle-même, sans cache…). */
    val skipped: Int = 0,
) {
    val current: String? get() = packages.getOrNull(index)
    val currentLabel: String? get() = current?.let { labels[it] ?: it }
    val isFinished: Boolean get() = index >= packages.size
    val total: Int get() = packages.size

    fun markLaunched() = copy(launched = true)
    fun next() = copy(index = index + 1, launched = false)

    companion object {
        fun create(kind: AppBatchKind, apps: List<AppStorageInfo>, ownPackage: String): AppBatch {
            val eligible = apps.filter { app ->
                when (kind) {
                    AppBatchKind.CLEAR_CACHE -> app.cacheBytes > 0
                    AppBatchKind.UNINSTALL -> !app.isSystemApp && app.packageName != ownPackage
                }
            }
            // Les plus gros gains d'abord.
            val ordered = when (kind) {
                AppBatchKind.CLEAR_CACHE -> eligible.sortedByDescending { it.cacheBytes }
                AppBatchKind.UNINSTALL -> eligible.sortedByDescending { it.totalBytes }
            }
            return AppBatch(
                kind = kind,
                packages = ordered.map { it.packageName },
                labels = ordered.associate { it.packageName to it.label },
                skipped = apps.size - eligible.size,
            )
        }
    }
}
