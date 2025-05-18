package com.example.healthstash.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.healthstash.ui.components.MedicationCard
import com.example.healthstash.ui.viewmodel.MainViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.example.healthstash.ui.navigation.Screen

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavHostController
) {
    val medications by viewModel.medications.collectAsState() // 觀察並收集藥品資料
    val context = LocalContext.current // 取得當前 Context 用於顯示 Toast

    // 通知權限請求 (Android 13+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notificationPermissionState.status.isGranted && !notificationPermissionState.status.shouldShowRationale) {
                notificationPermissionState.launchPermissionRequest()
            }
        }
        if (notificationPermissionState.status.shouldShowRationale) {
            Toast.makeText(context, "需要通知權限以提醒您用藥", Toast.LENGTH_LONG).show()
        }
    }
    // 精確鬧鐘權限 (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        //val scheduleExactAlarmPermission = rememberPermissionState(Manifest.permission.SCHEDULE_EXACT_ALARM)
        LaunchedEffect(Unit) {
            // if (!scheduleExactAlarmPermission.status.isGranted) {
            // 通常不會直接 launchPermissionRequest，而是引導到設定
            // }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("我的藥品/保健品") },
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
        ) {
            if (medications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "尚無資料",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                navController.navigate(Screen.AddMedicationScreen.route)
                            }
                        ) {
                            Text("新增藥品")
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(medications, key = { it.id }) { medication ->
                        MedicationCard(
                            medication = medication,
                            onEditClick = { med ->
                                // *** 導航到編輯畫面並傳遞 ID ***
                                navController.navigate(Screen.EditMedicationScreen.createRoute(med.id))
                            },
                            onTakenClick = { med, dosage ->
                                viewModel.markAsTaken(med, dosage)
                                Toast.makeText(context, "${med.name} 已服用 1 劑", Toast.LENGTH_SHORT).show()
                            },
                            onDeleteClick = { med ->
                                viewModel.deleteMedication(med)
                                Toast.makeText(context, "${med.name} 已刪除", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }
}