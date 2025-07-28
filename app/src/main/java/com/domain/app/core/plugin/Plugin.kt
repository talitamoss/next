package com.domain.app.core.plugin

import android.content.Context
import com.domain.app.core.data.DataPoint

/**
 * Base plugin interface with enhanced metadata and future-ready capabilities
 */
interface Plugin {
    val id: String
    val metadata: PluginMetadata
    
    // Core functionality
    suspend fun initialize(context: Context)
    fun supportsManualEntry(): Boolean = false
    suspend fun createManualEntry(data: Map<String, Any>): DataPoint? = null
    fun getQuickAddConfig(): QuickAddConfig? = null
    
    // Multi-stage input support
    fun getQuickAddStages(): List<QuickAddStage>? = null
    
    // Validation
    fun validateDataPoint(data: Map<String, Any>): ValidationResult = ValidationResult.Success
    
    // Export/Import
    fun exportHeaders(): List<String> = listOf("timestamp", "value")
    fun formatForExport(dataPoint: DataPoint): Map<String, String> = mapOf(
        "timestamp" to dataPoint.timestamp.toString(),
        "value" to dataPoint.value.toString()
    )
    
    // Cleanup
    suspend fun cleanup() {}
}

/**
 * Enhanced plugin metadata with all foundational fields
 */
data class PluginMetadata(
    // Basic info
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    
    // Categorization
    val category: PluginCategory = PluginCategory.OTHER,
    val tags: List<String> = emptyList(),
    
    // Data characteristics
    val dataPattern: DataPattern = DataPattern.SINGLE_VALUE,
    val inputType: InputType = InputType.NUMBER,
    
    // New foundational fields
    val supportsMultiStage: Boolean = false,
    val relatedPlugins: List<String> = emptyList(),
    val exportFormat: ExportFormat = ExportFormat.CSV,
    val dataSensitivity: DataSensitivity = DataSensitivity.NORMAL,
    val naturalLanguageAliases: List<String> = emptyList(),
    
    // Optional fields
    val iconResource: Int? = null,
    val permissions: List<String> = emptyList(),
    val contextualTriggers: List<ContextTrigger> = emptyList()
)

/**
 * Data collection patterns
 */
enum class DataPattern {
    SINGLE_VALUE,      // One value at a time (mood, weight)
    CUMULATIVE,        // Values add up (water, calories)
    DURATION,          // Time-based (sleep, exercise)
    OCCURRENCE,        // Yes/No events (medication, habits)
    RATING,            // Scale ratings (pain, energy)
    TEXT,              // Text entries (journal, notes)
    COMPOSITE          // Multiple data types
}

enum class PluginCategory {
    HEALTH,
    MENTAL_WELLNESS,
    PRODUCTIVITY,
    LIFESTYLE,
    JOURNAL,
    OTHER
}

enum class InputType {
    NUMBER,
    TEXT,
    CHOICE,
    SLIDER,
    DURATION,
    SCALE,
    TIME_PICKER,
    DATE_PICKER
}

enum class ExportFormat {
    CSV, JSON, XML, CUSTOM
}

enum class DataSensitivity {
    PUBLIC,    // Can be shared freely
    NORMAL,    // Standard privacy
    SENSITIVE, // Health data
    PRIVATE    // Never share
}

enum class ContextTrigger {
    TIME_OF_DAY,      // Suggest at certain times
    LOCATION,         // Suggest at locations
    AFTER_EVENT,      // After another data entry
    PATTERN_BASED,    // Based on patterns
    MANUAL_ONLY       // Never auto-suggest
}

/**
 * Configuration for quick add
 */
data class QuickAddConfig(
    val title: String,
    val defaultValue: Any? = null,
    val inputType: InputType = InputType.NUMBER,
    val options: List<QuickOption>? = null,
    val unit: String? = null
)

data class QuickOption(
    val label: String,
    val value: Any,
    val icon: String? = null
)

/**
 * Multi-stage quick add support
 */
data class QuickAddStage(
    val id: String,
    val title: String,
    val inputType: InputType,
    val required: Boolean = true,
    val options: List<QuickOption>? = null,
    val validation: ((Any?) -> ValidationResult)? = null,
    val hint: String? = null,
    val defaultValue: Any? = null
)

/**
 * Validation results
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
    data class Warning(val message: String) : ValidationResult()
}
