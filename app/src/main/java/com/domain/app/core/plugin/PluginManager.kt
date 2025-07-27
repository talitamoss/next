package com.domain.app.core.plugin

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.toEntity
import com.domain.app.core.event.Event
import com.domain.app.core.event.EventBus
import com.domain.app.core.storage.AppDatabase
import com.domain.app.core.storage.entity.PluginStateEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginManager @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val pluginRegistry: PluginRegistry
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activePlugins = mutableMapOf<String, Plugin>()
    private val pluginJobs = mutableMapOf<String, Job>()
    
    private val _pluginStates = MutableStateFlow<Map<String, PluginState>>(emptyMap())
    val pluginStates: StateFlow<Map<String, PluginState>> = _pluginStates.asStateFlow()
    
    init {
        // Monitor plugin state changes from database
        scope.launch {
            database.pluginStateDao().getAllStates()
                .map { states ->
                    states.associate { entity ->
                        entity.pluginId to PluginState(
                            pluginId = entity.pluginId,
                            isEnabled = entity.isEnabled,
                            isCollecting = entity.isCollecting,
                            lastCollection = entity.lastCollection,
                            errorCount = entity.errorCount
                        )
                    }
                }
                .collect { states ->
                    _pluginStates.value = states
                }
        }
    }
    
    /**
     * Initialize all registered plugins
     */
    suspend fun initializePlugins() = withContext(Dispatchers.IO) {
        val registeredPlugins = pluginRegistry.getAllPlugins()
        
        registeredPlugins.forEach { plugin ->
            try {
                // Initialize plugin
                plugin.initialize(context)
                
                // Create or update plugin state in database
                val existingState = database.pluginStateDao().getState(plugin.id)
                if (existingState == null) {
                    database.pluginStateDao().insertOrUpdate(
                        PluginStateEntity(
                            pluginId = plugin.id,
                            isEnabled = true,
                            isCollecting = false
                        )
                    )
                }
                
                // Add to active plugins
                activePlugins[plugin.id] = plugin
                
                // Start monitoring plugin data
                monitorPluginData(plugin)
                
            } catch (e: Exception) {
                handlePluginError(plugin.id, e)
            }
        }
    }
    
    /**
     * Enable a plugin and start data collection
     */
    suspend fun enablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId] ?: return@withContext
        
        try {
            plugin.startCollection()
            
            database.pluginStateDao().updateCollectingState(pluginId, true)
            database.pluginStateDao().clearErrors(pluginId)
            
            EventBus.emit(Event.PluginStateChanged(pluginId, true))
            
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Disable a plugin and stop data collection
     */
    suspend fun disablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId] ?: return@withContext
        
        try {
            plugin.stopCollection()
            
            database.pluginStateDao().updateCollectingState(pluginId, false)
            
            EventBus.emit(Event.PluginStateChanged(pluginId, false))
            
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Get a specific plugin instance
     */
    fun getPlugin(pluginId: String): Plugin? {
        return activePlugins[pluginId]
    }
    
    /**
     * Get all active plugins
     */
    fun getAllActivePlugins(): List<Plugin> {
        return activePlugins.values.toList()
    }
    
    /**
     * Get plugins by category
     */
    fun getPluginsByCategory(category: PluginCategory): List<Plugin> {
        return activePlugins.values.filter { 
            it.metadata.category == category 
        }
    }
    
    /**
     * Create manual data entry for a plugin
     */
    suspend fun createManualEntry(
        pluginId: String, 
        data: Map<String, Any>
    ): DataPoint? = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId] ?: return@withContext null
        
        if (!plugin.supportsManualEntry()) {
            return@withContext null
        }
        
        try {
            return@withContext plugin.createManualEntry(data)
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
            return@withContext null
        }
    }
    
    /**
     * Update plugin configuration
     */
    suspend fun updatePluginConfiguration(
        pluginId: String,
        configuration: Map<String, Any>
    ) = withContext(Dispatchers.IO) {
        try {
            val configJson = configuration.toString() // In production, use proper JSON serialization
            database.pluginStateDao().updateConfiguration(pluginId, configJson)
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Clean up all plugins
     */
    suspend fun cleanup() {
        pluginJobs.values.forEach { it.cancel() }
        activePlugins.values.forEach { plugin ->
            try {
                plugin.cleanup()
            } catch (e: Exception) {
                // Log error but continue cleanup
            }
        }
        scope.cancel()
    }
    
    private fun monitorPluginData(plugin: Plugin) {
        pluginJobs[plugin.id]?.cancel()
        
        pluginJobs[plugin.id] = scope.launch {
            plugin.dataFlow()
                .catch { e -> handlePluginError(plugin.id, e) }
                .collect { dataPoint ->
                    try {
                        // Save to database
                        val entity = dataPoint.toEntity()
                        database.dataPointDao().insert(entity)
                        
                        // Emit event
                        EventBus.emit(Event.DataCollected(dataPoint))
                        
                        // Update last collection time
                        database.pluginStateDao().insertOrUpdate(
                            database.pluginStateDao().getState(plugin.id)?.copy(
                                lastCollection = Instant.now()
                            ) ?: PluginStateEntity(
                                pluginId = plugin.id,
                                lastCollection = Instant.now()
                            )
                        )
                    } catch (e: Exception) {
                        handlePluginError(plugin.id, e)
                    }
                }
        }
    }
    
    private suspend fun handlePluginError(pluginId: String, error: Throwable) {
        database.pluginStateDao().recordError(
            pluginId, 
            error.message ?: "Unknown error"
        )
        // In production, add proper logging
    }
}

data class PluginState(
    val pluginId: String,
    val isEnabled: Boolean,
    val isCollecting: Boolean,
    val lastCollection: Instant?,
    val errorCount: Int
)
