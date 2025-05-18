package com.example.healthstash.ui.screens

import android.Manifest
import android.app.Application
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.* // Import all filled icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.healthstash.R
import com.example.healthstash.ui.viewmodel.EditMedicationViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.core.net.toUri

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun EditMedicationScreen(
    navController: NavController,
    medicationId: Int // 從導航接收
) {
    val application = LocalContext.current.applicationContext as Application
    // 使用 Factory 創建 ViewModel
    val viewModel: EditMedicationViewModel = viewModel(
        factory = EditMedicationViewModel.Factory(application, medicationId)
    )

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.onGalleryImageSelected(it) } // 移除 context 參數，ViewModel 內部獲取
        }
    )
    val defaultImageResList = listOf(
        R.drawable.med1, R.drawable.med2, R.drawable.med3, R.drawable.med4, R.drawable.med5
    )
    val readStoragePermission = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)

    val medicationNameError by rememberUpdatedState(viewModel.medicationNameError)
    val totalQuantityError by rememberUpdatedState(viewModel.totalQuantityError)
    val usageTimesList = viewModel.usageTimesList

    var showDeleteDialog by remember { mutableStateOf(false) }

    // 從 ViewModel 獲取用於顯示的圖示狀態
    val displayImageUri by rememberUpdatedState(viewModel.imageUri)
    val displaySelectedDefaultResId by rememberUpdatedState(viewModel.selectedDefaultImageResId)
    val initialDbIconUri by rememberUpdatedState(viewModel.initialIconUriString)
    val initialDbDrawableResId by rememberUpdatedState(viewModel.initialIconDrawableResId)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("編輯藥品/保健品") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val isFormValid = medicationNameError == null &&
                    totalQuantityError == null &&
                    viewModel.medicationName.isNotBlank() &&
                    viewModel.totalQuantityInput.isNotBlank()

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                // 名稱輸入區塊 (與 AddMedicationScreen 相同)
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Medication, "藥品名稱", tint = MaterialTheme.colorScheme.primary)
                        Text("藥品名稱", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.medicationName,
                        onValueChange = viewModel::onMedicationNameChange,
                        label = { Text("請輸入藥品/保健品名稱") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, isError = medicationNameError != null,
                        supportingText = { if (medicationNameError != null) Text(medicationNameError!!, color = MaterialTheme.colorScheme.error) }
                    )
                }

                // 圖片選擇區塊
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Collections, "選擇圖片", tint = MaterialTheme.colorScheme.primary)
                        Text("選擇藥品圖示", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    // 當前圖示預覽
                    Box(
                        modifier = Modifier.size(96.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant).align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            displayImageUri != null -> { // 1. 用戶新選了相簿圖 (已複製到內部)
                                AsyncImage(model = displayImageUri, contentDescription = "新選擇的圖片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            displaySelectedDefaultResId != null -> { // 2. 用戶新選了預設圖示
                                Image(painter = painterResource(id = displaySelectedDefaultResId!!), contentDescription = "新選擇的預設圖片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            !initialDbIconUri.isNullOrBlank() -> { // 3. 顯示從 DB 加載的 URI 圖示
                                AsyncImage(model = initialDbIconUri!!.toUri(), contentDescription = "目前藥品圖片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            initialDbDrawableResId != null && initialDbDrawableResId != 0 -> { // 4. 顯示從 DB 加載的 Drawable 圖示
                                Image(painter = painterResource(id = initialDbDrawableResId!!), contentDescription = "目前藥品圖片", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                            else -> { // 5. 通用預設圖示
                                Icon(Icons.Filled.Medication, "預設藥品圖示", modifier = Modifier.size(48.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // 圖片選擇列表
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(Color.White).clickable {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU || readStoragePermission.status.isGranted) {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            } else { readStoragePermission.launchPermissionRequest() }
                        }.border(2.dp, if (displayImageUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.PhotoLibrary, "從相簿", Modifier.size(32.dp))
                                Text("從相簿", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                            }
                        }
                        defaultImageResList.forEach { resId ->
                            Image(painter = painterResource(id = resId), contentDescription = "預設圖示 $resId",
                                modifier = Modifier.size(96.dp).clip(CircleShape).background(Color.White)
                                    .border(2.dp, if (displaySelectedDefaultResId == resId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { viewModel.onDefaultImageSelected(resId) }, // 使用新的 ViewModel 方法
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                // 用藥時間輸入區塊 (與 AddMedicationScreen 相同)
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Schedule, "時間", tint = MaterialTheme.colorScheme.primary)
                        Text("用藥時間（最多 4 組）", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    usageTimesList.forEach { timeState -> key(timeState.id) {
                        TimeInputRow(timeState, { di, dv -> viewModel.updateTimeDigit(timeState.id, di, dv) }, { viewModel.removeTimeField(timeState) }, usageTimesList.size > 1)
                    }}
                    if (usageTimesList.size < 4) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.addTimeField() }) {
                                Icon(Icons.Filled.Add, "新增時間"); Spacer(Modifier.width(4.dp)); Text("新增一組時間")
                            }
                        }
                    }
                }

                // 總數量輸入區塊 (與 AddMedicationScreen 相同)
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.Inventory2, "藥品數量", tint = MaterialTheme.colorScheme.primary)
                        Text("藥品數量", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.totalQuantityInput,
                        onValueChange = viewModel::onTotalQuantityChange,
                        label = { Text("請輸入總數量 (1~500)") },
                        modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, isError = totalQuantityError != null,
                        supportingText = { if (totalQuantityError != null) Text(totalQuantityError!!, color = MaterialTheme.colorScheme.error) }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button( // *** 刪除按鈕 ***
                    onClick = { showDeleteDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "刪除")
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("刪除")
                }
                Button( // *** 完成按鈕 ***
                    onClick = {
                        viewModel.updateMedication {
                            Toast.makeText(context, "${viewModel.medicationName} 已更新", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    },
                    enabled = isFormValid
                ) {
                    Icon(Icons.Filled.Done, contentDescription = "完成")
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("完成")
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("確認刪除") },
            text = { Text("您確定要刪除 ${viewModel.medicationName} 嗎？此操作無法復原。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.deleteMedication { navController.popBackStack() }
                }) { Text("刪除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("取消") } }
        )
    }
}