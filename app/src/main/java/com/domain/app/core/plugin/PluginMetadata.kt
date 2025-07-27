package com.domain.app.core.plugin

/**
 * Metadata describing a plugin
 */
data class PluginMetadata(
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val iconResource: Int? = null,
    val category: PluginCategory = PluginCategory.OTHER,
    val permissions: List<String> = emptyList()
)

enum class PluginCategory {
    HEALTH,
    LOCATION,
    ACTIVITY,
    PRODUCTIVITY,
    SOCIAL,
    OTHER
}
