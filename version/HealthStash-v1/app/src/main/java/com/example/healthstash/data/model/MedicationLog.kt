package com.example.healthstash.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_logs")
data class MedicationLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationName: String,
    val timestamp: Long, // System.currentTimeMillis()
    val dosage: String // 例如 "1 顆"
)