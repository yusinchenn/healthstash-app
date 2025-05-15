package com.example.healthstash.ui.navigation

/**
 * Sealed class representing the different screens in the application.
 * Each object corresponds to a unique route.
 */
sealed class Screen(val route: String) {
    /**
     * Represents the main screen of the application where the list of medications is displayed.
     * Route: "main_screen"
     */
    object MainScreen : Screen("main_screen")

    /**
     * Represents the screen used for adding a new medication or health supplement.
     * Route: "add_medication_screen"
     */
    object AddMedicationScreen : Screen("add_medication_screen")

    /**
     * Represents the screen that displays the medication usage log.
     * Route: "log_screen"
     */
    object LogScreen : Screen("log_screen")

    object EditMedicationScreen : Screen("edit_medication_screen/{medicationId}") {
        fun createRoute(medicationId: Int): String = "edit_medication_screen/$medicationId"
    }
}