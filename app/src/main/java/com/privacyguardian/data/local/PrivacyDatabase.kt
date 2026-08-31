package com.privacyguardian.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScanHistoryEntity::class], version = 1, exportSchema = false)
abstract class PrivacyDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
}
