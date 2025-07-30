// app/src/main/java/com/domain/app/Screen.kt
package com.domain.app

import androidx.compose.ui.graphics.vector.ImageVector
import com.domain.app.ui.theme.AppIcons

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        icon = AppIcons.Navigation.dashboard
    )
    
    object Data : Screen(
        route = "data",
        title = "Data",
        icon = AppIcons.Navigation.data
    )
    
    object Social : Screen(
        route = "social",
        title = "Social",
        icon = AppIcons.Plugin.health // TODO: Add dedicated social icon to AppIcons
    )
    
    object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = AppIcons.Navigation.settings
    )
}
