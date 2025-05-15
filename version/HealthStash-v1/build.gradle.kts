// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // kotlin.compose plugin is often applied at the module level or managed by AGP.
    // If libs.plugins.kotlin.compose refers to the Compose Compiler plugin, it's fine.
    alias(libs.plugins.kotlin.compose) apply false // Assuming this is for the Compose Compiler
    alias(libs.plugins.ksp) apply false // Add KSP plugin alias
}