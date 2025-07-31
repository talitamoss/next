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

// Extension function for getting description



fun PluginCapability.getDescription(): String = when(this) {
    PluginCapability.COLLECT_DATA -> "Collect and record data"
    PluginCapability.READ_OWN_DATA -> "Read own data"
    PluginCapability.READ_ALL_DATA -> "Read all data"
    PluginCapability.MODIFY_DATA -> "Modify existing data"
    PluginCapability.DELETE_DATA -> "Delete data"
    PluginCapability.NETWORK_ACCESS -> "Access network"
    PluginCapability.CUSTOM_UI -> "Provide custom UI"
    PluginCapability.MODIFY_THEME -> "Modify app theme"
    PluginCapability.EXPORT_DATA -> "Export data"
    PluginCapability.IMPORT_DATA -> "Import data"
    else -> "Permission: ${this.name}"
}
