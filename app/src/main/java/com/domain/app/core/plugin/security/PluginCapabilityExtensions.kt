// app/src/main/java/com/domain/app/core/plugin/security/PluginCapabilityExtensions.kt
package com.domain.app.core.plugin.security

import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.RiskLevel

/**
 * Extension functions for PluginCapability
 * This file provides additional utility extensions that complement the core definitions
 * Note: Core getDescription() and getRiskLevel() are defined in PluginCapability.kt
 */

/**
 * Get icon for a capability (using Material Icons references)
 * This is specific to the security module's UI needs
 */
fun PluginCapability.getIcon(): String = when (this) {
    PluginCapability.READ_OWN_DATA -> "visibility"
    PluginCapability.READ_ALL_DATA -> "visibility_off"
    PluginCapability.MODIFY_DATA -> "edit"
    PluginCapability.DELETE_DATA -> "delete"
    PluginCapability.COLLECT_DATA -> "add_circle"
    
    PluginCapability.NETWORK_ACCESS -> "wifi"
    PluginCapability.CAMERA_ACCESS -> "camera_alt"
    PluginCapability.MICROPHONE_ACCESS -> "mic"
    PluginCapability.ACCESS_LOCATION -> "location_on"
    PluginCapability.ACCESS_SENSORS -> "sensors"
    PluginCapability.ACCESS_BIOMETRIC -> "fingerprint"
    
    PluginCapability.SHOW_NOTIFICATIONS -> "notifications"
    PluginCapability.SYSTEM_NOTIFICATIONS -> "notification_important"
    PluginCapability.SCHEDULE_NOTIFICATIONS -> "schedule"
    PluginCapability.PUSH_NOTIFICATIONS -> "notifications_active"
    
    PluginCapability.LOCAL_STORAGE -> "storage"
    PluginCapability.EXTERNAL_STORAGE -> "sd_storage"
    PluginCapability.CLOUD_STORAGE -> "cloud"
    PluginCapability.CLOUD_SYNC -> "sync"
    PluginCapability.CACHE_DATA -> "cached"
    
    PluginCapability.EXPORT_DATA -> "file_download"
    PluginCapability.IMPORT_DATA -> "file_upload"
    PluginCapability.SHARE_DATA -> "share"
    PluginCapability.INTEGRATE_SERVICES -> "hub"
    
    PluginCapability.ANALYTICS_BASIC -> "analytics"
    PluginCapability.ANALYTICS_DETAILED -> "query_stats"
    
    PluginCapability.BACKGROUND_SYNC -> "sync"
    PluginCapability.BACKGROUND_PROCESS -> "schedule"
    PluginCapability.BACKGROUND_PROCESSING -> "schedule"
    
    PluginCapability.CUSTOM_UI -> "dashboard_customize"
    PluginCapability.MODIFY_THEME -> "palette"
    PluginCapability.ADD_MENU_ITEMS -> "menu"
    PluginCapability.FULLSCREEN_UI -> "fullscreen"
    
    PluginCapability.MODIFY_SETTINGS -> "settings"
    PluginCapability.INSTALL_PLUGINS -> "extension"
    
    PluginCapability.SEND_EMAILS -> "email"
    PluginCapability.SEND_SMS -> "sms"
    
    PluginCapability.FILE_ACCESS -> "folder_open"
}

/**
 * Check if capability requires explicit user consent
 * These are capabilities that access sensitive resources
 */
fun PluginCapability.requiresConsent(): Boolean = when (this) {
    PluginCapability.CAMERA_ACCESS,
    PluginCapability.MICROPHONE_ACCESS,
    PluginCapability.ACCESS_LOCATION,
    PluginCapability.ACCESS_BIOMETRIC,
    PluginCapability.ACCESS_SENSORS,
    PluginCapability.DELETE_DATA,
    PluginCapability.MODIFY_DATA,
    PluginCapability.READ_ALL_DATA,
    PluginCapability.NETWORK_ACCESS,
    PluginCapability.EXTERNAL_STORAGE,
    PluginCapability.CLOUD_STORAGE,
    PluginCapability.CLOUD_SYNC,
    PluginCapability.FILE_ACCESS,
    PluginCapability.SEND_EMAILS,
    PluginCapability.SEND_SMS,
    PluginCapability.SYSTEM_NOTIFICATIONS,
    PluginCapability.PUSH_NOTIFICATIONS,
    PluginCapability.MODIFY_SETTINGS,
    PluginCapability.INSTALL_PLUGINS -> true
    else -> false
}

