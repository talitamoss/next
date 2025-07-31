package com.domain.app.core.plugin

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
