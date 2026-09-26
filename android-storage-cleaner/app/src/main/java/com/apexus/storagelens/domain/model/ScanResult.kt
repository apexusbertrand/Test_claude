package com.apexus.storagelens.domain.model

data class ScanResult(
    val volume: StorageVolumeInfo,
    val root: FileNode,
    val topFiles: List<FileEntry>,
    val categories: List<CategoryUsage>,
    val cleanupGroups: List<CleanupGroup>,
    val inaccessibleDirectories: Int,
    val durationMs: Long,
    val finishedAt: Long,
) {
    val recoverableBytes: Long get() = cleanupGroups.sumOf { it.totalBytes }
    val recommendedBytes: Long
        get() = cleanupGroups.sumOf { g -> g.candidates.filter { it.recommended }.sumOf { it.size } }
}

sealed interface ScanState {
    data object Idle : ScanState
    data class Running(
        val currentPath: String,
        val filesScanned: Long,
        val bytesScanned: Long,
        /** Fraction estimée (0..1) à partir de l'espace utilisé connu, ou null si inconnue. */
        val fraction: Float?,
        val phase: ScanPhase,
    ) : ScanState
    data class Done(val result: ScanResult) : ScanState
    data class Failed(val message: String) : ScanState
}

enum class ScanPhase { LISTING, ANALYZING, DUPLICATES }
