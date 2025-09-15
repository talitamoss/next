// app/src/main/java/com/domain/app/ui/settings/navigation/SettingsNavigation.kt
package com.domain.app.ui.settings.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.domain.app.ui.settings.*
import com.domain.app.ui.settings.sections.*

/**
 * Settings navigation routes
 */
sealed class SettingsRoute(val route: String) {
    object Main : SettingsRoute("settings_main")
    // Profile route removed
    object Aesthetic : SettingsRoute("settings_aesthetic")
    object Privacy : SettingsRoute("settings_privacy")
    object Security : SettingsRoute("settings_security")
    object DataManagement : SettingsRoute("settings_data")
    object Plugins : SettingsRoute("settings_plugins")
    object PluginSecurity : SettingsRoute("settings_plugin_security")
    object Notifications : SettingsRoute("settings_notifications")
    object About : SettingsRoute("settings_about")
    object Licenses : SettingsRoute("settings_licenses")
    
    // Plugin detail with argument
    object PluginDetail : SettingsRoute("settings_plugin/{pluginId}") {
        fun createRoute(pluginId: String) = "settings_plugin/$pluginId"
    }
}

@Composable
fun SettingsNavigation(
    onNavigateBack: () -> Unit
) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = SettingsRoute.Main.route
    ) {
        // Main Settings
    composable(SettingsRoute.Main.route) {
	SettingsScreen(
            navController = navController,
            onNavigateToAesthetic = { navController.navigate(SettingsRoute.Aesthetic.route) },
            onNavigateToPrivacy = { navController.navigate(SettingsRoute.Privacy.route) },
            onNavigateToSecurity = { navController.navigate(SettingsRoute.Security.route) },
            onNavigateToDataManagement = { navController.navigate(SettingsRoute.DataManagement.route) },
            onNavigateToPlugins = { navController.navigate(SettingsRoute.Plugins.route) },
            onNavigateToPluginSecurity = { navController.navigate(SettingsRoute.PluginSecurity.route) },
            onNavigateToNotifications = { navController.navigate(SettingsRoute.Notifications.route) },
            onNavigateToAbout = { navController.navigate(SettingsRoute.About.route) }
        )
    }        
        // Aesthetic
        composable(SettingsRoute.Aesthetic.route) {
            AestheticSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Privacy
        composable(SettingsRoute.Privacy.route) {
            PrivacySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Security
        composable(SettingsRoute.Security.route) {
            SecuritySettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Data Management
        composable(SettingsRoute.DataManagement.route) {
            DataManagementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Plugins
        composable(SettingsRoute.Plugins.route) {
            PluginsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPlugin = { plugin ->
                    navController.navigate(SettingsRoute.PluginDetail.createRoute(plugin.id))
                }
            )
        }
        
        // Plugin Security
        composable(SettingsRoute.PluginSecurity.route) {
            PluginSecurityScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Notifications
        composable(SettingsRoute.Notifications.route) {
            NotificationSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // About
        composable(SettingsRoute.About.route) {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Licenses
        composable(SettingsRoute.Licenses.route) {
            LicensesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Plugin Detail
        composable(SettingsRoute.PluginDetail.route) { backStackEntry ->
            val pluginId = backStackEntry.arguments?.getString("pluginId") ?: ""
            PluginDetailScreen(
                pluginId = pluginId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
