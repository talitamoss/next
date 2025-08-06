// app/src/main/java/com/domain/app/ui/utils/PluginUtils.kt
package com.domain.app.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.ui.theme.AppIcons

/**
 * Get the appropriate icon for a plugin object
 */
fun getPluginIcon(plugin: Plugin): ImageVector {
    return getPluginIconById(plugin.id)
}

/**
 * Get the appropriate icon for a plugin by its ID
 */
fun getPluginIconById(pluginId: String): ImageVector {
    return when (pluginId) {
        "water" -> AppIcons.Plugin.water
        "mood" -> AppIcons.Plugin.mood
        "sleep" -> AppIcons.Plugin.sleep
        "exercise" -> AppIcons.Plugin.exercise
        "energy" -> AppIcons.Plugin.energy
        "counter" -> AppIcons.Plugin.counter
        "location" -> AppIcons.Plugin.location
        "health" -> AppIcons.Plugin.health
        "productivity" -> AppIcons.Plugin.productivity
        else -> AppIcons.Plugin.custom
    }
}

/**
 * Extension properties for Plugin - placeholder implementations
 * These are used in some legacy code but should be migrated to PluginCapability checks
 */
val Plugin.notification: Boolean
    get() = this.securityManifest.requestedCapabilities.contains(PluginCapability.SHOW_NOTIFICATIONS)

val Plugin.storage: Boolean
    get() = this.securityManifest.requestedCapabilities.contains(PluginCapability.LOCAL_STORAGE) ||
            this.securityManifest.requestedCapabilities.contains(PluginCapability.EXTERNAL_STORAGE)

/**
 * Extension properties for PluginCapability
 * Used in permission dialogs to check capability types
 */
val PluginCapability.notification: Boolean
    get() = this == PluginCapability.SHOW_NOTIFICATIONS || 
            this == PluginCapability.SYSTEM_NOTIFICATIONS

val PluginCapability.storage: Boolean
    get() = this == PluginCapability.LOCAL_STORAGE || 
            this == PluginCapability.EXTERNAL_STORAGE ||
            this == PluginCapability.CLOUD_SYNC
