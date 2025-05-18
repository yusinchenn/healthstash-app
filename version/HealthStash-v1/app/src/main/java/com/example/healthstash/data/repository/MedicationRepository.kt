package com.example.healthstash.data.repository

import com.example.healthstash.data.local.MedicationDao
import com.example.healthstash.data.model.Medication
import kotlinx.coroutines.flow.Flow

// MedicationRepository 負責封裝對 MedicationDao 的所有操作邏輯
class MedicationRepository(private val medicationDao: MedicationDao) {

    // 取得所有藥品清單，資料類型為 Flow，讓 UI 可即時更新
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()

    // 插入一筆藥品資料，回傳自動產生的主鍵（可用於後續操作）
    suspend fun insert(medication: Medication): Long {
        return medicationDao.insertMedication(medication)
    }

    // 更新藥品資料（根據 ID）
    suspend fun update(medication: Medication) {
        medicationDao.updateMedication(medication)
    }

    // 刪除指定藥品資料
    suspend fun delete(medication: Medication) {
        medicationDao.deleteMedication(medication)
    }

    // 依 ID 查詢特定藥品，回傳 Flow（支援即時 UI 更新）
    fun getMedicationById(id: Int): Flow<Medication?> {
        return medicationDao.getMedicationById(id)
    }

    // 可以啟用這段，用於查詢低庫存藥品，例如提醒使用者要補貨
    /*
    fun getLowStockMedications(threshold: Int): Flow<List<Medication>> {
        return medicationDao.getLowStockMedications(threshold)
    }
    */

    // 檢查藥品名稱是否已存在，避免重複新增
    suspend fun medicationExists(name: String): Boolean {
        return medicationDao.medicationExists(name)
    }

    // 編輯藥品時，檢查是否有其他藥品名稱與新名稱重複（排除自己）
    suspend fun medicationExistsByNameAndNotId(name: String, excludeId: Int): Boolean {
        return medicationDao.medicationExistsByNameAndNotId(name, excludeId)
    }
}
