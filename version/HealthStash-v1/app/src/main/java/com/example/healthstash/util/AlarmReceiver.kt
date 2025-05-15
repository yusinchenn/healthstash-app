package com.example.healthstash.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.healthstash.data.local.AppDatabase
import com.example.healthstash.data.model.Medication
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers


class AlarmReceiver : BroadcastReceiver() {
    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job) // Use IO dispatcher for DB operations

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync() // Keep the BroadcastReceiver alive

        scope.launch {
            try {
                if (intent.action == "com.example.health stash.TAKE_MEDICATION") {
                    // ... (現有的提醒邏輯) ...
                } else if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOK_POWER") {
                    Log.d("AlarmReceiver", "Boot completed, rescheduling alarms.")
                    val medicationDao = AppDatabase.getDatabase(context.applicationContext).medicationDao()
                    val allMedications = medicationDao.getAllMedications().firstOrNull() // Get a snapshot

                    allMedications?.forEach { medication ->
                        if (medication.usageTimes.isNotEmpty()) {
                            // 這裡需要一個可以被靜態調用或在 Receiver 中實例化的排程邏輯
                            // 不能直接調用 ViewModel 中的方法
                            // 可以將 scheduleNotificationsForMedication 的核心邏輯提取到一個 Util 類
                            // 例如: AlarmSchedulerUtil(context).scheduleNotificationsForMedication(medication)
                            Log.d("AlarmReceiver", "Rescheduling for ${medication.name}")
                            // 為了演示，假設我們直接複製部分邏輯 (不推薦，應重構)
                            reScheduleAlarmForMedication(context.applicationContext, medication)
                        }
                    }
                }
            } finally {
                pendingResult.finish() // Finish the broadcast
            }
        }
    }

    // 提取出來的、或重新實現的排程邏輯 (僅為示例，應與 ViewModel 中的邏輯保持一致或共用)
    private fun reScheduleAlarmForMedication(context: Context, medication: Medication) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return // 假設鬧鐘設定需要 API S

//        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        medication.usageTimes.forEachIndexed { index, timeString ->
//            // ... (複製或調用與 ViewModel 中 scheduleNotificationsForMedication 相同的邏輯)
//            // ... 來創建 Calendar, Intent, PendingIntent 並設定鬧鐘
//            // ... 確保使用 medication.id (它應該是從資料庫讀取的有效 ID)
//        }
    }
}