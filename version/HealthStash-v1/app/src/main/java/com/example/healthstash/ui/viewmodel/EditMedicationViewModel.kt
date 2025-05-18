package com.example.healthstash.ui.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.lifecycle.*
import com.example.healthstash.data.local.AppDatabase
import com.example.healthstash.data.model.Medication
import com.example.healthstash.data.model.TimeInputState
import com.example.healthstash.data.repository.MedicationRepository
import com.example.healthstash.R
import com.example.healthstash.util.AlarmReceiver
import com.example.healthstash.util.NotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.firstOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.UUID

class EditMedicationViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle // 用於接收導航參數
) : AndroidViewModel(application) {

    private val repository: MedicationRepository
    val medicationId: Int = savedStateHandle.get<Int>("medicationId")!! // 取得要編輯的藥品 ID
    private var originalMedication: Medication? = null // 儲存原始藥品資訊以利比對與更新

    // UI 狀態 (與 AddMedicationViewModel 類似)
    var medicationName by mutableStateOf("")
    var totalQuantityInput by mutableStateOf("")
    // --- 圖示狀態 ---
    var imageUri by mutableStateOf<Uri?>(null) // 用戶從相簿新選擇的圖片 (複製到內部儲存後的 URI)
    @get:DrawableRes
    var selectedDefaultImageResId by mutableStateOf<Int?>(null) // 用戶從列表新選擇的預設圖示 ID

    // --- 用於在 UI 中顯示 "目前" 圖示的狀態 (從DB加載的原始圖示資訊) ---
    // 這兩個狀態讓 UI 可以知道最初加載的是 URI 還是 Drawable ID
    var initialIconUriString by mutableStateOf<String?>(null)
    @get:DrawableRes
    var initialIconDrawableResId by mutableStateOf<Int?>(null)

    // 使用時間欄位列表
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

    // --- 新增：圖片儲存邏輯 (與 AddMedicationViewModel 相同) ---
    private fun saveImageFromUriToInternalStorage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            // 創建一個更唯一的子目錄名，例如基於 medicationId (如果需要在藥品刪除時清理)
            // 或者一個通用的圖片目錄
            val imageDir = File(context.filesDir, "medication_images")
            if (!imageDir.exists()) imageDir.mkdirs()

            val imageFile = File(imageDir, "${UUID.randomUUID()}.jpg") // 或其他命名策略
            val outputStream = FileOutputStream(imageFile)

            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(imageFile) // 返回 file:/// URI
        } catch (e: Exception) {
            Log.e("EditMedicationVM", "Error saving image from URI: $sourceUri", e)
            null
        }
    }

    fun onGalleryImageSelected(uri: Uri) { // Context 可以從 getApplication() 獲取
        viewModelScope.launch(Dispatchers.IO) {
            val copiedUri = saveImageFromUriToInternalStorage(getApplication(), uri)
            launch(Dispatchers.Main) {
                if (copiedUri != null) {
                    imageUri = copiedUri // 用戶選擇了新的相簿圖片 (已複製)
                    selectedDefaultImageResId = null // 清除預設圖示選擇
                    // initialIconUriString = null // 清除初始DB圖示，因為用戶已做新選擇
                    // initialIconDrawableResId = null
                } else {
                    Toast.makeText(getApplication(), "圖片儲存失敗", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun onDefaultImageSelected(@DrawableRes resId: Int) {
        selectedDefaultImageResId = resId // 用戶選擇了新的預設圖示
        imageUri = null // 清除相簿圖片選擇
        // initialIconUriString = null
        // initialIconDrawableResId = null
    }
    // --- 圖片邏輯結束 ---

    // 載入藥品詳細資訊並填入 UI 狀態
    private fun loadMedicationDetails() {
        viewModelScope.launch {
            originalMedication = repository.getMedicationById(medicationId).firstOrNull()
            originalMedication?.let { med ->
                medicationName = med.name
                totalQuantityInput = med.totalQuantity.toString()

                // 初始化圖示狀態 (重設用戶在UI上的當前選擇)
                imageUri = null
                selectedDefaultImageResId = null

                // 設置初始圖示狀態，用於UI首次顯示
                initialIconUriString = med.iconUriString
                initialIconDrawableResId = med.iconDrawableResId

                // 將用藥時間轉換為 TimeInputState 清單
                usageTimesList.clear()
                med.usageTimes.forEach { timeStr ->
                    val parts = timeStr.split(":")
                    if (parts.size == 2 && parts[0].length == 2 && parts[1].length == 2) {
                        usageTimesList.add(TimeInputState(parts[0][0].toString(), parts[0][1].toString(), parts[1][0].toString(), parts[1][1].toString()))
                    } else {
                        usageTimesList.add(TimeInputState())
                    }
                }
                if (usageTimesList.isEmpty()) usageTimesList.add(TimeInputState()) // 預設新增一筆
            }
        }
    }

    // 表單輸入處理
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

    // 用藥時間欄位操作
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

    // 驗證邏輯
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

    //更新藥品資料並設定提醒
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

            // --- 決定最終的圖示資訊，基於 Medication 模型的新欄位 ---
            var finalIconUriString: String? = null
            @DrawableRes var finalIconDrawableResId: Int? = null

            if (imageUri != null) { // 1. 用戶新選擇了相簿圖片 (imageUri 是複製到內部的 URI)
                finalIconUriString = imageUri.toString()
                finalIconDrawableResId = null
            } else if (selectedDefaultImageResId != null) { // 2. 用戶新選擇了預設圖示列表中的圖示
                finalIconDrawableResId = selectedDefaultImageResId
                finalIconUriString = null
            } else { // 3. 用戶未做任何圖示更改，保留原始圖示
                finalIconUriString = oldMed.iconUriString
                finalIconDrawableResId = oldMed.iconDrawableResId
            }
            // 如果原始圖示也沒有，可以設定一個應用程式級別的預設
            if (finalIconUriString == null && finalIconDrawableResId == null) {
                finalIconDrawableResId = R.drawable.ic_default_med
            }

            val newRemainingQuantity = when {
                quantity < oldMed.totalQuantity -> minOf(oldMed.remainingQuantity, quantity) // 總量減少，剩餘不超新總量
                quantity > oldMed.totalQuantity -> oldMed.remainingQuantity + (quantity - oldMed.totalQuantity) // 總量增加，增加的量也算入剩餘
                else -> oldMed.remainingQuantity // 總量不變，剩餘不變
            }.coerceIn(0, quantity)

            val updatedMedication = oldMed.copy(
                name = name,
                iconUriString = finalIconUriString,
                iconDrawableResId = finalIconDrawableResId,
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

    // 刪除藥品資料
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

    // 設定與取消提醒鬧鐘
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
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        medication.usageTimes.forEachIndexed { index, _ ->
            val intent = Intent(context, AlarmReceiver::class.java).apply { action = "com.example.health stash.TAKE_MEDICATION" }
            val reqCode = medication.id * 1000 + index
            val pIntent = PendingIntent.getBroadcast(context, reqCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if (pIntent != null) { alarmManager.cancel(pIntent); pIntent.cancel() }
        }
    }

    // ViewModel 提供建構方法
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