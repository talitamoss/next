// app/src/main/java/com/domain/app/core/plugin/security/PluginCapabilityExtensions.kt
package com.domain.app.core.plugin.security

import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.RiskLevel

/**
 * Extension functions for PluginCapability
 * This file centralizes all capability-related extensions to avoid duplication
 */

/**
 * Get human-readable description for a capability
 */
fun PluginCapability.getDescription(): String = when (this) {
    PluginCapability.READ_DATA -> "Read and view your data"
    PluginCapability.WRITE_DATA -> "Create and modify data entries"
    PluginCapability.DELETE_DATA -> "Delete existing data"
    PluginCapability.NETWORK_ACCESS -> "Access network and internet"
    PluginCapability.CAMERA_ACCESS -> "Use device camera"
    PluginCapability.LOCATION_ACCESS -> "Access location services"
    PluginCapability.NOTIFICATION_ACCESS -> "Send notifications"
    PluginCapability.STORAGE_ACCESS -> "Access device storage"
    PluginCapability.SENSOR_ACCESS -> "Access device sensors"
    PluginCapability.EXPORT_DATA -> "Export data to files"
    PluginCapability.IMPORT_DATA -> "Import data from files"
    PluginCapability.SHARE_DATA -> "Share data with other apps"
    PluginCapability.SYNC_DATA -> "Sync data across devices"
    PluginCapability.ANALYTICS -> "Perform data analytics"
    PluginCapability.BACKGROUND_EXECUTION -> "Run in background"
}

/**
 * Get risk level for a capability
 */
fun PluginCapability.getRiskLevel(): RiskLevel = when (this) {
    PluginCapability.READ_DATA -> RiskLevel.LOW
    PluginCapability.WRITE_DATA -> RiskLevel.MEDIUM
    PluginCapability.DELETE_DATA -> RiskLevel.HIGH
    PluginCapability.NETWORK_ACCESS -> RiskLevel.MEDIUM
    PluginCapability.CAMERA_ACCESS -> RiskLevel.HIGH
    PluginCapability.LOCATION_ACCESS -> RiskLevel.HIGH
    PluginCapability.NOTIFICATION_ACCESS -> RiskLevel.LOW
    PluginCapability.STORAGE_ACCESS -> RiskLevel.MEDIUM
    PluginCapability.SENSOR_ACCESS -> RiskLevel.LOW
    PluginCapability.EXPORT_DATA -> RiskLevel.LOW
    PluginCapability.IMPORT_DATA -> RiskLevel.MEDIUM
    PluginCapability.SHARE_DATA -> RiskLevel.MEDIUM
    PluginCapability.SYNC_DATA -> RiskLevel.MEDIUM
    PluginCapability.ANALYTICS -> RiskLevel.LOW
    PluginCapability.BACKGROUND_EXECUTION -> RiskLevel.MEDIUM
}

/**
 * Get icon for a capability (using Material Icons references)
 */
fun PluginCapability.getIcon(): String = when (this) {
    PluginCapability.READ_DATA -> "visibility"
    PluginCapability.WRITE_DATA -> "edit"
    PluginCapability.DELETE_DATA -> "delete"
    PluginCapability.NETWORK_ACCESS -> "wifi"
    PluginCapability.CAMERA_ACCESS -> "camera_alt"
    PluginCapability.LOCATION_ACCESS -> "location_on"
    PluginCapability.NOTIFICATION_ACCESS -> "notifications"
    PluginCapability.STORAGE_ACCESS -> "storage"
    PluginCapability.SENSOR_ACCESS -> "sensors"
    PluginCapability.EXPORT_DATA -> "file_download"
    PluginCapability.IMPORT_DATA -> "file_upload"
    PluginCapability.SHARE_DATA -> "share"
    PluginCapability.SYNC_DATA -> "sync"
    PluginCapability.ANALYTICS -> "analytics"
    PluginCapability.BACKGROUND_EXECUTION -> "schedule"
}

/**
 * Check if capability requires user consent
 */
fun PluginCapability.requiresConsent(): Boolean = when (this) {
    PluginCapability.CAMERA_ACCESS,
    PluginCapability.LOCATION_ACCESS,
    PluginCapability.DELETE_DATA,
    PluginCapability.NETWORK_ACCESS,
    PluginCapability.STORAGE_ACCESS -> true
    else -> false
}

/**
 * Get color for risk level (as hex string for compatibility)
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
 */
fun RiskLevel.getDisplayName(): String = when (this) {
    RiskLevel.LOW -> "Low Risk"
    RiskLevel.MEDIUM -> "Medium Risk"
    RiskLevel.HIGH -> "High Risk"
    RiskLevel.CRITICAL -> "Critical Risk"
    RiskLevel.UNKNOWN -> "Unknown Risk"
}
