package com.example.healthstash.data.local

import androidx.room.*
import com.example.healthstash.data.model.Medication
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationDao {

    // 取得所有藥品清單，依藥名遞增排序。回傳 Flow 讓 UI 能自動監聽更新。
    @Query("SELECT * FROM medications ORDER BY name ASC")
    fun getAllMedications(): Flow<List<Medication>>

    // 依照藥品 ID 查詢對應藥品資料，回傳 Flow，支援 UI 實時更新。
    @Query("SELECT * FROM medications WHERE id = :id")
    fun getMedicationById(id: Int): Flow<Medication?>

    // 插入新藥品紀錄，若主鍵衝突（如相同 ID）則覆蓋原資料。
    // 回傳新增資料的主鍵（rowId），可用於追蹤或後續操作。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMedication(medication: Medication): Long

    // 更新現有藥品資料（以 primary key 為依據）。
    @Update
    suspend fun updateMedication(medication: Medication)

    // 刪除特定藥品資料。
    @Delete
    suspend fun deleteMedication(medication: Medication)

    // 查詢剩餘數量低於指定門檻的藥品清單，可用於通知提醒。
    @Query("SELECT * FROM medications WHERE remainingQuantity <= :threshold")
    fun getLowStockMedications(threshold: Int): Flow<List<Medication>>

    // 檢查資料庫中是否已存在指定藥名的藥品（可用於新增時避免重複）。
    @Query("SELECT EXISTS(SELECT 1 FROM medications WHERE name = :name LIMIT 1)")
    suspend fun medicationExists(name: String): Boolean

    // 用於編輯藥品時檢查是否有其他藥品與新名稱重複（排除目前這筆的 ID）。
    @Query("SELECT EXISTS(SELECT 1 FROM medications WHERE name = :name AND id != :excludeId LIMIT 1)")
    suspend fun medicationExistsByNameAndNotId(name: String, excludeId: Int): Boolean
}
