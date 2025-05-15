package com.example.healthstash.util

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
    }

    @TypeConverter
    fun fromIconResId(resId: Int?): String? = resId?.toString()

    @TypeConverter
    fun toIconResId(resIdStr: String?): Int? = resIdStr?.toIntOrNull()

}