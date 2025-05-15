package com.example.healthstash.data.local

import androidx.room.*
import com.example.healthstash.data.model.MedicationLog
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationLogDao {
    @Query("SELECT * FROM medication_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<MedicationLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: MedicationLog)

    @Query("DELETE FROM medication_logs")
    suspend fun clearAllLogs()

    @Query("SELECT EXISTS(SELECT 1 FROM medications WHERE name = :name AND id != :excludeId LIMIT 1)")
    suspend fun medicationExistsByNameAndNotId(name: String, excludeId: Int): Boolean
}