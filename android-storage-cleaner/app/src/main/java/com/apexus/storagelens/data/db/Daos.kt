package com.apexus.storagelens.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TrashDao {
    @Query("SELECT * FROM trash_items ORDER BY deletedAt DESC")
    fun observeAll(): Flow<List<TrashItemEntity>>

    @Query("SELECT * FROM trash_items WHERE batchId = :batchId")
    suspend fun byBatch(batchId: String): List<TrashItemEntity>

    @Query("SELECT * FROM trash_items WHERE id = :id")
    suspend fun byId(id: Long): TrashItemEntity?

    @Query("SELECT * FROM trash_items WHERE deletedAt < :before")
    suspend fun olderThan(before: Long): List<TrashItemEntity>

    @Query("SELECT * FROM trash_items")
    suspend fun all(): List<TrashItemEntity>

    @Insert
    suspend fun insert(item: TrashItemEntity): Long

    @Delete
    suspend fun delete(item: TrashItemEntity)
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY finishedAt DESC LIMIT 100")
    fun observeScans(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY finishedAt DESC LIMIT 1")
    fun observeLastScan(): Flow<ScanHistoryEntity?>

    @Query("SELECT * FROM cleanup_history ORDER BY performedAt DESC LIMIT 100")
    fun observeCleanups(): Flow<List<CleanupHistoryEntity>>

    @Query("SELECT COALESCE(SUM(freedBytes), 0) FROM cleanup_history")
    fun observeTotalFreed(): Flow<Long>

    @Insert
    suspend fun insertScan(entry: ScanHistoryEntity)

    @Insert
    suspend fun insertCleanup(entry: CleanupHistoryEntity)
}
