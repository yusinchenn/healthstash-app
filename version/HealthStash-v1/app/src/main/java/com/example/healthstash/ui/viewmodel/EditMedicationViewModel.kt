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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthstash.data.local.AppDatabase
import com.example.healthstash.data.model.Medication
import com.example.healthstash.data.model.TimeInputState
import com.example.healthstash.data.repository.MedicationRepository
import com.example.healthstash.util.AlarmReceiver
import com.example.healthstash.util.NotificationHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Calendar

class EditMedicationViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle // 用於接收導航參數
) : AndroidViewModel(application) {

    private val repository: MedicationRepository
    val medicationId: Int = savedStateHandle.get<Int>("medicationId")!! // 從導航參數獲取

    private var originalMedication: Medication? = null

    // UI 狀態 (與 AddMedicationViewModel 類似)
    var medicationName by mutableStateOf("")
    var totalQuantityInput by mutableStateOf("")
    var imageUri by mutableStateOf<Uri?>(null) // 用戶新選擇的圖片URI
    var selectedDefaultImageResId by mutableStateOf<Int?>(null) // 用戶新選擇的預設圖片ID
    var currentIconStringFromDb by mutableStateOf<String?>(null)

    val usageTimesList = mutableStateListOf<TimeInputState>()

    // 錯誤狀態
    var medicationNameError by mutableStateOf<String?>(null)
    var totalQuantityError by mutableStateOf<String?>(null)

    private var nameCheckJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MedicationRepository(database.medicationDao())
        loadMedicationDetails()
    }

    private fun loadMedicationDetails() {
        viewModelScope.launch {
            originalMedication = repository.getMedicationById(medicationId).firstOrNull()
            originalMedication?.let { med ->
                medicationName = med.name
                totalQuantityInput = med.totalQuantity.toString()

                // 初始化圖示狀態
                imageUri = null // 先重置用戶選擇的 URI
                selectedDefaultImageResId = null // 先重置用戶選擇的預設圖示 ID

                if (med.iconResId != null) {
                    val context = getApplication<Application>().applicationContext
                    @Suppress("DiscouragedApi")
                    val resId = context.resources.getIdentifier(med.iconResId, "drawable", context.packageName)
                    if (resId != 0) {
                        selectedDefaultImageResId = resId
                    } else {
                        currentIconStringFromDb = med.iconResId
                    }
                }

                usageTimesList.clear()
                if (med.usageTimes.isNotEmpty()) {
                    med.usageTimes.forEach { timeStr ->
                        val parts = timeStr.split(":")
                        if (parts.size == 2 && parts[0].length == 2 && parts[1].length == 2) {
                            usageTimesList.add(TimeInputState(parts[0][0].toString(), parts[0][1].toString(), parts[1][0].toString(), parts[1][1].toString()))
                        } else {
                            usageTimesList.add(TimeInputState()) // 如果格式不對，加個空的
                        }
                    }
                }
                if (usageTimesList.isEmpty()) { // 如果沒有時間或解析失敗，至少提供一個輸入框
                    usageTimesList.add(TimeInputState())
                }
            }
        }
    }



    fun onMedicationNameChange(newName: String) {
        medicationName = newName
        medicationNameError = null
        if (newName.length > 10) {
            medicationNameError = "限制10字以內"; nameCheckJob?.cancel(); return
        }
        if (newName.isBlank()) {
            medicationNameError = "藥品名稱不能為空"; nameCheckJob?.cancel(); return
        }
        nameCheckJob?.cancel()
        nameCheckJob = viewModelScope.launch {
            delay(500)
            if (repository.medicationExistsByNameAndNotId(newName.trim(), medicationId)) { // 排除自身ID
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

    fun addTimeField() { if (usageTimesList.size < 4) usageTimesList.add(TimeInputState()) }
    fun removeTimeField(timeState: TimeInputState) {
        if (usageTimesList.size > 1) usageTimesList.remove(timeState)
        else usageTimesList[0] = TimeInputState() // 清空最後一個
    }
    fun updateTimeDigit(timeId: String, digitIndex: Int, digitValue: String) {
        val index = usageTimesList.indexOfFirst { it.id == timeId }
        if (index != -1) {
            val current = usageTimesList[index]
            val newDigit = digitValue.firstOrNull()?.toString() ?: ""
            val updated = when (digitIndex) {
                0 -> current.copy(h1 = newDigit); 1 -> current.copy(h2 = newDigit)
                2 -> current.copy(m1 = newDigit); 3 -> current.copy(m2 = newDigit)
                else -> current
            }
            usageTimesList[index] = updated.copy(error = updated.validate())
        }
    }

    private fun validateAllInputsForUpdate(): Boolean {
        onMedicationNameChange(medicationName)
        onTotalQuantityChange(totalQuantityInput)

        var allAttemptedTimesAreValid = true

        usageTimesList.forEachIndexed { index, timeState ->
            val validationError = if (timeState.isFilled()) timeState.validate() else null
            usageTimesList[index] = timeState.copy(error = validationError) // 更新 UI 錯誤狀態

            if (timeState.isFilled() && validationError != null) {
                allAttemptedTimesAreValid = false
            }
        }

        return medicationNameError == null &&
                totalQuantityError == null &&
                medicationName.isNotBlank() &&
                totalQuantityInput.isNotBlank() &&
                allAttemptedTimesAreValid
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun updateMedication(onSuccess: () -> Unit) {
        if (!validateAllInputsForUpdate()) {
            Toast.makeText(getApplication(), "請修正錯誤的輸入或填寫必填項", Toast.LENGTH_SHORT).show()
            return
        }

        originalMedication?.let { oldMed ->
            val name = medicationName.trim()
            val quantity = totalQuantityInput.toInt()
            val validTimes = usageTimesList.mapNotNull { it.toTimeString() }

            // 處理剩餘量：如果總量減少且少於原剩餘量，則剩餘量等於新總量。否則按比例或保持。
            // 簡單策略：如果總量減少，剩餘量不能超過新總量。如果總量增加，維持原剩餘量或按比例增加。
            // 這裡使用一個相對保守的策略：
            val newRemainingQuantity = when {
                quantity < oldMed.totalQuantity -> minOf(oldMed.remainingQuantity, quantity) // 總量減少，剩餘不超新總量
                quantity > oldMed.totalQuantity -> oldMed.remainingQuantity + (quantity - oldMed.totalQuantity) // 總量增加，增加的量也算入剩餘
                else -> oldMed.remainingQuantity // 總量不變，剩餘不變
            }.coerceIn(0, quantity)

            val finalIconString: String? = when {
                imageUri != null -> imageUri.toString()
                selectedDefaultImageResId != null -> {
                    val resName = getApplication<Application>().resources.getResourceEntryName(selectedDefaultImageResId!!)
                    resName
                }
                else -> currentIconStringFromDb // 沒變更圖片時，保留原來的
            }
            val updatedMedication = oldMed.copy(
                name = name,
                iconResId = finalIconString,
                usageTimes = validTimes,
                totalQuantity = quantity,
                remainingQuantity = newRemainingQuantity
            )

            viewModelScope.launch {
                cancelScheduledNotifications(oldMed) // 取消舊藥品的鬧鐘
                repository.update(updatedMedication)
                scheduleNotificationsForMedication(updatedMedication) // 為新藥品設定鬧鐘
                onSuccess()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    fun deleteMedication(onSuccess: () -> Unit) {
        originalMedication?.let { medToDelete ->
            viewModelScope.launch {
                cancelScheduledNotifications(medToDelete)
                repository.delete(medToDelete)
                Toast.makeText(getApplication(), "${medToDelete.name} 已刪除", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleNotificationsForMedication(medicationWithId: Medication) {
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (!alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(context, "尚未授權精確鬧鐘，無法設定提醒", Toast.LENGTH_SHORT).show()
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
                    if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
                }

                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    action = "com.example.healthstash.TAKE_MEDICATION"
                    putExtra(NotificationHelper.EXTRA_MEDICATION_ID, medicationWithId.id)
                    putExtra(NotificationHelper.EXTRA_MEDICATION_NAME, medicationWithId.name)
                    putExtra(NotificationHelper.EXTRA_NOTIFICATION_ID, medicationWithId.id * 1000 + index)
                }

                val requestCode = medicationWithId.id * 1000 + index
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    Toast.makeText(context, "設定鬧鐘失敗：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun cancelScheduledNotifications(medication: Medication) {
        // ... (與 AddMedicationViewModel 中的邏輯相同或提取為共用 Util)
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // ... (完整的取消鬧鐘邏輯)
        medication.usageTimes.forEachIndexed { index, _ ->
            val intent = Intent(context, AlarmReceiver::class.java).apply { action = "com.example.health stash.TAKE_MEDICATION" }
            val reqCode = medication.id * 1000 + index
            val pIntent = PendingIntent.getBroadcast(context, reqCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pIntent != null) { alarmManager.cancel(pIntent); pIntent.cancel() }
        }
    }

    // Factory for creating EditMedicationViewModel with medicationId
    companion object {
        fun Factory(application: Application, medicationId: Int): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    if (modelClass.isAssignableFrom(EditMedicationViewModel::class.java)) {
                        return EditMedicationViewModel(application, SavedStateHandle(mapOf("medicationId" to medicationId))) as T
                    }
                    throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
                }
            }
        }
    }
}