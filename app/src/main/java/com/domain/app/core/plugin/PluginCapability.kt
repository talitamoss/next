package com.domain.app.core.plugin

/**
 * Plugin capability definitions and extensions
 * Defines all permissions that plugins can request from the system
 * 
 * File location: app/src/main/java/com/domain/app/core/plugin/PluginCapability.kt
 */

/**
 * Capabilities that plugins can request.
 * Each capability represents a specific permission or access level.
 */
enum class PluginCapability {
    // Data capabilities
    COLLECT_DATA,           // Can collect and record new data
    READ_OWN_DATA,         // Can read data created by this plugin
    READ_ALL_DATA,         // Can read all data from any plugin
    MODIFY_DATA,           // Can modify existing data
    DELETE_DATA,           // Can delete data permanently
    
    // UI capabilities
    CUSTOM_UI,             // Can display custom user interface
    MODIFY_THEME,          // Can change app appearance
    ADD_MENU_ITEMS,        // Can add items to app menus
    SHOW_NOTIFICATIONS,    // Can show notifications
    FULLSCREEN_UI,         // Can use fullscreen display
    
    // System capabilities
    BACKGROUND_SYNC,       // Can sync data in background
    BACKGROUND_PROCESS,    // Can run background processes
    NETWORK_ACCESS,        // Can access internet
    FILE_ACCESS,           // Can access device files
    CAMERA_ACCESS,         // Can use camera
    MICROPHONE_ACCESS,     // Can use microphone
    
    // Integration capabilities
    SHARE_DATA,            // Can share data with other apps
    IMPORT_DATA,           // Can import data from files
    EXPORT_DATA,           // Can export data to files
    INTEGRATE_SERVICES,    // Can connect to external services
    
    // Advanced capabilities
    ACCESS_SENSORS,        // Can access device sensors
    ACCESS_LOCATION,       // Can access location
    ACCESS_BIOMETRIC,      // Can access health sensors
    MODIFY_SETTINGS,       // Can change app settings
    INSTALL_PLUGINS,       // Can install other plugins
    
    // Analytics capabilities
    ANALYTICS_BASIC,       // Basic data analysis
    ANALYTICS_DETAILED,    // Advanced analytics
    
    // Communication capabilities
    SEND_EMAILS,           // Can send emails
    SEND_SMS,              // Can send text messages
    PUSH_NOTIFICATIONS,    // Can send push notifications
    
    // Storage capabilities
    LOCAL_STORAGE,         // Can store data locally
    CLOUD_STORAGE,         // Can store data in cloud
    CACHE_DATA            // Can cache temporary data
}

/**
 * Extension functions for PluginCapability
 */

/**
 * Get a human-readable description for a capability
 */
fun PluginCapability.getDescription(): String {
    return when (this) {
        // Data capabilities
        PluginCapability.COLLECT_DATA -> "Collect and record data"
        PluginCapability.READ_OWN_DATA -> "Read data created by this plugin"
        PluginCapability.READ_ALL_DATA -> "Read all data from any plugin"
        PluginCapability.MODIFY_DATA -> "Modify existing data"
        PluginCapability.DELETE_DATA -> "Delete data permanently"
        
        // UI capabilities
        PluginCapability.CUSTOM_UI -> "Display custom user interface"
        PluginCapability.MODIFY_THEME -> "Change app appearance"
        PluginCapability.ADD_MENU_ITEMS -> "Add items to app menus"
        PluginCapability.SHOW_NOTIFICATIONS -> "Show notifications"
        PluginCapability.FULLSCREEN_UI -> "Use fullscreen display"
        
        // System capabilities
        PluginCapability.BACKGROUND_SYNC -> "Sync data in background"
        PluginCapability.BACKGROUND_PROCESS -> "Run background processes"
        PluginCapability.NETWORK_ACCESS -> "Access internet"
        PluginCapability.FILE_ACCESS -> "Access device files"
        PluginCapability.CAMERA_ACCESS -> "Use camera"
        PluginCapability.MICROPHONE_ACCESS -> "Use microphone"
        
        // Integration capabilities
        PluginCapability.SHARE_DATA -> "Share data with other apps"
        PluginCapability.IMPORT_DATA -> "Import data from files"
        PluginCapability.EXPORT_DATA -> "Export data to files"
        PluginCapability.INTEGRATE_SERVICES -> "Connect to external services"
        
        // Advanced capabilities
        PluginCapability.ACCESS_SENSORS -> "Access device sensors"
        PluginCapability.ACCESS_LOCATION -> "Access location"
        PluginCapability.ACCESS_BIOMETRIC -> "Access health sensors"
        PluginCapability.MODIFY_SETTINGS -> "Change app settings"
        PluginCapability.INSTALL_PLUGINS -> "Install other plugins"
        
        // Analytics capabilities
        PluginCapability.ANALYTICS_BASIC -> "Basic data analysis"
        PluginCapability.ANALYTICS_DETAILED -> "Advanced analytics"
        
        // Communication capabilities
        PluginCapability.SEND_EMAILS -> "Send emails"
        PluginCapability.SEND_SMS -> "Send text messages"
        PluginCapability.PUSH_NOTIFICATIONS -> "Send push notifications"
        
        // Storage capabilities
        PluginCapability.LOCAL_STORAGE -> "Store data locally"
        PluginCapability.CLOUD_STORAGE -> "Store data in cloud"
        PluginCapability.CACHE_DATA -> "Cache temporary data"
    }
}

