// app/src/main/java/com/domain/app/ui/utils/PluginUtils.kt
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
        "caffeine" -> AppIcons.Plugin.energy
        "alcohol" -> AppIcons.Plugin.alcohol
        "screen_time" -> AppIcons.Plugin.screenTime
        "social" -> AppIcons.Plugin.social  // ADDED: Social plugin icon mapping
        "counter" -> AppIcons.Plugin.counter
        "location" -> AppIcons.Plugin.location
        "health" -> AppIcons.Plugin.health
        "productivity" -> AppIcons.Plugin.productivity
        "food" -> AppIcons.Plugin.food
	"meditation" -> AppIcons.Plugin.meditation
	"journal" -> AppIcons.Plugin.journal
	"medical" -> AppIcons.Plugin.medication
	"poo" -> AppIcons.Plugin.poo
	"audio" -> AppIcons.Plugin.audio
        else -> AppIcons.Plugin.custom
    }
}

/**
 * Get color for plugin based on its ID or category
 */
fun getPluginColor(plugin: Plugin): String {
    return when (plugin.id) {
        "water" -> "#2196F3"      // Blue
        "mood" -> "#9C27B0"       // Purple
        "sleep" -> "#3F51B5"      // Indigo
        "movement" -> "#4CAF50"   // Green
        "work" -> "#FF9800"       // Orange
        "caffeine" -> "#795548"   // Brown
        "alcohol" -> "#F44336"    // Red
        "screen_time" -> "#00BCD4" // Cyan
        "social" -> "#7B68EE"     // Medium Purple - ADDED
        "counter" -> "#607D8B"    // Blue Grey
        "location" -> "#009688"   // Teal
        "health" -> "#E91E63"     // Pink
        "productivity" -> "#FF9800" // Orange
        "food" -> "#8BC34A"       // Light Green
	"medication" -> "#4CAF50" // Green
	"poo" -> "#8D6E63"        // Brown and Sticky
        else -> "#9E9E9E"         // Grey
    }
}

/**
 * Get plugin display priority (for dashboard ordering)
 */
fun getPluginPriority(plugin: Plugin): Int {
    return when (plugin.id) {
        "mood" -> 1
        "water" -> 2
        "sleep" -> 3
        "movement" -> 4
        "social" -> 5        // ADDED: High priority for social plugin
        "work" -> 6
        "caffeine" -> 7
        "alcohol" -> 8
        "screen_time" -> 9
        "food" -> 10
        "health" -> 11
        "productivity" -> 12
        "location" -> 13
        "counter" -> 14
	"poo" -> 15
        else -> 99
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
