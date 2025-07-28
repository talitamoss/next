package com.domain.app.core.plugin

import android.content.Context
import com.domain.app.core.data.DataPoint

/**
 * Minimal plugin interface for MVP
 * Future features will be added through extension interfaces
 */
interface Plugin {
    /**
     * Unique identifier for this plugin
     */
    val id: String
    
    /**
     * Plugin metadata (name, description, icon, etc.)
     */
    val metadata: PluginMetadata
    
    /**
     * Initialize the plugin with application context
     */
    suspend fun initialize(context: Context)
    
    /**
     * Does this plugin support manual data entry?
     */
    fun supportsManualEntry(): Boolean = false
    
    /**
     * Create a manual data point (for quick add functionality)
     */
    suspend fun createManualEntry(data: Map<String, Any>): DataPoint? = null
    
    /**
     * Get quick add configuration for UI
     */
    fun getQuickAddConfig(): QuickAddConfig? = null
    
    /**
     * Clean up resources
     */
    suspend fun cleanup() {}
}

/**
 * Configuration for quick add UI
 */
data class QuickAddConfig(
    val title: String,
    val defaultValue: Any? = null,
    val inputType: InputType = InputType.NUMBER,
    val options: List<QuickOption>? = null,
    val unit: String? = null
)

enum class InputType {
    NUMBER,
    TEXT,
    CHOICE,
    SLIDER
}

data class QuickOption(
    val label: String,
    val value: Any,
    val icon: String? = null
)
