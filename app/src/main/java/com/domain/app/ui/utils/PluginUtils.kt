package com.domain.app.ui.utils

import androidx.compose.ui.graphics.vector.ImageVector
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.ui.theme.AppIcons

/**
 * Utility functions for plugin UI operations
 */

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
        "movement" -> AppIcons.Plugin.exercise
	"work" -> AppIcons.Plugin.productivity
        "energy" -> AppIcons.Plugin.energy
        "counter" -> AppIcons.Plugin.counter
        "location" -> AppIcons.Plugin.location
        "health" -> AppIcons.Plugin.health
        "productivity" -> AppIcons.Plugin.productivity
        "food" -> AppIcons.Plugin.food
        else -> AppIcons.Plugin.custom
    }
}

/**
 * Extension functions for Plugin type-safe capability checking
 */
fun Plugin.hasCapability(capability: PluginCapability): Boolean {
    return this.securityManifest.requestedCapabilities.contains(capability)
}

fun Plugin.hasAnyCapability(capabilities: Set<PluginCapability>): Boolean {
    return this.securityManifest.requestedCapabilities.any { it in capabilities }
}

fun Plugin.hasAllCapabilities(capabilities: Set<PluginCapability>): Boolean {
    return this.securityManifest.requestedCapabilities.containsAll(capabilities)
}

fun Plugin.hasNotificationCapability(): Boolean {
    return this.securityManifest.requestedCapabilities.any {
        it == PluginCapability.SHOW_NOTIFICATIONS || 
        it == PluginCapability.SYSTEM_NOTIFICATIONS ||
        it == PluginCapability.SCHEDULE_NOTIFICATIONS
    }
}

fun Plugin.hasStorageCapability(): Boolean {
    return this.securityManifest.requestedCapabilities.any {
        it == PluginCapability.LOCAL_STORAGE || 
        it == PluginCapability.EXTERNAL_STORAGE ||
        it == PluginCapability.CLOUD_SYNC
    }
}

fun Plugin.hasDataAccessCapability(): Boolean {
    return this.securityManifest.requestedCapabilities.any {
        it == PluginCapability.READ_OWN_DATA ||
        it == PluginCapability.READ_ALL_DATA ||
        it == PluginCapability.COLLECT_DATA
    }
}

/**
 * Backward compatibility - deprecated extensions
 * These will be removed in a future version
 */
@Deprecated(
    message = "Use hasNotificationCapability() instead",
    replaceWith = ReplaceWith("hasNotificationCapability()")
)
val Plugin.notification: Boolean
    get() = hasNotificationCapability()

@Deprecated(
    message = "Use hasStorageCapability() instead", 
    replaceWith = ReplaceWith("hasStorageCapability()")
)
val Plugin.storage: Boolean
    get() = hasStorageCapability()
