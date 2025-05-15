package com.example.healthstash.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.healthstash.data.model.Medication
import com.example.healthstash.data.model.MedicationLog
import com.example.healthstash.util.Converters

@Database(entities = [Medication::class, MedicationLog::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao
    abstract fun medicationLogDao(): MedicationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "health stash_database"
                )
                    .fallbackToDestructiveMigration(false) // 僅為開發方便，正式版應處理遷移
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}