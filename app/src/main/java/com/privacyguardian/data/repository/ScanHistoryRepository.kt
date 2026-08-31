package com.privacyguardian.data.repository

import com.privacyguardian.data.local.ScanHistoryDao
import com.privacyguardian.data.local.ScanHistoryEntity
import kotlinx.coroutines.flow.Flow

class ScanHistoryRepository(private val dao: ScanHistoryDao) {
    fun getAll(): Flow<List<ScanHistoryEntity>> = dao.getAll()
    suspend fun getRecent(limit: Int = 3): List<ScanHistoryEntity> = dao.getRecent(limit)
    suspend fun insert(entity: ScanHistoryEntity): Long = dao.insert(entity)
    suspend fun clearAll() = dao.clearAll()
    suspend fun getById(id: Long) = dao.getById(id)
}
