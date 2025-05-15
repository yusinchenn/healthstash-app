package com.example.healthstash.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthstash.data.local.AppDatabase
import com.example.healthstash.data.model.Medication
import com.example.healthstash.data.model.MedicationLog
import com.example.healthstash.data.repository.LogRepository
import com.example.healthstash.data.repository.MedicationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val medicationRepository: MedicationRepository
    private val logRepository: LogRepository

    // 使用 MutableStateFlow 來儲存藥品資料
    private val _medications = MutableStateFlow<List<Medication>>(emptyList())
    val medications: StateFlow<List<Medication>> = _medications

    init {
        val database = AppDatabase.getDatabase(application)
        medicationRepository = MedicationRepository(database.medicationDao())
        logRepository = LogRepository(database.medicationLogDao())

        // 初始化時載入藥品資料
        loadMedications()
    }

    fun markAsTaken(medication: Medication, dosageTaken: Int) {
        viewModelScope.launch {
            if (medication.remainingQuantity >= dosageTaken) {
                val updatedMedication = medication.copy(remainingQuantity = medication.remainingQuantity - dosageTaken)
                medicationRepository.update(updatedMedication)

                val log = MedicationLog(
                    medicationName = medication.name,
                    timestamp = System.currentTimeMillis(),
                    dosage = "$dosageTaken 份/顆" // 根據實際情況調整
                )
                logRepository.insert(log)

                loadMedications() // 更新藥品資料
            }
        }
    }

//    private fun checkLowStock(medication: Medication) {
//         假設每天平均服用次數 * 劑量 = 每日消耗量
//         這部分邏輯需要根據藥品設定的使用頻率來計算
//         例如：如果一天吃3次，每次1顆，則每日消耗3顆。
//         如果 remainingQuantity <= 每日消耗量，則觸發 "剩餘一天" 通知
//         這裡簡化處理，如果剩餘小於等於一個特定值 (例如2)，就認為是低庫存
//        if (medication.remainingQuantity <= 2 && medication.remainingQuantity > 0) { // 假設低於等於2份算低庫存
//             觸發低庫存通知 (具體實現在 NotificationHelper)
//             NotificationHelper(getApplication()).showLowStockNotification(medication)
//             實際應用中，這個檢查和通知觸發可能更複雜，例如在特定時間檢查所有藥品
//        }
//    }

    // 刪除藥品並刷新列表
    fun deleteMedication(medication: Medication) {
        viewModelScope.launch {
            medicationRepository.delete(medication)  // 刪除藥品
            loadMedications()  // 刷新列表
        }
    }

    // 重新載入所有藥品資料
    private fun loadMedications() {
        viewModelScope.launch {
            medicationRepository.allMedications.collect { medicationsList ->
                _medications.value = medicationsList // 更新 StateFlow
            }
        }
    }

}