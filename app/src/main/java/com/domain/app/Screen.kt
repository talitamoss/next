// app/src/main/java/com/domain/app/Screen.kt
package com.domain.app

import androidx.compose.ui.graphics.vector.ImageVector
import com.domain.app.ui.theme.AppIcons

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Collect : Screen(
        route = "collect",
        title = "Collect",
        icon = AppIcons.Navigation.dashboard
    )
    
    object Reflect : Screen(
        route = "reflect",
        title = "Reflect",
        icon = AppIcons.Navigation.data
    )
}
