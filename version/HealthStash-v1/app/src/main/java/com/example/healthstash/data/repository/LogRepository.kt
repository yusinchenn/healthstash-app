package com.example.healthstash.data.repository

import com.example.healthstash.data.local.MedicationLogDao
import com.example.healthstash.data.model.MedicationLog
import kotlinx.coroutines.flow.Flow

class LogRepository(private val medicationLogDao: MedicationLogDao) {
    val allLogs: Flow<List<MedicationLog>> = medicationLogDao.getAllLogs()

    suspend fun insert(log: MedicationLog) {
        medicationLogDao.insertLog(log)
    }

    suspend fun clearAllLogs() {
        medicationLogDao.clearAllLogs()
    }

    // ... (其他方法)

}