package com.apexus.storagelens.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.apexus.storagelens.domain.model.TrashItem

@Entity(tableName = "trash_items", indices = [Index("batchId"), Index("deletedAt")])
data class TrashItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalPath: String,
    val trashPath: String,
    val size: Long,
    val isDirectory: Boolean,
    val deletedAt: Long,
    val batchId: String,
) {
    fun toDomain() = TrashItem(id, originalPath, trashPath, size, isDirectory, deletedAt, batchId)
}

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val finishedAt: Long,
    val volumeLabel: String,
    val totalBytes: Long,
    val usedBytes: Long,
    val scannedBytes: Long,
    val fileCount: Int,
    val recoverableBytes: Long,
    val durationMs: Long,
)

@Entity(tableName = "cleanup_history")
data class CleanupHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val performedAt: Long,
    val itemCount: Int,
    val freedBytes: Long,
    val movedToTrash: Boolean,
)
