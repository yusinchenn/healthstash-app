package com.example.healthstash.data.local

import androidx.room.*
import com.example.healthstash.data.model.MedicationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationLogDao {

    // 取得所有用藥紀錄，依時間遞減排序（最新的在最前面）
    @Query("SELECT * FROM medication_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<MedicationLog>>

    // 插入一筆用藥紀錄，如果主鍵（若設定）衝突，則覆蓋舊資料
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLog)

    // 刪除所有用藥紀錄，可用於清除記錄功能
    @Query("DELETE FROM medication_logs")
    suspend fun clearAllLogs()
}