/**
 * Get color for risk level (as hex string for compatibility)
 * This extends the RiskLevel enum with UI-specific color coding
 */
fun RiskLevel.getColor(): String = when (this) {
    RiskLevel.LOW -> "#4CAF50"      // Green
    RiskLevel.MEDIUM -> "#FF9800"    // Orange
    RiskLevel.HIGH -> "#F44336"      // Red
    RiskLevel.CRITICAL -> "#9C27B0"  // Purple
    RiskLevel.UNKNOWN -> "#9E9E9E"   // Grey
}

/**
 * Get display name for risk level
 * Provides user-friendly names for risk levels
 */
fun RiskLevel.getDisplayName(): String = when (this) {
    RiskLevel.LOW -> "Low Risk"
    RiskLevel.MEDIUM -> "Medium Risk"
    RiskLevel.HIGH -> "High Risk"
    RiskLevel.CRITICAL -> "Critical Risk"
    RiskLevel.UNKNOWN -> "Unknown Risk"
}

/**
 * Check if capability is related to data operations
 */
fun PluginCapability.isDataOperation(): Boolean = when (this) {
    PluginCapability.COLLECT_DATA,
    PluginCapability.READ_OWN_DATA,
    PluginCapability.READ_ALL_DATA,
    PluginCapability.MODIFY_DATA,
    PluginCapability.DELETE_DATA,
    PluginCapability.EXPORT_DATA,
    PluginCapability.IMPORT_DATA,
    PluginCapability.SHARE_DATA -> true
    else -> false
}

/**
 * Check if capability involves background execution
 */
fun PluginCapability.isBackgroundCapability(): Boolean = when (this) {
    PluginCapability.BACKGROUND_SYNC,
    PluginCapability.BACKGROUND_PROCESS,
    PluginCapability.BACKGROUND_PROCESSING -> true
    else -> false
}

/**
 * Check if capability involves external communication
 */
fun PluginCapability.isExternalCommunication(): Boolean = when (this) {
    PluginCapability.NETWORK_ACCESS,
    PluginCapability.SEND_EMAILS,
    PluginCapability.SEND_SMS,
    PluginCapability.PUSH_NOTIFICATIONS,
    PluginCapability.CLOUD_STORAGE,
    PluginCapability.CLOUD_SYNC,
    PluginCapability.INTEGRATE_SERVICES -> true
    else -> false
}

/**
 * Get a short warning message for high-risk capabilities
 */
fun PluginCapability.getWarningMessage(): String? = when (this) {
    PluginCapability.DELETE_DATA -> "This plugin can permanently delete your data"
    PluginCapability.READ_ALL_DATA -> "This plugin can access all your app data"
    PluginCapability.MODIFY_SETTINGS -> "This plugin can change your app settings"
    PluginCapability.INSTALL_PLUGINS -> "This plugin can install other plugins"
    PluginCapability.ACCESS_LOCATION -> "This plugin can track your location"
    PluginCapability.ACCESS_BIOMETRIC -> "This plugin can access biometric data"
    PluginCapability.CAMERA_ACCESS -> "This plugin can use your camera"
    PluginCapability.MICROPHONE_ACCESS -> "This plugin can use your microphone"
    PluginCapability.SEND_EMAILS -> "This plugin can send emails on your behalf"
    PluginCapability.SEND_SMS -> "This plugin can send SMS messages"
    PluginCapability.FILE_ACCESS -> "This plugin can access your files"
    else -> null
}
