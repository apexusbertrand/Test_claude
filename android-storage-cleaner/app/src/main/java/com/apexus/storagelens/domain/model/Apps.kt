package com.apexus.storagelens.domain.model

data class AppStorageInfo(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val apkBytes: Long,
    val dataBytes: Long,
    val cacheBytes: Long,
    val lastUsedMillis: Long?,
    val isSystemApp: Boolean,
) {
    /** `dataBytes` inclut déjà le cache (cf. StorageStats.getDataBytes). */
    val totalBytes: Long get() = apkBytes + dataBytes
}

data class TrashItem(
    val id: Long,
    val originalPath: String,
    val trashPath: String,
    val size: Long,
    val isDirectory: Boolean,
    val deletedAt: Long,
    val batchId: String,
)

data class DeletionFailure(val path: String, val reason: String)

data class DeletionReport(
    val requestedCount: Int,
    val deletedCount: Int,
    val measuredFreedBytes: Long,
    val expectedFreedBytes: Long,
    val movedToTrash: Boolean,
    val batchId: String,
    val failures: List<DeletionFailure>,
)
