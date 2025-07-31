package com.domain.app.core.plugin

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.event.Event
import com.domain.app.core.event.EventBus
import com.domain.app.core.plugin.security.*
import com.domain.app.core.storage.AppDatabase
import com.domain.app.core.storage.entity.PluginStateEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central manager for plugin lifecycle and operations.
 * Handles plugin initialization, state management, and secure data operations.
 */
@Singleton
class PluginManager @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val dataRepository: DataRepository,
    private val pluginRegistry: PluginRegistry,
    private val permissionManager: PluginPermissionManager,
    private val securityMonitor: SecurityMonitor
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activePlugins = mutableMapOf<String, Plugin>()
    
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
                
            } catch (e: Exception) {
                handlePluginError(plugin.id, e)
            }
        }
    }
    
    /**
     * Enable a plugin
     */
    suspend fun enablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId] ?: return@withContext
        
        try {
            database.pluginStateDao().updateEnabledState(pluginId, true)
            
            // Publish event  
            EventBus.post(Event.PluginEnabled(pluginId))
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Disable a plugin
     */
    suspend fun disablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        try {
            database.pluginStateDao().updateEnabledState(pluginId, false)
            
            // Stop any active collection
            database.pluginStateDao().updateCollectingState(pluginId, false)
            
            // Publish event
            EventBus.post(Event.PluginDisabled(pluginId))
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Get all active plugins
     */
    fun getAllActivePlugins(): List<Plugin> {
        return activePlugins.values.toList()
    }
    
    /**
     * Get plugin by ID
     */
    fun getPlugin(pluginId: String): Plugin? {
        return activePlugins[pluginId]
    }
    
    /**
     * Get plugin state
     */
    fun getPluginState(pluginId: String): PluginState? {
        return _pluginStates.value[pluginId]
    }
    
    /**
     * Check if plugin is enabled
     */
    fun isPluginEnabled(pluginId: String): Boolean {
        return _pluginStates.value[pluginId]?.isEnabled ?: false
    }
    
    /**
     * Get plugins with manual entry support
     */
    fun getManualEntryPlugins(): List<Plugin> {
        return activePlugins.values.filter { it.supportsManualEntry() }
    }
    
    /**
     * Create manual data entry
     */
    suspend fun createManualEntry(
        pluginId: String,
        data: Map<String, Any>
    ): DataPoint? = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId] ?: return@withContext null
        
        if (!plugin.supportsManualEntry()) {
            return@withContext null
        }
        
        // Check permissions
        if (!permissionManager.hasCapability(pluginId, PluginCapability.COLLECT_DATA)) {
            securityMonitor.recordSecurityEvent(
                SecurityEvent.PermissionDenied(
                    pluginId = pluginId,
                    capability = PluginCapability.COLLECT_DATA,
                    reason = "Plugin lacks COLLECT_DATA permission"
                )
            )
            return@withContext null
        }
        
        try {
            val dataPoint = plugin.createManualEntry(data)
            
            if (dataPoint != null) {
                // Save data point
                dataRepository.saveDataPoint(dataPoint)
                
                // Update last collection time
                database.pluginStateDao().updateCollectionTime(pluginId, Instant.now())
                
                return@withContext dataPoint
            }
            
            return@withContext null
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
     * Get secure data repository for a plugin
     */
    fun getSecureDataRepository(pluginId: String): SecureDataRepository? {
        val plugin = activePlugins[pluginId] ?: return null
        val grantedCapabilities = runBlocking { 
            permissionManager.getGrantedPermissions(pluginId) 
        }
        
        return SecureDataRepository(
            actualRepository = dataRepository,
            pluginId = pluginId,
            grantedCapabilities = grantedCapabilities,
            dataAccessScopes = plugin.securityManifest.dataAccess,
            securityMonitor = securityMonitor
        )
    }
    
    /**
     * Clean up all plugins
     */
    suspend fun cleanup() {
        activePlugins.values.forEach { plugin ->
            try {
                plugin.cleanup()
            } catch (e: Exception) {
                // Log error but continue cleanup
            }
        }
        scope.cancel()
    }
    
    private suspend fun handlePluginError(pluginId: String, error: Throwable) {
        database.pluginStateDao().recordError(
            pluginId, 
            error.message ?: "Unknown error"
        )
        
        securityMonitor.recordSecurityEvent(
            SecurityEvent.SecurityViolation(
                pluginId = pluginId,
                violationType = "PLUGIN_ERROR",
                details = error.message ?: "Unknown error",
                severity = ViolationSeverity.MEDIUM
            )
        )
    }
}

data class PluginState(
    val pluginId: String,
    val isEnabled: Boolean,
    val isCollecting: Boolean,
    val lastCollection: Instant?,
    val errorCount: Int
)
