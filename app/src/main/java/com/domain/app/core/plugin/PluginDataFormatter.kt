// app/src/main/java/com/domain/app/core/plugin/PluginDataFormatter.kt
package com.domain.app.core.plugin

import com.domain.app.core.data.DataPoint

/**
 * Simple interface for plugins to format their data for display
 * Keeps it straightforward - just tell us what to show and how to label it
 */
interface PluginDataFormatter {
    
    /**
     * Get a one-line summary for collapsed view
     * Example: "Full Meal - Healthy, 2x portion"
     */
    fun formatSummary(dataPoint: DataPoint): String
    
    /**
     * Get formatted details for expanded view
     * Returns ordered list of label-value pairs to display
     * Example: [("Meal Type", "Full Meal"), ("Food", "Healthy"), ("Portion", "2x")]
     */
    fun formatDetails(dataPoint: DataPoint): List<DataField>
    
    /**
     * Fields that should never be shown to the user
     * Example: ["metadata", "version", "inputType"]
     */
    fun getHiddenFields(): List<String> = listOf("metadata", "timestamp", "source", "version", "inputType")
}

/**
 * Represents a formatted field for display
 */
data class DataField(
    val label: String,
    val value: String,
    val isImportant: Boolean = false,  // Show in bold or highlighted
    val isLongText: Boolean = false    // Display as multiline (for notes, journal entries)
)
