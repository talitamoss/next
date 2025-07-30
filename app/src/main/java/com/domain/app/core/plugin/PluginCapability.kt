package com.domain.app.core.plugin

/**
 * Defines granular permissions that plugins can request.
 * Each capability grants specific access within the application.
 */
enum class PluginCapability {
    // Data capabilities
    COLLECT_DATA,
    READ_OWN_DATA,
    READ_ALL_DATA,
    MODIFY_DATA,
    DELETE_DATA,
    
    // UI capabilities
    CUSTOM_UI,
    MODIFY_THEME,
    ADD_MENU_ITEMS,
    SHOW_NOTIFICATIONS,
    FULLSCREEN_UI,
    
    // System capabilities
    BACKGROUND_SYNC,
    BACKGROUND_PROCESS,
    NETWORK_ACCESS,
    FILE_ACCESS,
    CAMERA_ACCESS,
    MICROPHONE_ACCESS,
    
    // Integration capabilities
    SHARE_DATA,
    IMPORT_DATA,
    EXPORT_DATA,
    INTEGRATE_SERVICES,
    
    // Advanced capabilities
    ACCESS_SENSORS,
    ACCESS_LOCATION,
    ACCESS_BIOMETRIC,
    MODIFY_SETTINGS,
    INSTALL_PLUGINS,
    
    // Analytics capabilities
    ANALYTICS_BASIC,
    ANALYTICS_DETAILED,
    
    // Communication capabilities
    SEND_EMAILS,
    SEND_SMS,
    PUSH_NOTIFICATIONS,
    
    // Storage capabilities
    LOCAL_STORAGE,
    CLOUD_STORAGE,
    CACHE_DATA
}

/**
 * Risk levels associated with different capabilities
 */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Extension function to get the risk level for a capability
 */
fun PluginCapability.getRiskLevel(): RiskLevel {
    return when (this) {
        PluginCapability.COLLECT_DATA,
        PluginCapability.READ_OWN_DATA,
        PluginCapability.CUSTOM_UI,
        PluginCapability.LOCAL_STORAGE,
        PluginCapability.CACHE_DATA -> RiskLevel.LOW
        
        PluginCapability.SHOW_NOTIFICATIONS,
        PluginCapability.ADD_MENU_ITEMS,
        PluginCapability.BACKGROUND_SYNC,
        PluginCapability.NETWORK_ACCESS,
        PluginCapability.SHARE_DATA,
        PluginCapability.ANALYTICS_BASIC -> RiskLevel.MEDIUM
        
        PluginCapability.READ_ALL_DATA,
        PluginCapability.MODIFY_DATA,
        PluginCapability.DELETE_DATA,
        PluginCapability.FILE_ACCESS,
        PluginCapability.ACCESS_LOCATION,
        PluginCapability.EXPORT_DATA,
        PluginCapability.IMPORT_DATA,
        PluginCapability.INTEGRATE_SERVICES,
        PluginCapability.ANALYTICS_DETAILED -> RiskLevel.HIGH
        
        PluginCapability.MODIFY_SETTINGS,
        PluginCapability.INSTALL_PLUGINS,
        PluginCapability.ACCESS_BIOMETRIC,
        PluginCapability.ACCESS_SENSORS,
        PluginCapability.CAMERA_ACCESS,
        PluginCapability.MICROPHONE_ACCESS,
        PluginCapability.SEND_EMAILS,
        PluginCapability.SEND_SMS,
        PluginCapability.CLOUD_STORAGE,
        PluginCapability.MODIFY_THEME,
        PluginCapability.FULLSCREEN_UI,
        PluginCapability.BACKGROUND_PROCESS,
        PluginCapability.PUSH_NOTIFICATIONS -> RiskLevel.CRITICAL
    }
}

/**
 * Extension function to get human-readable description for a capability
 */
fun PluginCapability.getDescription(): String {
    return when (this) {
        PluginCapability.COLLECT_DATA -> "Create and save new data entries"
        PluginCapability.READ_OWN_DATA -> "View data this plugin has created"
        PluginCapability.READ_ALL_DATA -> "View all data from any plugin"
        PluginCapability.MODIFY_DATA -> "Edit existing data entries"
        PluginCapability.DELETE_DATA -> "Remove data permanently"
        
        PluginCapability.CUSTOM_UI -> "Display custom interface elements"
        PluginCapability.MODIFY_THEME -> "Change app colors and appearance"
        PluginCapability.ADD_MENU_ITEMS -> "Add options to app menus"
        PluginCapability.SHOW_NOTIFICATIONS -> "Display system notifications"
        PluginCapability.FULLSCREEN_UI -> "Take control of entire screen"
        
        PluginCapability.BACKGROUND_SYNC -> "Sync data when app is closed"
        PluginCapability.BACKGROUND_PROCESS -> "Run tasks in background"
        PluginCapability.NETWORK_ACCESS -> "Connect to the internet"
        PluginCapability.FILE_ACCESS -> "Read and write files on device"
        PluginCapability.CAMERA_ACCESS -> "Use device camera"
        PluginCapability.MICROPHONE_ACCESS -> "Use device microphone"
        
        PluginCapability.SHARE_DATA -> "Share data with other apps"
        PluginCapability.IMPORT_DATA -> "Import data from external sources"
        PluginCapability.EXPORT_DATA -> "Export data to files"
        PluginCapability.INTEGRATE_SERVICES -> "Connect to external services"
        
        PluginCapability.ACCESS_SENSORS -> "Use device sensors (accelerometer, etc.)"
        PluginCapability.ACCESS_LOCATION -> "Access device location"
        PluginCapability.ACCESS_BIOMETRIC -> "Access health and biometric data"
        PluginCapability.MODIFY_SETTINGS -> "Change app settings"
        PluginCapability.INSTALL_PLUGINS -> "Install additional plugins"
        
        PluginCapability.ANALYTICS_BASIC -> "Track basic usage patterns"
        PluginCapability.ANALYTICS_DETAILED -> "Track detailed user behavior"
        
        PluginCapability.SEND_EMAILS -> "Send emails on your behalf"
        PluginCapability.SEND_SMS -> "Send text messages"
        PluginCapability.PUSH_NOTIFICATIONS -> "Send push notifications"
        
        PluginCapability.LOCAL_STORAGE -> "Store data on device"
        PluginCapability.CLOUD_STORAGE -> "Store data in cloud"
        PluginCapability.CACHE_DATA -> "Cache temporary data"
    }
}

/**
 * Extension function to get required Android permissions for capabilities
 */
fun PluginCapability.getRequiredAndroidPermissions(): List<String> {
    return when (this) {
        PluginCapability.CAMERA_ACCESS -> listOf("android.permission.CAMERA")
        PluginCapability.MICROPHONE_ACCESS -> listOf("android.permission.RECORD_AUDIO")
        PluginCapability.ACCESS_LOCATION -> listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
        )
        PluginCapability.NETWORK_ACCESS -> listOf("android.permission.INTERNET")
        PluginCapability.FILE_ACCESS -> listOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )
        PluginCapability.SEND_SMS -> listOf("android.permission.SEND_SMS")
        PluginCapability.SHOW_NOTIFICATIONS -> listOf("android.permission.POST_NOTIFICATIONS")
        else -> emptyList()
    }
}
