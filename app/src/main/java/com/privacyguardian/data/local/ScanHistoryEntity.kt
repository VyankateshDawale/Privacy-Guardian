package com.privacyguardian.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val detectionType: String, // e.g. "API_KEY, EMAIL"
    val riskLevel: String, // CRITICAL, HIGH, etc.
    val riskScore: Int,
    val action: String, // PROTECTED, IGNORED, etc.
    val protectedImageUri: String?, // nullable
    val itemCount: Int
)
