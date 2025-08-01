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
        // Low risk capabilities
        COLLECT_DATA,
        READ_OWN_DATA,
        LOCAL_STORAGE,
        CACHE_DATA,
        ANALYTICS_BASIC -> RiskLevel.LOW
        
        // Medium risk capabilities
        SHOW_NOTIFICATIONS,
        EXPORT_DATA,
        IMPORT_DATA,
        CUSTOM_UI,
        MODIFY_THEME,
        ADD_MENU_ITEMS,
        FULLSCREEN_UI,
        ANALYTICS_DETAILED -> RiskLevel.MEDIUM
        
        // High risk capabilities
        READ_ALL_DATA,
        MODIFY_DATA,
        DELETE_DATA,
        NETWORK_ACCESS,
        FILE_ACCESS,
        SHARE_DATA,
        INTEGRATE_SERVICES,
        ACCESS_SENSORS,
        ACCESS_LOCATION,
        ACCESS_BIOMETRIC,
        BACKGROUND_SYNC,
        BACKGROUND_PROCESS,
        SEND_EMAILS,
        SEND_SMS,
        PUSH_NOTIFICATIONS,
        CLOUD_STORAGE -> RiskLevel.HIGH
        
        // Critical risk capabilities
        CAMERA_ACCESS,
        MICROPHONE_ACCESS,
        MODIFY_SETTINGS,
        INSTALL_PLUGINS -> RiskLevel.CRITICAL
    }
}
