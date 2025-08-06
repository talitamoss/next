package com.domain.app.ui.utils

import androidx.compose.ui.graphics.vector.ImageVector
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.ui.theme.AppIcons

/**
 * Single source of truth for capability icons
 * This replaces all duplicate getIconForCapability functions
 */
fun getIconForCapability(capability: PluginCapability): ImageVector {
    return when (capability) {
        // Data operations
        PluginCapability.COLLECT_DATA -> AppIcons.Action.add
        PluginCapability.READ_OWN_DATA -> AppIcons.Storage.folder
        PluginCapability.READ_ALL_DATA -> AppIcons.Storage.database
        PluginCapability.DELETE_DATA -> AppIcons.Action.delete
        PluginCapability.EXPORT_DATA -> AppIcons.Data.upload
        
        // Notifications
        PluginCapability.SHOW_NOTIFICATIONS -> AppIcons.Communication.notifications
        PluginCapability.SYSTEM_NOTIFICATIONS -> AppIcons.Communication.notifications
        PluginCapability.SCHEDULE_NOTIFICATIONS -> AppIcons.Data.calendar
        
        // Storage
        PluginCapability.LOCAL_STORAGE -> AppIcons.Storage.storage
        PluginCapability.EXTERNAL_STORAGE -> AppIcons.Storage.folder
        PluginCapability.CLOUD_SYNC -> AppIcons.Storage.cloud
        
        // System access
        PluginCapability.NETWORK_ACCESS -> AppIcons.Communication.cloud
        PluginCapability.ACCESS_LOCATION -> AppIcons.Plugin.location
        PluginCapability.MODIFY_SETTINGS -> AppIcons.Navigation.settings
        PluginCapability.BACKGROUND_PROCESSING -> AppIcons.Status.sync
        
        // Default
        else -> AppIcons.Plugin.custom
    }
}

/**
 * Get description for a capability
 */
fun PluginCapability.getDescription(): String {
    return when (this) {
        PluginCapability.COLLECT_DATA -> "Collect and save behavioral data"
        PluginCapability.READ_OWN_DATA -> "Read data collected by this plugin"
        PluginCapability.READ_ALL_DATA -> "Read data from all plugins"
        PluginCapability.DELETE_DATA -> "Delete existing data points"
        PluginCapability.EXPORT_DATA -> "Export data to external formats"
        PluginCapability.SHOW_NOTIFICATIONS -> "Show in-app notifications"
        PluginCapability.SYSTEM_NOTIFICATIONS -> "Show system notifications"
        PluginCapability.SCHEDULE_NOTIFICATIONS -> "Schedule future notifications"
        PluginCapability.LOCAL_STORAGE -> "Store data locally on device"
        PluginCapability.EXTERNAL_STORAGE -> "Access external storage"
        PluginCapability.CLOUD_SYNC -> "Sync data with cloud services"
        PluginCapability.NETWORK_ACCESS -> "Access network resources"
        PluginCapability.ACCESS_LOCATION -> "Access device location"
        PluginCapability.MODIFY_SETTINGS -> "Modify app settings"
        PluginCapability.BACKGROUND_PROCESSING -> "Run background tasks"
        else -> "Unknown capability"
    }
}

/**
 * Get risk level for a capability
 */
fun PluginCapability.getRiskLevel(): RiskLevel {
    return when (this) {
        PluginCapability.COLLECT_DATA,
        PluginCapability.READ_OWN_DATA,
        PluginCapability.SHOW_NOTIFICATIONS -> RiskLevel.LOW
        
        PluginCapability.LOCAL_STORAGE,
        PluginCapability.EXPORT_DATA,
        PluginCapability.SCHEDULE_NOTIFICATIONS,
        PluginCapability.MODIFY_SETTINGS -> RiskLevel.MEDIUM
        
        PluginCapability.READ_ALL_DATA,
        PluginCapability.DELETE_DATA,
        PluginCapability.SYSTEM_NOTIFICATIONS,
        PluginCapability.EXTERNAL_STORAGE,
        PluginCapability.CLOUD_SYNC,
        PluginCapability.NETWORK_ACCESS,
        PluginCapability.ACCESS_LOCATION,
        PluginCapability.BACKGROUND_PROCESSING -> RiskLevel.HIGH
        
        else -> RiskLevel.UNKNOWN
    }
}

enum class RiskLevel {
    LOW, MEDIUM, HIGH, UNKNOWN
}

/**
 * Extension properties for PluginCapability
 * These check capability categories
 */
val PluginCapability.isNotificationCapability: Boolean
    get() = this in setOf(
        PluginCapability.SHOW_NOTIFICATIONS,
        PluginCapability.SYSTEM_NOTIFICATIONS,
        PluginCapability.SCHEDULE_NOTIFICATIONS
    )

val PluginCapability.isStorageCapability: Boolean
    get() = this in setOf(
        PluginCapability.LOCAL_STORAGE,
        PluginCapability.EXTERNAL_STORAGE,
        PluginCapability.CLOUD_SYNC
    )

val PluginCapability.isDataCapability: Boolean
    get() = this in setOf(
        PluginCapability.COLLECT_DATA,
        PluginCapability.READ_OWN_DATA,
        PluginCapability.READ_ALL_DATA,
        PluginCapability.DELETE_DATA,
        PluginCapability.EXPORT_DATA
    )

/**
 * Backward compatibility - deprecated extensions
 */
@Deprecated(
    message = "Use isNotificationCapability instead",
    replaceWith = ReplaceWith("isNotificationCapability")
)
val PluginCapability.notification: Boolean
    get() = isNotificationCapability

@Deprecated(
    message = "Use isStorageCapability instead",
    replaceWith = ReplaceWith("isStorageCapability")
)
val PluginCapability.storage: Boolean
    get() = isStorageCapability
