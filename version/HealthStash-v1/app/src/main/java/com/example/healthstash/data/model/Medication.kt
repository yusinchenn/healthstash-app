package com.example.healthstash.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.healthstash.util.Converters

@Entity(tableName = "medications")
@TypeConverters(Converters::class)
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconResId: String?, // 可以是 drawable 名稱或圖片路徑/URI 字串
    val usageTimes: List<String>, // 例如 ["08:00", "12:00", "20:00"]
    val totalQuantity: Int,
    var remainingQuantity: Int,
    val imageUri: String?
)