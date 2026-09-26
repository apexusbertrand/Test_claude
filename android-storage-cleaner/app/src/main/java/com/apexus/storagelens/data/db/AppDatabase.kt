package com.apexus.storagelens.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TrashItemEntity::class, ScanHistoryEntity::class, CleanupHistoryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trashDao(): TrashDao
    abstract fun historyDao(): HistoryDao
}
