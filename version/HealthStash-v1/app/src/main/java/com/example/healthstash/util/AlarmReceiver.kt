package com.example.healthstash.util

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(NotificationHelper.EXTRA_MEDICATION_NAME) ?: "提醒"
        val message = intent.getStringExtra("message") ?: "該吃藥囉！"

        NotificationHelper.showNotification(
            context = context,
            title = title,
            message = message
        )
    }
}

