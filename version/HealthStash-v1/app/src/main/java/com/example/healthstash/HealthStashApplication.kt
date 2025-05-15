package com.example.healthstash

import android.app.Application
import com.example.healthstash.util.NotificationHelper

class HealthStashApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 建立通知渠道
        NotificationHelper(this).createNotificationChannels()
    }
}