plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Applies the Jetpack Compose compiler plugin
    alias(libs.plugins.ksp)          // Apply KSP for Room
}

android {
    namespace = "com.example.healthstash"
    compileSdk = 35 // Consider using the latest stable SDK, e.g., 34, instead of a preview (35)

    defaultConfig {
        applicationId = "com.example.healthstash"
        minSdk = 24
        targetSdk = 35 // Match compileSdk or use a recent stable SDK
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // 核心功能：Android 核心套件與 Jetpack Compose 基礎
    implementation(libs.androidx.core.ktx) // Kotlin 擴充功能，提升開發效率
    implementation(libs.androidx.lifecycle.runtime.ktx) // Lifecycle 支援 Kotlin 協程
    implementation(libs.androidx.activity.compose) // Activity 與 Compose 整合
    implementation(platform(libs.androidx.compose.bom)) // Compose BOM，統一 Compose 套件版本
    implementation(libs.androidx.compose.ui) // Compose 核心 UI 套件（包含 Modifier、Layout 等）
    implementation(libs.androidx.compose.ui.util) // 提供附加 UI 功能（如 draw 等）
    implementation(libs.androidx.ui.graphics) // 繪圖與顏色相關功能
    implementation(libs.androidx.ui.tooling.preview) // 預覽支援（@Preview）

    // Material Design 元件與圖示
    implementation(libs.androidx.material3) // Material 3 UI 元件
    implementation(libs.androidx.material.icons.extended) // Material Icons 圖示套件

    // 圖片載入
    implementation(libs.coil.compose) // 使用 Coil 載入圖片，支援 Compose

    //架構支援：ViewModel 與 Navigation
    implementation(libs.androidx.lifecycle.viewmodel.compose) // ViewModel 與 Compose 整合
    implementation(libs.androidx.navigation.compose) // Jetpack Navigation for Compose

    // 本地儲存：Room 資料庫
    implementation(libs.androidx.room.runtime) // Room 資料庫核心
    ksp(libs.androidx.room.compiler) // Room 編譯器（使用 KSP 進行註解處理）
    implementation(libs.androidx.room.ktx) // Room 與 Kotlin 協程整合支援

    // 非同步處理：Kotlin 協程
    implementation(libs.kotlinx.coroutines.core) // Kotlin 協程核心功能
    implementation(libs.kotlinx.coroutines.android) // Android 專用的協程支援

    // 權限處理：Accompanist 套件
    implementation(libs.accompanist.permissions) // Accompanist 權限處理（適用於 Compose）

    // 測試相關
    testImplementation(libs.junit) // 單元測試（JVM）
    androidTestImplementation(libs.androidx.junit) // Android JUnit 擴充
    androidTestImplementation(libs.androidx.espresso.core) // UI 測試 - Espresso
    androidTestImplementation(platform(libs.androidx.compose.bom)) // 確保 Compose 測試版本一致
    androidTestImplementation(libs.androidx.ui.test.junit4) // Compose 測試支援 JUnit4
    debugImplementation(libs.androidx.ui.tooling) // Compose Tooling（可在預覽畫面上顯示 Debug 資訊）
    debugImplementation(libs.androidx.ui.test.manifest) // 用於測試時模擬 AndroidManifest
}
