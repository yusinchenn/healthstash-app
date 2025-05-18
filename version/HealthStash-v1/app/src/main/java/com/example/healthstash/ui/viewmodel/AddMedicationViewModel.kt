package com.example.healthstash.ui.viewmodel

import android.app.*
import android.content.*
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthstash.R
import com.example.healthstash.data.local.AppDatabase
import com.example.healthstash.data.model.Medication
import com.example.healthstash.data.model.TimeInputState
import com.example.healthstash.data.repository.MedicationRepository
import com.example.healthstash.util.AlarmReceiver
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.util.*

class AddMedicationViewModel(application: Application) : AndroidViewModel(application) {

    // Repository：負責與資料庫互動
    private val repository: MedicationRepository

    // --- 使用者輸入狀態 ---
    var medicationName by mutableStateOf("")
    var totalQuantityInput by mutableStateOf("")
    var imageUri by mutableStateOf<Uri?>(null)
    var selectedDefaultImageResId by mutableStateOf<Int?>(null)

    // 多筆服藥時間輸入欄位的狀態
    val usageTimesList = mutableStateListOf(TimeInputState())

    // --- 表單錯誤訊息狀態 ---
    var medicationNameError by mutableStateOf<String?>(null)
    var totalQuantityError by mutableStateOf<String?>(null)

    // 用於防止過度查詢的工作參考
    private var nameCheckJob: Job? = null

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MedicationRepository(database.medicationDao())
    }

    // --- 圖片儲存邏輯 ---
    fun saveImageFromUri(context: Context, sourceUri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val imageDir = File(context.filesDir, "medication_images")
            if (!imageDir.exists()) imageDir.mkdirs()

            val imageFile = File(imageDir, "${UUID.randomUUID()}.jpg")
            val outputStream = FileOutputStream(imageFile)

            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun onGalleryImageSelected(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) { // 將文件操作移至 IO 線程
            val copiedUri = saveImageFromUri(context, uri) // 呼叫您新增的儲存方法
            launch(Dispatchers.Main) { // 切回主線程更新 UI 狀態
                if (copiedUri != null) {
                    imageUri = copiedUri // 更新 viewModel 的 imageUri 為複製後的 URI
                    selectedDefaultImageResId = null // 清除預設圖示的選擇
                } else {
                    Toast.makeText(context, "圖片儲存失敗", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // --- 名稱輸入邏輯與即時驗證 ---
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
            delay(500) // 等待使用者輸入穩定
            if (repository.medicationExists(newName.trim())) {
                medicationNameError = "項目已存在"
            }
        }
    }

    // --- 數量輸入驗證 ---
    fun onTotalQuantityChange(newQuantity: String) {
        totalQuantityInput = newQuantity
        totalQuantityError = null
        if (newQuantity.isEmpty()) return

        val number = newQuantity.toIntOrNull()
        if (number == null || number !in 1..500) {
            totalQuantityError = "請輸入1~500的數字"
        }
    }

    // --- 管理時間欄位 ---
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

    // --- 驗證整份表單 ---
    private fun validateAllInputs(): Boolean {
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
                allTimesValid
    }


    // --- 新增藥品並設定提醒 ---
    @RequiresApi(Build.VERSION_CODES.S)
    fun addMedication(onSuccess: () -> Unit) {
        if (!validateAllInputs()) {
            Toast.makeText(getApplication(), "請修正錯誤的輸入或填寫必填項", Toast.LENGTH_SHORT).show()
            return
        }

        val name = medicationName.trim()
        val quantity = totalQuantityInput.toInt()
        val validTimes = usageTimesList.mapNotNull { it.toTimeString() }

        if (validTimes.isEmpty() && usageTimesList.any { it.isFilled() }) { // 如果用戶嘗試填寫時間但都無效
            Toast.makeText(getApplication(), "請輸入有效的用藥時間", Toast.LENGTH_SHORT).show()
            return
        }
        // 如果 validTimes 為空但 usageTimesList 也為空(或都未填寫)，可以彈出"尚未設定提醒時間"的 Toast
        if (validTimes.isEmpty()) {
            Toast.makeText(getApplication(), "尚未設定提醒時間，將不會通知您", Toast.LENGTH_SHORT).show()
        }

        // --- 決定最終的圖示資訊 ---
        var finalIconUriString: String? = null
        @DrawableRes var finalIconDrawableResId: Int? = null

        if (imageUri != null) {
            // imageUri 此時應該是 saveImageFromUri 返回的、指向應用內部儲存的 URI
            finalIconUriString = imageUri.toString()
            finalIconDrawableResId = null // 清除預設圖示ID，因為 URI 優先
        } else if (selectedDefaultImageResId != null) {
            finalIconDrawableResId = selectedDefaultImageResId
            finalIconUriString = null // 清除 URI
        } else {
            // 如果兩者都為 null (用戶未選擇任何圖示)，則使用應用程式的預設圖示
            finalIconDrawableResId = R.drawable.ic_default_med // 確保你有這個預設圖示
            finalIconUriString = null
        }

        val medicationToInsert = Medication(
            name = name,
            iconUriString = finalIconUriString,       // 使用處理後的 URI 字串
            iconDrawableResId = finalIconDrawableResId, // 使用處理後的 Drawable ID
            usageTimes = validTimes,
            totalQuantity = quantity,
            remainingQuantity = quantity
        )

        viewModelScope.launch {
            val newMedicationId = repository.insert(medicationToInsert)
            if (newMedicationId > 0) {
                val insertedMedication = medicationToInsert.copy(id = newMedicationId.toInt())
                scheduleNotificationsForMedication(insertedMedication) // 確保鬧鐘設定使用正確的 ID
                onSuccess()
            } else {
                Toast.makeText(getApplication(), "新增藥品失敗", Toast.LENGTH_SHORT).show()
            }
            clearForm()
        }
    }

    // --- 設定鬧鐘提醒 ---
    @RequiresApi(Build.VERSION_CODES.S)
    private fun scheduleNotificationsForMedication(medicationWithId: Medication) {
        val context = getApplication<Application>().applicationContext
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

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

    // --- 清除表單欄位 ---
    fun clearForm() {
        medicationName = ""
        totalQuantityInput = ""
        imageUri = null
        selectedDefaultImageResId = null
        usageTimesList.clear()
        usageTimesList.add(TimeInputState())
        medicationNameError = null
        totalQuantityError = null
        nameCheckJob?.cancel()
    }
}