/**
 * Get the risk level associated with a capability
 */
fun PluginCapability.getRiskLevel(): RiskLevel {
    return when (this) {
        PluginCapability.READ_OWN_DATA,
        PluginCapability.LOCAL_STORAGE,
        PluginCapability.CACHE_DATA,
        PluginCapability.CUSTOM_UI,
        PluginCapability.ANALYTICS_BASIC -> RiskLevel.LOW
        
        PluginCapability.COLLECT_DATA,
        PluginCapability.MODIFY_DATA,
        PluginCapability.SHOW_NOTIFICATIONS,
        PluginCapability.BACKGROUND_SYNC,
        PluginCapability.IMPORT_DATA,
        PluginCapability.EXPORT_DATA,
        PluginCapability.ACCESS_SENSORS,
        PluginCapability.ANALYTICS_DETAILED -> RiskLevel.MEDIUM
        
        PluginCapability.READ_ALL_DATA,
        PluginCapability.DELETE_DATA,
        PluginCapability.NETWORK_ACCESS,
        PluginCapability.FILE_ACCESS,
        PluginCapability.CAMERA_ACCESS,
        PluginCapability.MICROPHONE_ACCESS,
        PluginCapability.SHARE_DATA,
        PluginCapability.INTEGRATE_SERVICES,
        PluginCapability.ACCESS_LOCATION,
        PluginCapability.ACCESS_BIOMETRIC,
        PluginCapability.MODIFY_SETTINGS,
        PluginCapability.INSTALL_PLUGINS,
        PluginCapability.SEND_EMAILS,
        PluginCapability.SEND_SMS,
        PluginCapability.PUSH_NOTIFICATIONS,
        PluginCapability.CLOUD_STORAGE,
        PluginCapability.BACKGROUND_PROCESS,
        PluginCapability.MODIFY_THEME,
        PluginCapability.ADD_MENU_ITEMS,
        PluginCapability.FULLSCREEN_UI -> RiskLevel.HIGH
    }
}

/**
 * Risk levels for plugin capabilities
 */
enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

/**
 * Check if a capability requires explicit user consent
 */
fun PluginCapability.requiresExplicitConsent(): Boolean {
    return getRiskLevel() == RiskLevel.HIGH
}

/**
 * Get a detailed explanation of what this capability allows
 */
fun PluginCapability.getDetailedExplanation(): String {
    return when (this) {
        PluginCapability.COLLECT_DATA -> "This plugin can collect and store new behavioral data entries. All data remains encrypted on your device."
        PluginCapability.READ_OWN_DATA -> "This plugin can only read data that it has created. It cannot access data from other plugins."
        PluginCapability.READ_ALL_DATA -> "This plugin can read all data stored in the app, including data from other plugins. Use caution when granting this permission."
        PluginCapability.NETWORK_ACCESS -> "This plugin can connect to the internet. Your data remains on your device unless you explicitly choose to share it."
        PluginCapability.ACCESS_LOCATION -> "This plugin can access your device's location. Location data is stored locally and encrypted."
        PluginCapability.SHARE_DATA -> "This plugin can share data with other apps or services. You control what data is shared and when."
        else -> getDescription()
    }
}
