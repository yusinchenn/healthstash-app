package com.example.healthstash.data.model

import androidx.annotation.DrawableRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.healthstash.util.Converters

// 定義 Room 資料表 "medications"
@Entity(tableName = "medications")
@TypeConverters(Converters::class) // 使用自訂型別轉換器處理 List<String> 等非原生型別
data class Medication(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val iconUriString: String?,    // 用於 URI (例如，複製到內部儲存後的 file:///... Uri)
    @DrawableRes val iconDrawableResId: Int?, // 用於 Drawable 資源 ID (例如 R.drawable.med1)
    val usageTimes: List<String>,
    val totalQuantity: Int,
    var remainingQuantity: Int
    // 移除舊的 imageUri: String? (如果它與 iconUriString 重複)
    // 移除舊的 iconResId: String (如果它之前是混合用途的)
)
