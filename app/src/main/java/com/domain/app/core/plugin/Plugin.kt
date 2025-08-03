package com.domain.app.core.plugin

import androidx.annotation.DrawableRes
import kotlinx.coroutines.flow.Flow

/**
 * Core plugin interface - simplified without UI components
 * Focuses on data collection and management
 * 
 * File location: app/src/main/java/com/domain/app/core/plugin/Plugin.kt
 */
interface Plugin {
    /**
     * Unique identifier for the plugin
     */
    val id: String
    
    /**
     * Plugin metadata including name, description, version, and icon
     */
    val metadata: PluginMetadata
    
    /**
     * Capabilities required by this plugin
     */
    val requiredCapabilities: Set<PluginCapability>
    
    /**
     * Whether this plugin supports automatic data collection
     */
    val supportsAutomaticCollection: Boolean
    
    /**
     * Initialize the plugin
     */
    suspend fun initialize()
    
    /**
     * Clean up plugin resources
     */
    suspend fun cleanup()
    
    /**
     * Collect data (if supported)
     */
    suspend fun collectData(): PluginData?
    
    /**
     * Get plugin configuration parameters
     */
    fun getConfiguration(): PluginConfiguration
    
    /**
     * Update plugin configuration
     */
    suspend fun updateConfiguration(config: Map<String, Any>)
    
    /**
     * Get summary statistics for dashboard display
     */
    suspend fun getSummaryData(): PluginSummary
    
    /**
     * Export plugin data in various formats
     */
    suspend fun exportData(format: ExportFormat): ByteArray
    
    /**
     * Import data from external source
     */
    suspend fun importData(data: ByteArray, format: ExportFormat): Boolean
    
    /**
     * Observe plugin events
     */
    fun observeEvents(): Flow<PluginEvent>
}

/**
 * Plugin metadata
 */
data class PluginMetadata(
    val name: String,
    val description: String,
    val version: String,
    val author: String,
    val website: String? = null,
    @DrawableRes val iconResource: Int? = null,
    val category: PluginCategory = PluginCategory.OTHER,
    val tags: List<String> = emptyList()
)

/**
 * Plugin categories for organization
 */
enum class PluginCategory {
    HEALTH,
    FITNESS,
    MOOD,
    PRODUCTIVITY,
    FINANCE,
    SOCIAL,
    ENTERTAINMENT,
    EDUCATION,
    OTHER
}

/**
 * Base plugin data structure
 */
interface PluginData {
    val timestamp: Long
    val pluginId: String
    val dataType: String
    val data: Map<String, Any>
}

/**
 * Plugin configuration structure
 */
data class PluginConfiguration(
    val parameters: List<ConfigParameter>
)

/**
 * Configuration parameter definition
 */
sealed class ConfigParameter {
    abstract val key: String
    abstract val label: String
    abstract val defaultValue: Any?
    
    data class BooleanParam(
        override val key: String,
        override val label: String,
        override val defaultValue: Boolean = false
    ) : ConfigParameter()
    
    data class IntParam(
        override val key: String,
        override val label: String,
        override val defaultValue: Int = 0,
        val min: Int? = null,
        val max: Int? = null
    ) : ConfigParameter()
    
    data class StringParam(
        override val key: String,
        override val label: String,
        override val defaultValue: String = "",
        val options: List<String>? = null
    ) : ConfigParameter()
}

/**
 * Plugin summary data for dashboard display
 */
data class PluginSummary(
    val primaryValue: String,
    val primaryLabel: String,
    val secondaryValue: String? = null,
    val secondaryLabel: String? = null,
    val trend: Trend? = null,
    val lastUpdated: Long
)

/**
 * Trend indicator
 */
enum class Trend {
    UP,
    DOWN,
    STABLE
}

/**
 * Supported export formats
 */
enum class ExportFormat {
    JSON,
    CSV,
    FHIR,
    OPEN_MHEALTH,
    CUSTOM
}

/**
 * Plugin events
 */
sealed class PluginEvent {
    data class DataCollected(val data: PluginData) : PluginEvent()
    data class ConfigurationChanged(val key: String, val value: Any?) : PluginEvent()
    data class Error(val message: String, val throwable: Throwable? = null) : PluginEvent()
    data class StatusChanged(val status: PluginStatus) : PluginEvent()
}

/**
 * Plugin status
 */
enum class PluginStatus {
    INACTIVE,
    INITIALIZING,
    ACTIVE,
    COLLECTING,
    ERROR,
    DISABLED
}

/**
 * Abstract base class for plugins
 * Provides default implementations for common functionality
 */
abstract class BasePlugin : Plugin {
    override val supportsAutomaticCollection: Boolean = false
    
    override suspend fun collectData(): PluginData? = null
    
    override fun getConfiguration(): PluginConfiguration = PluginConfiguration(emptyList())
    
    override suspend fun updateConfiguration(config: Map<String, Any>) {
        // Default: no-op
    }
    
    override suspend fun getSummaryData(): PluginSummary {
        return PluginSummary(
            primaryValue = "No data",
            primaryLabel = metadata.name,
            lastUpdated = System.currentTimeMillis()
        )
    }
    
    override suspend fun exportData(format: ExportFormat): ByteArray {
        return when (format) {
            ExportFormat.JSON -> exportAsJson()
            ExportFormat.CSV -> exportAsCsv()
            else -> throw UnsupportedOperationException("Export format $format not supported")
        }
    }
    
    override suspend fun importData(data: ByteArray, format: ExportFormat): Boolean {
        return false // Default: no import support
    }
    
    protected abstract suspend fun exportAsJson(): ByteArray
    
    protected abstract suspend fun exportAsCsv(): ByteArray
}
