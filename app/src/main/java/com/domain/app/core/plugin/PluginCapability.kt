package com.domain.app.core.plugin

/**
 * Capabilities that plugins can request.
 * Each capability grants specific permissions within the app.
 */
enum class PluginCapability {
    // Data capabilities
    COLLECT_DATA,         // Can create new data entries
    READ_OWN_DATA,        // Can read data it created
    READ_ALL_DATA,        // Can read any data (high privilege)
    MODIFY_DATA,          // Can update existing data
    DELETE_DATA,          // Can delete data
    
    // UI capabilities
    CUSTOM_UI,            // Can provide custom UI components
    MODIFY_THEME,         // Can modify app appearance
    ADD_MENU_ITEMS,       // Can add items to menus
    SHOW_NOTIFICATIONS,   // Can display notifications
    FULLSCREEN_UI,        // Can take over full screen
    
    // System capabilities
    BACKGROUND_SYNC,      // Can sync in background
    BACKGROUND_PROCESS,   // Can run background tasks
    NETWORK_ACCESS,       // Can access network
    FILE_ACCESS,          // Can read/write files
    CAMERA_ACCESS,        // Can access camera
    MICROPHONE_ACCESS,    // Can access microphone
    
    // Integration capabilities
    SHARE_DATA,           // Can share data externally
    IMPORT_DATA,          // Can import external data
    EXPORT_DATA,          // Can export data
    INTEGRATE_SERVICES,   // Can integrate with external services
    
    // Advanced capabilities
    ACCESS_SENSORS,       // Can access device sensors
    ACCESS_LOCATION,      // Can access location
    ACCESS_BIOMETRIC,     // Can access biometric data
    MODIFY_SETTINGS,      // Can change app settings
    INSTALL_PLUGINS,      // Can install other plugins
    
    // Analytics capabilities
    ANALYTICS_BASIC,      // Can track basic usage
    ANALYTICS_DETAILED,   // Can track detailed behavior
    
    // Communication capabilities
    SEND_EMAILS,          // Can send emails
    SEND_SMS,             // Can send SMS
    PUSH_NOTIFICATIONS,   // Can send push notifications
    
    // Storage capabilities
    LOCAL_STORAGE,        // Can use local storage
    CLOUD_STORAGE,        // Can use cloud storage
    CACHE_DATA,           // Can cache data locally
}

/**
 * Risk levels associated with capabilities
 */
fun PluginCapability.getRiskLevel(): RiskLevel {
    return when (this) {
        // Low risk - basic functionality
        COLLECT_DATA,
        READ_OWN_DATA,
        CUSTOM_UI,
        LOCAL_STORAGE,
        CACHE_DATA -> RiskLevel.LOW
        
        // Medium risk - extended functionality
        SHOW_NOTIFICATIONS,
        ADD_MENU_ITEMS,
        BACKGROUND_SYNC,
        NETWORK_ACCESS,
        SHARE_DATA,
        ANALYTICS_BASIC -> RiskLevel.MEDIUM
        
        // High risk - sensitive access
        READ_ALL_DATA,
        MODIFY_DATA,
        DELETE_DATA,
        FILE_ACCESS,
        ACCESS_LOCATION,
        EXPORT_DATA,
        IMPORT_DATA,
        INTEGRATE_SERVICES,
        ANALYTICS_DETAILED -> RiskLevel.HIGH
        
        // Critical risk - full access
        MODIFY_SETTINGS,
        INSTALL_PLUGINS,
        ACCESS_BIOMETRIC,
        ACCESS_SENSORS,
        CAMERA_ACCESS,
        MICROPHONE_ACCESS,
        SEND_EMAILS,
        SEND_SMS,
        CLOUD_STORAGE,
        MODIFY_THEME,
        FULLSCREEN_UI,
        BACKGROUND_PROCESS,
        PUSH_NOTIFICATIONS -> RiskLevel.CRITICAL
    }
}

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

/**
 * Human-readable descriptions for capabilities
 */
fun PluginCapability.getDescription(): String {
    return when (this) {
        COLLECT_DATA -> "Create and save new data entries"
        READ_OWN_DATA -> "View data this plugin has created"
        READ_ALL_DATA -> "View all data from any plugin"
        MODIFY_DATA -> "Edit existing data entries"
        DELETE_DATA -> "Remove data permanently"
        
        CUSTOM_UI -> "Display custom interface elements"
        MODIFY_THEME -> "Change app colors and appearance"
        ADD_MENU_ITEMS -> "Add options to app menus"
        SHOW_NOTIFICATIONS -> "Display system notifications"
        FULLSCREEN_UI -> "Take control of entire screen"
        
        BACKGROUND_SYNC -> "Sync data when app is closed"
        BACKGROUND_PROCESS -> "Run tasks in background"
        NETWORK_ACCESS -> "Connect to the internet"
        FILE_ACCESS -> "Read and write files on device"
        CAMERA_ACCESS -> "Use device camera"
        MICROPHONE_ACCESS -> "Use device microphone"
        
        SHARE_DATA -> "Share data with other apps"
        IMPORT_DATA -> "Import data from external sources"
        EXPORT_DATA -> "Export data to files"
        INTEGRATE_SERVICES -> "Connect to external services"
        
        ACCESS_SENSORS -> "Use device sensors (accelerometer, etc.)"
        ACCESS_LOCATION -> "Access device location"
        ACCESS_BIOMETRIC -> "Access health and biometric data"
        MODIFY_SETTINGS -> "Change app settings"
        INSTALL_PLUGINS -> "Install additional plugins"
        
        ANALYTICS_BASIC -> "Track basic usage patterns"
        ANALYTICS_DETAILED -> "Track detailed user behavior"
        
        SEND_EMAILS -> "Send emails on your behalf"
        SEND_SMS -> "Send text messages"
        PUSH_NOTIFICATIONS -> "Send push notifications"
        
        LOCAL_STORAGE -> "Store data on device"
        CLOUD_STORAGE -> "Store data in cloud"
        CACHE_DATA -> "Cache temporary data"
    }
}

/**
 * Required Android permissions for capabilities
 */
fun PluginCapability.getRequiredAndroidPermissions(): List<String> {
    return when (this) {
        CAMERA_ACCESS -> listOf("android.permission.CAMERA")
        MICROPHONE_ACCESS -> listOf("android.permission.RECORD_AUDIO")
        ACCESS_LOCATION -> listOf(
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION"
        )
        NETWORK_ACCESS -> listOf("android.permission.INTERNET")
        FILE_ACCESS -> listOf(
            "android.permission.READ_EXTERNAL_STORAGE",
            "android.permission.WRITE_EXTERNAL_STORAGE"
        )
        SEND_SMS -> listOf("android.permission.SEND_SMS")
        SHOW_NOTIFICATIONS -> listOf("android.permission.POST_NOTIFICATIONS")
        else -> emptyList()
    }
}
