package com.example.healthstash.data.local

import androidx.room.*
import com.example.healthstash.data.model.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<Medication>>

    @Query("SELECT * FROM medications WHERE id = :id")
    fun getMedicationById(id: Int): Flow<Medication?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long // <--- 修改返回類型

    @Update
    suspend fun updateMedication(medication: Medication)

    @Delete
    suspend fun deleteMedication(medication: Medication)

    @Query("SELECT * FROM medications WHERE remainingQuantity <= :threshold")
    fun getLowStockMedications(threshold: Int): Flow<List<Medication>> // 用於剩餘一天通知

    // *** 新增方法：檢查藥品名稱是否存在 ***
    @Query("SELECT EXISTS(SELECT 1 FROM medications WHERE name = :name LIMIT 1)")
    suspend fun medicationExists(name: String): Boolean

    // ... (其他方法)

    @Query("SELECT EXISTS(SELECT 1 FROM medications WHERE name = :name AND id != :excludeId LIMIT 1)")
    suspend fun medicationExistsByNameAndNotId(name: String, excludeId: Int): Boolean
}