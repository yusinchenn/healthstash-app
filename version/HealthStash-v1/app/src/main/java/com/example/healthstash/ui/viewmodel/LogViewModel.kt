package com.example.healthstash.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthstash.data.local.AppDatabase
import com.example.healthstash.data.model.MedicationLog
import com.example.healthstash.data.repository.LogRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: LogRepository
    val medicationLogs: StateFlow<List<MedicationLog>>

    init {
        val logDao = AppDatabase.getDatabase(application).medicationLogDao()
        repository = LogRepository(logDao)
        medicationLogs = repository.allLogs
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun formatLogTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }
}