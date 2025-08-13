// app/src/main/java/com/domain/app/core/plugin/Plugin.kt
package com.domain.app.core.plugin

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult

/**
 * Core plugin interface that all plugins must implement
 */
interface Plugin {
    val id: String
    val metadata: PluginMetadata
    val securityManifest: PluginSecurityManifest
    val trustLevel: PluginTrustLevel
    
    // Core lifecycle methods
    suspend fun initialize(context: Context)
    suspend fun onEnable(context: Context) {}
    suspend fun onDisable(context: Context) {}
    
    // Cleanup method - REQUIRED by PluginManager
    suspend fun cleanup() {}
    
    // Data validation
    fun validateDataPoint(data: Map<String, Any>): ValidationResult
    
    // Manual entry support
    fun supportsManualEntry(): Boolean = false
    suspend fun createManualEntry(data: Map<String, Any>): DataPoint? = null
    
    // Quick add configuration
    fun getQuickAddConfig(): QuickAddConfig? = null
    fun getQuickAddStages(): List<QuickAddStage>? = null
    
    // Export functionality
    fun exportHeaders(): List<String> = listOf("Date", "Time", "Value")
    fun formatForExport(dataPoint: DataPoint): Map<String, String>
    
    // Permission rationale
    fun getPermissionRationale(): Map<PluginCapability, String> = emptyMap()
}

/**
 * Plugin metadata
 */
data class PluginMetadata(
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val category: PluginCategory,
    val tags: List<String> = emptyList(),
    val dataPattern: DataPattern,
    val inputType: InputType,
    val supportsMultiStage: Boolean = false,
    val relatedPlugins: List<String> = emptyList(),
    val exportFormat: ExportFormat = ExportFormat.CSV,
    val dataSensitivity: DataSensitivity = DataSensitivity.NORMAL,
    val naturalLanguageAliases: List<String> = emptyList(),
    val minVersion: Int = 1,
    val maxDataPointsPerDay: Int? = null,
    val permissions: List<String> = emptyList(),
    val contextualTriggers: List<ContextTrigger> = emptyList()
)

/**
 * Plugin categories for organization
 */
enum class PluginCategory {
    HEALTH,
    MENTAL_WELLNESS,
    PRODUCTIVITY,
    LIFESTYLE,
    JOURNAL,
    OTHER
}

/**
 * Data collection patterns
 */
enum class DataPattern {
    SINGLE_VALUE,
    CUMULATIVE,
    DURATION,
    OCCURRENCE,
    RATING,
    TEXT,
    COMPOSITE
}

/**
 * Input types for data collection
 * Each type maps directly to a specific UI component in the component repository
 */
enum class InputType {
    // Original input types
    NUMBER,                    // Numeric input field
    TEXT,                      // Text input field
    CHOICE,                    // Single choice from options
    @Deprecated("Use VERTICAL_SLIDER or HORIZONTAL_SLIDER for specific orientation")
    SLIDER,                    // Generic slider (kept for backward compatibility)
    DURATION,                  // Duration picker
    SCALE,                     // Rating scale
    TIME_PICKER,               // Time picker (original)
    DATE_PICKER,               // Date picker (original)
    
    // New slider types for better control
    VERTICAL_SLIDER,           // Vertical slider for ratings/scales (NEW)
    HORIZONTAL_SLIDER,         // Horizontal slider for quantities/amounts (NEW)
    
    // Additional types needed by QuickAddDialog
    BOOLEAN,                   // Toggle/checkbox (NEW - needed by dialog)
    DATE,                      // Date selection (NEW - needed by dialog) 
    TIME,                      // Time selection (NEW - needed by dialog)
    
    // Future input types for extensibility
    COLOR_PICKER,              // Color selection (future)
    LOCATION_PICKER,           // Location selection (future)
    IMAGE_PICKER,              // Image selection (future)
    MULTI_CHOICE               // Multiple choice selection (future)
}

/**
 * Export format options
 */
enum class ExportFormat {
    CSV, 
    JSON, 
    XML, 
    CUSTOM
}

/**
 * Data sensitivity levels for privacy classification
 */
enum class DataSensitivity {
    PUBLIC,
    NORMAL,
    SENSITIVE,
    PRIVATE,
    REGULATED
}

/**
 * Context triggers for intelligent suggestions
 */
enum class ContextTrigger {
    TIME_OF_DAY,
    LOCATION,
    AFTER_EVENT,
    PATTERN_BASED,
    MANUAL_ONLY
}

/**
 * Configuration for quick add functionality
 */
data class QuickAddConfig(
    val title: String,
    val defaultValue: Any? = null,
    val inputType: InputType = InputType.NUMBER,
    val options: List<QuickOption>? = null,
    val unit: String? = null,
    val id: String = "value",
    val min: Number? = null,
    val max: Number? = null,
    val step: Number? = null,
    val stages: List<QuickAddStage>? = null,
    val presets: List<QuickOption>? = null,
    val placeholder: String? = null
    // NOTE: No validation field here - PluginGenerator has its own ValidationRule
)

/**
 * Quick add option
 */
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
    val description: String? = null,
    val placeholder: String? = null,
    val defaultValue: Any? = null,
    val min: Number? = null,
    val max: Number? = null,
    val step: Number? = null,
    val unit: String? = null
)

// NOTE: ValidationRule and ValidationRuleType are NOT defined here
// They exist in PluginGenerator.kt with a different structure:
// data class ValidationRule(
//     val type: String,  // Not an enum
//     val field: String,
//     val message: String,
//     val min: Int = 0,
//     val max: Int = 100
// )

/**
 * Risk warning for permissions
 * RiskLevel enum is defined in PluginCapability.kt
 */
data class RiskWarning(
    val severity: RiskLevel,
    val message: String,
    val capability: PluginCapability
)
