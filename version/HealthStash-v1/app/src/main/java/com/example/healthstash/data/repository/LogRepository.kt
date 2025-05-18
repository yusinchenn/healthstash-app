package com.example.healthstash.data.repository

import com.example.healthstash.data.local.MedicationLogDao
import com.example.healthstash.data.model.MedicationLog
import kotlinx.coroutines.flow.Flow

// LogRepository 作為資料來源的中介層，封裝對 MedicationLogDao 的操作
class LogRepository(private val medicationLogDao: MedicationLogDao) {

    // 取得所有用藥紀錄（以 Flow 回傳，可即時反映資料變更到 UI）
    val allLogs: Flow<List<MedicationLog>> = medicationLogDao.getAllLogs()

    // 插入一筆新的用藥紀錄
    suspend fun insert(log: MedicationLog) {
        medicationLogDao.insertLog(log)
    }

    // 清除所有用藥紀錄（通常用於重置或測試）
    suspend fun clearAllLogs() {
        medicationLogDao.clearAllLogs()
    }

    // ...（可以擴充其他用藥紀錄相關邏輯，如依時間篩選、搜尋等）

}
