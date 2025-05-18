package com.example.healthstash.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.healthstash.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID_MEDICATION_REMINDER = "medication_reminder_channel"
        const val CHANNEL_NAME_MEDICATION_REMINDER = "用藥提醒"
        const val CHANNEL_DESCRIPTION = "提醒您按時服藥"

        const val EXTRA_MEDICATION_ID = "extra_medication_id"
        const val EXTRA_MEDICATION_NAME = "extra_medication_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        fun showNotification(context: Context, title: String, message: String) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val notificationId = System.currentTimeMillis().toInt()

            val notification = NotificationCompat.Builder(context, CHANNEL_ID_MEDICATION_REMINDER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(notificationId, notification)
        }
    }

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_MEDICATION_REMINDER,
                CHANNEL_NAME_MEDICATION_REMINDER,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}

