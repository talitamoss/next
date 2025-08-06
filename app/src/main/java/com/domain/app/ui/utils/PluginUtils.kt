package com.domain.app.ui.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.domain.app.core.plugin.Plugin
import com.domain.app.ui.theme.AppIcons

/**
 * Get the appropriate icon for a plugin
 */
fun getPluginIcon(plugin: Plugin): ImageVector {
    return when (plugin.id) {
        "water" -> AppIcons.Plugin.water
        "mood" -> AppIcons.Plugin.mood
        "sleep" -> AppIcons.Plugin.sleep
        "exercise" -> AppIcons.Plugin.exercise
        "energy" -> AppIcons.Plugin.energy
        "counter" -> AppIcons.Plugin.counter
        "location" -> AppIcons.Plugin.location
        else -> AppIcons.Plugin.custom
    }
}

/**
 * Extension properties for plugin capabilities
 */
val Plugin.notification: Boolean
    get() = false // Placeholder - implement based on your needs

val Plugin.storage: Boolean
    get() = false // Placeholder - implement based on your needs
