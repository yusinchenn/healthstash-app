package com.example.healthstash // 確保這是您的主包名

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import com.example.healthstash.ui.navigation.HealthStashAppNavigation
import com.example.healthstash.ui.theme.HealthStashTheme // 確保您的主題名稱和路徑正確

/**
 * The root Composable function for the HealthStash application.
 *
 * This function sets up the overall theme and delegates the main UI structure
 * (including Scaffold and NavHost) to [HealthStashAppNavigation].
 */
@RequiresApi(Build.VERSION_CODES.S)
@Composable
fun HealthStashApp() {
    // Apply your custom Material 3 theme to the entire application.
    HealthStashTheme {
        // HealthStashAppNavigation is responsible for setting up the Scaffold
        // (which includes elements like the bottom navigation bar)
        // and the NavHost for navigating between different screens (Composable).
        HealthStashAppNavigation()
    }
}