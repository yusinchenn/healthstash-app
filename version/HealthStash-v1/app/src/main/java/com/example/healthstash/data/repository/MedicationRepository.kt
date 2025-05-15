package com.example.healthstash.data.repository

import com.example.healthstash.data.local.MedicationDao
import com.example.healthstash.data.model.Medication
import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val medicationDao: MedicationDao) {
    val allMedications: Flow<List<Medication>> = medicationDao.getAllMedications()

    suspend fun insert(medication: Medication): Long { // <--- 修改返回類型
        return medicationDao.insertMedication(medication)
    }

    suspend fun update(medication: Medication) {
        medicationDao.updateMedication(medication)
    }

    suspend fun delete(medication: Medication) {
        medicationDao.deleteMedication(medication)
    }

    fun getMedicationById(id: Int): Flow<Medication?> {
        return medicationDao.getMedicationById(id)
    }

//    fun getLowStockMedications(threshold: Int): Flow<List<Medication>> {
//        return medicationDao.getLowStockMedications(threshold)
//    }

    suspend fun medicationExists(name: String): Boolean {
        return medicationDao.medicationExists(name)
    }


    suspend fun medicationExistsByNameAndNotId(name: String, excludeId: Int): Boolean {
        return medicationDao.medicationExistsByNameAndNotId(name, excludeId)
    }
}