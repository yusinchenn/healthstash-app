package com.example.healthstash.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.healthstash.data.model.Medication
import com.example.healthstash.data.model.MedicationLog
import com.example.healthstash.util.Converters

// 宣告 Room 資料庫，包含兩個 Entity：Medication 與 MedicationLog
// version 是資料庫版本，exportSchema = false 表示不匯出 schema 檔案（可避免版本控管誤入）
@Database(
    entities = [Medication::class, MedicationLog::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class) // 使用自訂的型別轉換器（例如處理時間）
abstract class AppDatabase : RoomDatabase() {

    // 抽象函式，提供 DAO 供外部操作資料庫
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationLogDao(): MedicationLogDao

    companion object {
        // Volatile 確保 INSTANCE 變數在多執行緒中一致性，避免建立多個資料庫實例
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 提供外部取得資料庫實例的方法（單例模式）
        fun getDatabase(context: Context): AppDatabase {
            // 若 INSTANCE 為 null，則同步鎖建立資料庫實例
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,       // 使用 Application Context，避免記憶體洩漏
                    AppDatabase::class.java,         // 資料庫類別
                    "health_stash_database"          // 資料庫檔名（建議用底線命名法）
                )
                    // 若資料庫 schema 有變更而未提供遷移策略，將刪除原資料重建（僅建議開發期使用）
                    .fallbackToDestructiveMigration(false)
                    .build()

                // 將建立的資料庫實例賦值給 INSTANCE 供下次使用
                INSTANCE = instance
                instance
            }
        }
    }
}
