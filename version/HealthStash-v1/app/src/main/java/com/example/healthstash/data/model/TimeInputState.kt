package com.example.healthstash.data.model

import java.util.UUID

data class TimeInputState(
    val h1: String = "", // 小時的第一位 (0-2)
    val h2: String = "", // 小時的第二位 (0-9)
    val m1: String = "", // 分鐘的第一位 (0-5)
    val m2: String = "", // 分鐘的第二位 (0-9)
    val error: String? = null, // 錯誤訊息
    val id: String = UUID.randomUUID().toString() // 唯一標識符，用於列表操作
) {
    fun isFilled(): Boolean = h1.isNotBlank() || h2.isNotBlank() || m1.isNotBlank() || m2.isNotBlank()

    fun validate(): String? {
        if (!isFilled()) return null // 沒有輸入不報錯

        val time = toTimeString() ?: return "格式錯誤"
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull()
        val minute = parts.getOrNull(1)?.toIntOrNull()

        return when {
            hour == null || hour !in 0..23 -> "小時應為 00~23"
            minute == null || minute !in 0..59 -> "分鐘應為 00~59"
            else -> null
        }
    }

    fun toTimeString(): String? {
        if (listOf(h1, h2, m1, m2).any { it.length != 1 || !it.all { c -> c.isDigit() } }) return null
        return "${h1}${h2}:${m1}${m2}"
    }
}
