package com.example.healthstash.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination

import com.example.healthstash.ui.screens.* // 引入所有 Screens，確保路由對應正確
import com.example.healthstash.ui.viewmodel.* // 引入所有 ViewModel，管理狀態

// 定義底部導航項目的資料結構
data class BottomNavItem(
    val label: String, // 顯示在底部導航列的標籤文字
    val icon: ImageVector, // 顯示在底部導航列的圖示
    val screen: Screen // 這個項目對應的畫面 (screen)
)

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HealthStashAppNavigation() {
    val navController = rememberNavController() // 用來控制導航的 NavController

    // 創建 ViewModel 實例
    val mainViewModel: MainViewModel = viewModel()
    val addMedicationViewModel: AddMedicationViewModel = viewModel()
    val logViewModel: LogViewModel = viewModel()

    // 定義底部導航列項目
    val bottomNavItems = remember {
        listOf(
            BottomNavItem("主畫面", Icons.Filled.Home, Screen.MainScreen), // 主畫面
            BottomNavItem("新增藥品", Icons.Filled.AddCircle, Screen.AddMedicationScreen), // 新增藥品
            BottomNavItem("用藥紀錄", Icons.AutoMirrored.Filled.ListAlt, Screen.LogScreen) // 用藥紀錄
        )
    }

    // Scaffold 來自 Compose，這裡設置底部導航列
    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(navController = navController, items = bottomNavItems) // 配置底部導航列
        }
    ) { innerPadding ->
        //  配置 NavHost，處理頁面導航邏輯
        AppNavHost(
            navController = navController,
            mainViewModel = mainViewModel,
            addMedicationViewModel = addMedicationViewModel,
            logViewModel = logViewModel,
            paddingValues = innerPadding
        )
    }
}

// 底部導航列元件，管理每個導航項目的顯示與選擇狀態
@Composable
fun AppBottomNavigationBar(navController: NavController, items: List<BottomNavItem>) {
    NavigationBar {
        // 獲取當前導航堆疊的條目，來判斷目前選中的頁面
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        // 遍歷每個底部導航項目，並生成對應的按鈕
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) }, // 顯示圖示
                label = { Text(item.label) }, // 顯示文字
                selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true, // 判斷是否是當前選中頁面
                onClick = {
                    // 按下時進行導航
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true // 保持狀態
                        }
                        launchSingleTop = true // 防止重複導航到同一頁面
                        restoreState = true // 恢復狀態
                    }
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppNavHost(
    navController: NavHostController, // 用來控制頁面導航的 Controller
    mainViewModel: MainViewModel, // 主畫面的 ViewModel
    addMedicationViewModel: AddMedicationViewModel, // 新增藥品頁面的 ViewModel
    logViewModel: LogViewModel, // 用藥紀錄頁面的 ViewModel
    paddingValues: PaddingValues // Scaffold 提供的內邊距
) {
    NavHost(
        navController = navController, // 設定導航控制器
        startDestination = Screen.MainScreen.route, // 預設啟動頁面為主畫面
        modifier = Modifier.padding(paddingValues) // 避免與底部導航列重疊
    ) {
        // 🔹 設定各頁面的路由與對應的 Composable
        composable(Screen.MainScreen.route) {
            MainScreen(
                navController = navController,
                viewModel = mainViewModel
            )
        }
        composable(Screen.AddMedicationScreen.route) {
            AddMedicationScreen(
                navController = navController,
                viewModel = addMedicationViewModel
            )
        }
        composable(Screen.LogScreen.route) {
            LogScreen(
                navController = navController,
                viewModel = logViewModel
            )
        }
        composable(
            route = Screen.EditMedicationScreen.route,
            arguments = listOf(navArgument("medicationId") { type = NavType.IntType }) // 帶有參數的路由，用來編輯藥品
        ) { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getInt("medicationId")
            if (medicationId != null) {
                // 若有藥品 ID，顯示編輯頁面
                EditMedicationScreen(
                    navController = navController,
                    medicationId = medicationId
                )
            } else {
                // 若找不到藥品 ID，顯示錯誤並返回上一頁
                Text("錯誤：找不到藥品 ID")
                LaunchedEffect(Unit) {
                    navController.popBackStack() // 返回上一頁
                }
            }
        }
    }
}