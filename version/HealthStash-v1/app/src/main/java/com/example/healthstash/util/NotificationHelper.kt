package com.example.healthstash.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.healthstash.MainActivity // 點擊通知時打開的 Activity
import com.example.healthstash.R // 需要一個通知圖示，例如 ic_notification
import com.example.healthstash.data.model.Medication

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_REMINDER = "medication_reminder_channel"
        const val CHANNEL_NAME_REMINDER = "用藥提醒"
        const val CHANNEL_ID_LOW_STOCK = "low_stock_warning_channel"
        const val CHANNEL_NAME_LOW_STOCK = "低庫存警告"

        const val EXTRA_MEDICATION_ID = "medication_id"
        const val EXTRA_MEDICATION_NAME = "medication_name"
        const val EXTRA_NOTIFICATION_ID = "notification_id" // 用於 AlarmReceiver
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDER,
                CHANNEL_NAME_REMINDER,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "提醒您按時服用藥品或保健品"
                enableLights(true)
                lightColor = android.graphics.Color.CYAN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(reminderChannel)

            val lowStockChannel = NotificationChannel(
                CHANNEL_ID_LOW_STOCK,
                CHANNEL_NAME_LOW_STOCK,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "提醒您藥品庫存不足"
            }
            notificationManager.createNotificationChannel(lowStockChannel)
        }
    }

    fun showMedicationReminderNotification(medicationId: Int, medicationName: String, notificationId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // 可以帶參數，以便打開 App 時導航到特定藥品
            putExtra(EXTRA_MEDICATION_ID, medicationId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId, // requestCode 需唯一，或使用 medicationId
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
            .setContentTitle("HealthStash 用藥提醒")
            .setContentText("該服用 $medicationName 了！")
            .setSmallIcon(R.drawable.ic_notification_icon) // 替換為你的通知圖示
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun showLowStockNotification(medication: Medication) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_MEDICATION_ID, medication.id)
        }
        // 使用藥品 ID 作為通知 ID (加上一個偏移量以避免與提醒通知衝突)
        val notificationId = medication.id + 10000
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_LOW_STOCK)
            .setContentTitle("HealthStash 低庫存警告")
            .setContentText("${medication.name} 即將用完 (剩餘 ${medication.remainingQuantity} 份)，請記得補充。")
            .setSmallIcon(R.drawable.ic_notification_icon) // 替換為你的通知圖示
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}