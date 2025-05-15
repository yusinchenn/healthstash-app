package com.example.healthstash.ui.screens

import android.Manifest
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.healthstash.R
import com.example.healthstash.data.model.TimeInputState
import com.example.healthstash.ui.viewmodel.AddMedicationViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

@RequiresApi(Build.VERSION_CODES.S)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun AddMedicationScreen(
    navController: NavController,
    viewModel: AddMedicationViewModel
) {
    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let {
                viewModel.imageUri = it
                viewModel.selectedDefaultImageResId = null
            }
        }
    )
    val defaultImageResList = listOf(
        R.drawable.med1, R.drawable.med2, R.drawable.med3, R.drawable.med4, R.drawable.med5
    )
    val readStoragePermission = rememberPermissionState(Manifest.permission.READ_EXTERNAL_STORAGE)
    val medicationNameError by rememberUpdatedState(viewModel.medicationNameError)
    val totalQuantityError by rememberUpdatedState(viewModel.totalQuantityError)
    val usageTimesList = viewModel.usageTimesList

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新增藥品/保健品") },
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
                .verticalScroll(rememberScrollState()), // 主 Column 滾動
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                // 名稱輸入區塊
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Medication, // 或 Icons.Filled.MedicalServices
                            contentDescription = "藥品名稱",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("藥品名稱", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.medicationName,
                        onValueChange = viewModel::onMedicationNameChange,
                        label = { Text("請輸入藥品/保健品名稱") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = medicationNameError != null,
                        supportingText = {
                            if (medicationNameError != null) Text(
                                medicationNameError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
                // 圖片選擇區塊
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Collections,
                            contentDescription = "選擇圖片",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("選擇藥品圖示", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // --- 相簿選擇項 ---
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    } else if (readStoragePermission.status.isGranted) {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    } else {
                                        readStoragePermission.launchPermissionRequest()
                                        Toast.makeText(context, "請授予圖片讀取權限", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .border(2.dp,
                                    if (viewModel.imageUri != null) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = "從相簿新增",
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    "從相簿新增",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        // --- 預設圖片 ---
                        defaultImageResList.forEach { resId ->
                            Image(
                                painter = painterResource(id = resId),
                                contentDescription = "預設圖示 $resId",
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(
                                        2.dp,
                                        if (viewModel.selectedDefaultImageResId == resId)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline,
                                        CircleShape
                                    )
                                    .clickable {
                                        viewModel.selectedDefaultImageResId = resId
                                        viewModel.imageUri = null
                                    },
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
                // 用藥時間輸入區塊
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = "時間", tint = MaterialTheme.colorScheme.primary)
                        Text("用藥時間（最多 4 組）", style = MaterialTheme.typography.titleMedium)
                    }

                    Spacer(Modifier.height(12.dp))

                    usageTimesList.forEach { timeState ->
                        key(timeState.id) {
                            TimeInputRow(
                                timeState = timeState,
                                onDigitChange = { digitIndex, digitValue ->
                                    viewModel.updateTimeDigit(timeState.id, digitIndex, digitValue)
                                },
                                onRemove = { viewModel.removeTimeField(timeState) },
                                showRemoveButton = usageTimesList.size > 1
                            )
                        }
                    }

                    if (usageTimesList.size < 4) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { viewModel.addTimeField() }) {
                                Icon(Icons.Filled.Add, contentDescription = "新增時間")
                                Spacer(Modifier.width(4.dp))
                                Text("新增一組時間")
                            }
                        }
                    }
                }
                // 總數量輸入區塊
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Inventory2, // 或 Icons.Filled.Numbers
                            contentDescription = "藥品數量",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("藥品數量", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = viewModel.totalQuantityInput,
                        onValueChange = viewModel::onTotalQuantityChange,
                        label = { Text("請輸入總數量 (1~500)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = totalQuantityError != null,
                        supportingText = {
                            if (totalQuantityError != null) Text(
                                totalQuantityError!!,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            medicationNameError == null && totalQuantityError == null &&
                    viewModel.medicationName.isNotBlank() && viewModel.totalQuantityInput.isNotBlank() &&
                    usageTimesList.any { it.isFilled() && it.validate() == null } && // 至少有一個有效時間
                    usageTimesList.all { !it.isFilled() || it.validate() == null } // 所有填寫的時間都有效

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        viewModel.clearForm()
                        navController.popBackStack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "退出") // 使用 AutoMirrored
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("退出")
                }
                Button(
                    onClick = {
                        // 確保 viewModel 中有 addMedication 方法
                        viewModel.addMedication {
                            Toast.makeText(context, "${viewModel.medicationName} 已新增", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        }
                    }
                ) {
                    Icon(Icons.Filled.Done, contentDescription = "新增")
                    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                    Text("完成")
                }
            }
        }
    }
}

@Composable
fun TimeInputRow(
    timeState: TimeInputState,
    onDigitChange: (digitIndex: Int, value: String) -> Unit,
    onRemove: () -> Unit,
    showRemoveButton: Boolean
) {
    val focusRequesters = remember { List(4) { FocusRequester() } }
    val focusManager = LocalFocusManager.current

    Column { // 將 Row 和錯誤訊息包在 Column 中
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TimeDigitTextField(timeState.h1, 0, focusRequesters, onDigitChange, focusManager)
            TimeDigitTextField(timeState.h2, 1, focusRequesters, onDigitChange, focusManager)
            Text(
                text = ":",
                style = MaterialTheme.typography.headlineSmall.copy(color = if (timeState.error != null) MaterialTheme.colorScheme.error else LocalContentColor.current),
                modifier = Modifier.padding(horizontal = 2.dp)
            )
            TimeDigitTextField(timeState.m1, 2, focusRequesters, onDigitChange, focusManager)
            TimeDigitTextField(timeState.m2, 3, focusRequesters, onDigitChange, focusManager)

            if (showRemoveButton) {
                IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.RemoveCircleOutline, "移除時間", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (timeState.error != null) {
            Text(
                text = timeState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp, top = 2.dp)
            )
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun TimeDigitTextField(
    value: String,
    digitIndex: Int, // 0:H1, 1:H2, 2:M1, 3:M2
    focusRequesters: List<FocusRequester>,
    onDigitChange: (digitIndex: Int, value: String) -> Unit,
    focusManager: FocusManager
) {
    val maxLength = 1
    OutlinedTextField(
        value = value,
        onValueChange = { input: String ->
            if (input.length <= maxLength && input.all { char: Char -> char.isDigit() }) {
                onDigitChange(digitIndex, input)
                if (input.length == maxLength && digitIndex < 3) {
                    focusManager.moveFocus(FocusDirection.Next)
                }
            }
        },
        modifier = Modifier
            .width(60.dp)
            .focusRequester(focusRequesters[digitIndex])
            .onKeyEvent { keyEvent ->
                if (keyEvent.type == KeyEventType.KeyUp &&
                    keyEvent.key == Key.Backspace &&
                    value.isEmpty() &&
                    digitIndex > 0
                ) {
                    focusManager.moveFocus(FocusDirection.Previous)
                    true
                } else {
                    false
                }
            },
        textStyle = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Center),
        singleLine = true,
        keyboardOptions = KeyboardOptions.Default.copy(
            keyboardType = KeyboardType.Number,
            imeAction = if (digitIndex < 3) ImeAction.Next else ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onNext = { if (digitIndex < 3) focusManager.moveFocus(FocusDirection.Next) },
            onDone = { focusManager.clearFocus() }
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = if (value.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            unfocusedBorderColor = if (value.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outline,
        )
    )
}