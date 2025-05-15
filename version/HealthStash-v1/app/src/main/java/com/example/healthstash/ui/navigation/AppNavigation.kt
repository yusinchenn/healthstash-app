package com.example.healthstash.ui.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.healthstash.ui.screens.AddMedicationScreen
import com.example.healthstash.ui.screens.LogScreen
import com.example.healthstash.ui.screens.MainScreen
import com.example.healthstash.ui.viewmodel.AddMedicationViewModel
import com.example.healthstash.ui.viewmodel.LogViewModel
import com.example.healthstash.ui.viewmodel.MainViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.healthstash.ui.screens.EditMedicationScreen // *** 導入 EditMedicationScreen ***
// 確保 Screen.kt 被正確導入，如果它在同一個包下，通常不需要顯式導入 Screen 本身，
// 而是可以直接使用其內部物件 Screen.MainScreen 等。
// 如果 Screen.kt 在不同的包，例如 import com.example.health stash.ui.navigation.Screen

data class BottomNavItem(
    val label: String,
    val icon: ImageVector,
    val screen: Screen // Screen 類型保持不變
)

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HealthStashAppNavigation() {
    val navController = rememberNavController()
    // 考慮使用 Hilt 或 Koin 進行 ViewModel 注入，或者通過 ViewModelProvider.Factory 創建
    // 這裡的 viewModel() 假設了 ViewModel 沒有需要 AndroidViewModelApplication 以外的參數
    val mainViewModel: MainViewModel = viewModel()
    val addMedicationViewModel: AddMedicationViewModel = viewModel()
    val logViewModel: LogViewModel = viewModel()

    val bottomNavItems = remember {
        listOf(
            // *** FIX START: Use correct Screen object names ***
            BottomNavItem("主畫面", Icons.Filled.Home, Screen.MainScreen),
            BottomNavItem("新增藥品", Icons.Filled.AddCircle, Screen.AddMedicationScreen),
            BottomNavItem("用藥紀錄", Icons.AutoMirrored.Filled.ListAlt, Screen.LogScreen)
            // *** FIX END ***
        )
    }

    Scaffold(
        bottomBar = {
            AppBottomNavigationBar(navController = navController, items = bottomNavItems)
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            mainViewModel = mainViewModel,
            addMedicationViewModel = addMedicationViewModel,
            logViewModel = logViewModel,
            paddingValues = innerPadding
        )
    }
}

@Composable
fun AppBottomNavigationBar(navController: NavController, items: List<BottomNavItem>) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination?.hierarchy?.any { it.route == item.screen.route } == true,
                onClick = {
                    navController.navigate(item.screen.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    mainViewModel: MainViewModel,
    addMedicationViewModel: AddMedicationViewModel,
    logViewModel: LogViewModel,
    paddingValues: PaddingValues
) {
    NavHost(
        navController = navController,
        // *** FIX START: Use correct Screen object name for startDestination ***
        startDestination = Screen.MainScreen.route,
        // *** FIX END ***
        modifier = Modifier.padding(paddingValues)
    ) {
        // *** FIX START: Use correct Screen object names for composable routes ***
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
            arguments = listOf(navArgument("medicationId") { type = NavType.IntType })
        ) { backStackEntry ->
            val medicationId = backStackEntry.arguments?.getInt("medicationId")
            if (medicationId != null) {
                EditMedicationScreen(
                    navController = navController,
                    medicationId = medicationId // 將 ID 傳給 Screen
                )
            } else {
                Text("錯誤：找不到藥品 ID") // 或導航回上一頁
                LaunchedEffect(Unit) { navController.popBackStack() }
            }
        }
    }
}