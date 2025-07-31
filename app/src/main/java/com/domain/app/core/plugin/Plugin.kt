package com.domain.app.core.plugin

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.security.PluginSecurityManifest
import com.domain.app.core.plugin.security.PluginTrustLevel

/**
 * Core plugin interface defining the contract for all behavioral data collection plugins.
 * Designed for extensibility while maintaining security and privacy.
 */
interface Plugin {
    val id: String
    val metadata: PluginMetadata
    
    // Security manifest - required for all plugins
    val securityManifest: PluginSecurityManifest
    
    // Trust level - determined by system
    val trustLevel: PluginTrustLevel
        get() = PluginTrustLevel.COMMUNITY
    
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
    
    // Permission rationale
    fun getPermissionRationale(): Map<PluginCapability, String> = emptyMap()
    
    // Cleanup
    suspend fun cleanup() {}
    val supportsAutomaticCollection: Boolean
}

/**
 * Plugin metadata containing descriptive and behavioral information
 */
data class PluginMetadata(
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val category: PluginCategory = PluginCategory.OTHER,
    val tags: List<String> = emptyList(),
    val dataPattern: DataPattern = DataPattern.SINGLE_VALUE,
    val inputType: InputType = InputType.NUMBER,
    val supportsMultiStage: Boolean = false,
    val relatedPlugins: List<String> = emptyList(),
    val exportFormat: ExportFormat = ExportFormat.CSV,
    val dataSensitivity: DataSensitivity = DataSensitivity.NORMAL,
    val naturalLanguageAliases: List<String> = emptyList(),
    val iconResource: Int? = null,
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
 */
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

/**
 * Export format options
 */
enum class ExportFormat {
    CSV, JSON, XML, CUSTOM
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
    val unit: String? = null
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

/**
 * Risk warning for permissions
 */
data class RiskWarning(
    val severity: RiskLevel,
    val message: String,
    val capability: PluginCapability
)

val supportsAutomaticCollection: Boolean get() = false


