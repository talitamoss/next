package com.domain.app

/**
 * Screen definitions for navigation
 * 
 * File location: app/src/main/java/com/domain/app/Screen.kt
 */
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Data : Screen("data")
    object Social : Screen("social")
    object Settings : Screen("settings")
}
