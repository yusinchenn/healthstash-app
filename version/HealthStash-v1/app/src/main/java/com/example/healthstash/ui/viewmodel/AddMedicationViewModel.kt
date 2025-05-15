package com.example.healthstash.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthstash.R
import com.example.healthstash.data.local.AppDatabase
import com.example.healthstash.data.model.Medication
import com.example.healthstash.data.model.TimeInputState
import com.example.healthstash.data.repository.MedicationRepository
import com.example.healthstash.util.AlarmReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

class AddMedicationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: MedicationRepository

    // Input states
    var medicationName by mutableStateOf("")
    var totalQuantityInput by mutableStateOf("")
    var imageUri by mutableStateOf<Uri?>(null)
    var selectedDefaultImageResId by mutableStateOf<Int?>(null)

    // *** 用於多個時間輸入的新狀態 ***
    val usageTimesList = mutableStateListOf(TimeInputState()) // 使用 mutableStateListOf 以便 Compose 能觀察到列表內部元素的變化

    // Error states for validation
    var medicationNameError by mutableStateOf<String?>(null)
    var totalQuantityError by mutableStateOf<String?>(null)

    private var nameCheckJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MedicationRepository(database.medicationDao())
    }

    fun onMedicationNameChange(newName: String) {
        medicationName = newName
        medicationNameError = null

        if (newName.length > 10) {
            medicationNameError = "限制10字以內"
            nameCheckJob?.cancel()
            return
        }
        if (newName.isBlank()) {
            medicationNameError = "藥品名稱不能為空"
            nameCheckJob?.cancel()
            return
        }
        nameCheckJob?.cancel()
        nameCheckJob = viewModelScope.launch {
            delay(500)
            if (repository.medicationExists(newName.trim())) {
                medicationNameError = "項目已存在"
            }
        }
    }

    fun onTotalQuantityChange(newQuantity: String) {
        totalQuantityInput = newQuantity
        totalQuantityError = null
        if (newQuantity.isEmpty()) return

        val number = newQuantity.toIntOrNull()
        if (number == null || number !in 1..500) {
            totalQuantityError = "請輸入1~500的數字"
        }
    }

    // --- 方法來管理時間輸入列表 ---
    fun addTimeField() {
        if (usageTimesList.size < 4) {
            usageTimesList.add(TimeInputState())
        }
    }

    fun removeTimeField(timeState: TimeInputState) {
        if (usageTimesList.size > 1) {
            usageTimesList.remove(timeState)
        } else if (usageTimesList.size == 1) {
            // 如果是最後一個，則清空它而不是移除
            usageTimesList[0] = TimeInputState()
        }
    }

    fun updateTimeDigit(timeId: String, digitIndex: Int, digitValue: String) {
        val index = usageTimesList.indexOfFirst { it.id == timeId }
        if (index != -1) {
            val current = usageTimesList[index]
            val newDigit = digitValue.firstOrNull()?.toString() ?: ""
            val updatedTime = when (digitIndex) {
                0 -> current.copy(h1 = newDigit)
                1 -> current.copy(h2 = newDigit)
                2 -> current.copy(m1 = newDigit)
                3 -> current.copy(m2 = newDigit)
                else -> current
            }
            // 即時驗證並更新錯誤
            usageTimesList[index] = updatedTime.copy(error = updatedTime.validate())
        }
    }
    // --- 時間輸入管理結束 ---


    private fun validateAllInputs(): Boolean {
        // 觸發驗證
        onMedicationNameChange(medicationName)
        onTotalQuantityChange(totalQuantityInput)

        var allTimesValid = true
        usageTimesList.forEachIndexed { index, timeState ->
            val error = timeState.validate()
            usageTimesList[index] = timeState.copy(error = error)
            if (timeState.isFilled() && error != null) {
                allTimesValid = false
            }
        }

        return medicationNameError == null &&
                totalQuantityError == null &&
                allTimesValid // 所有填寫的時間都要有效，但不要求必填
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun addMedication(onSuccess: () -> Unit) {
        if (!validateAllInputs()) {
            Toast.makeText(getApplication(), "請修正錯誤的輸入或填寫必填項", Toast.LENGTH_SHORT).show()
            return
        }

        val name = medicationName.trim()
        val quantity = totalQuantityInput.toInt() // 已在驗證中檢查

        val validTimes = usageTimesList.mapNotNull { it.toTimeString() }
        if (validTimes.isEmpty()) {
            Toast.makeText(
                getApplication(),
                "尚未設定提醒時間，將不會通知您",
                Toast.LENGTH_SHORT
            ).show()
        }
        val iconResId = selectedDefaultImageResId ?: R.drawable.ic_default_med

        val medicationToInsert = Medication(
            name = name,
            iconResId = iconResId.toString(),
            usageTimes = validTimes,
            totalQuantity = quantity,
            remainingQuantity = quantity, // 🔁 原本是 String，現在直接儲存 Int
            imageUri = imageUri?.toString()
        )


        viewModelScope.launch {
            val newMedicationId = repository.insert(medicationToInsert)
            if (newMedicationId > 0) {
                val insertedMedication = medicationToInsert.copy(id = newMedicationId.toInt())
                scheduleNotificationsForMedication(insertedMedication)
                onSuccess()
            } else {
                Toast.makeText(getApplication(), "新增藥品失敗", Toast.LENGTH_SHORT).show()
            }
            clearForm()
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleNotificationsForMedication(medicationWithId: Medication) {
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // ✅ 防呆：檢查精確鬧鐘權限
        if (!alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "尚未授權精確鬧鐘，無法設定提醒", Toast.LENGTH_SHORT).show()
            return
        }

        if (medicationWithId.id == 0 && medicationWithId.usageTimes.isNotEmpty()) {
            Toast.makeText(context, "無法設定提醒：藥品ID無效", Toast.LENGTH_SHORT).show()
            return
        }

        medicationWithId.usageTimes.forEachIndexed { index, timeString ->
            val timeParts = timeString.split(":")
            val hour = timeParts.getOrNull(0)?.toIntOrNull()
            val minute = timeParts.getOrNull(1)?.toIntOrNull()

            if (hour != null && minute != null) {
                val calendar = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (before(Calendar.getInstance())) {
                        add(Calendar.DATE, 1) // 設為明天
                    }
                }

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    putExtra("title", "用藥提醒")
                    putExtra("message", "${medicationWithId.name} 的用藥時間到了")
                }

                val pendingIntentRequestCode = medicationWithId.id * 1000 + index

                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    pendingIntentRequestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (_: SecurityException) {
                    Toast.makeText(
                        context,
                        "設定提醒失敗：未授權精確鬧鐘",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }



    fun clearForm() {
        medicationName = ""
        totalQuantityInput = ""
        imageUri = null
        selectedDefaultImageResId = null
        usageTimesList.clear()
        usageTimesList.add(TimeInputState()) // 重置為一個空的時間輸入

        medicationNameError = null
        totalQuantityError = null
        nameCheckJob?.cancel()
    }
}