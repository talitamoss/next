package com.domain.app.core.plugin

import android.content.Context
import com.domain.app.core.data.DataPoint
import kotlinx.coroutines.flow.Flow

/**
 * Base interface for all plugins.
 * Defines the contract that every data collection plugin must implement.
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
     * Start collecting data
     */
    suspend fun startCollection()
    
    /**
     * Stop collecting data
     */
    suspend fun stopCollection()
    
    /**
     * Get the current collection state
     */
    fun isCollecting(): Boolean
    
    /**
     * Flow of data points from this plugin
     */
    fun dataFlow(): Flow<DataPoint>
    
    /**
     * Manual data entry (optional)
     * @return true if plugin supports manual entry
     */
    fun supportsManualEntry(): Boolean = false
    
    /**
     * Create a manual data point (if supported)
     */
    suspend fun createManualEntry(data: Map<String, Any>): DataPoint? = null
    
    /**
     * Clean up resources
     */
    suspend fun cleanup()
}
