package com.domain.app.core.plugin

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.events.Event
import com.domain.app.core.events.EventBus  
import com.domain.app.core.plugin.security.*
import com.domain.app.core.storage.AppDatabase
import com.domain.app.core.storage.entity.PluginStateEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central manager for plugin lifecycle and operations.
 * Handles plugin initialization, state management, and secure data operations.
 * 
 * File location: app/src/main/java/com/domain/app/core/plugin/PluginManager.kt
 */
@Singleton
class PluginManager @Inject constructor(
    private val context: Context,
    private val database: AppDatabase,
    private val dataRepository: DataRepository,
    val pluginRegistry: PluginRegistry,
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
                            plugin = activePlugins[entity.pluginId],
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
        
        // Initialize plugins on startup
        scope.launch {
            initializePlugins()
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
                            isEnabled = false,
                            isCollecting = false,
                            configuration = null,
                            lastCollection = null,
                            errorCount = 0,
                            lastError = null
                        )
                    )
                }
                
                activePlugins[plugin.id] = plugin
                Timber.d("Initialized plugin: ${plugin.id}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize plugin: ${plugin.id}")
            }
        }
    }
    
    /**
     * Initialize a specific plugin
     */
    suspend fun initializePlugin(pluginId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val plugin = pluginRegistry.getPlugin(pluginId) 
                ?: return@withContext Result.failure(Exception("Plugin not found: $pluginId"))
            
            plugin.initialize(context)
            activePlugins[pluginId] = plugin
            
            // Enable in database
            database.pluginStateDao().updateEnabledState(pluginId, true)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
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
            
            Timber.d("Enabled plugin: $pluginId")
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Enable a plugin by instance
     */
    suspend fun enablePlugin(plugin: Plugin) = enablePlugin(plugin.id)
    
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
            
            Timber.d("Disabled plugin: $pluginId")
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Disable a plugin by instance
     */
    suspend fun disablePlugin(plugin: Plugin) = disablePlugin(plugin.id)
    
    /**
     * Get all plugins
     */
    suspend fun getAllPlugins(): List<Plugin> {
        return pluginRegistry.getAllPlugins()
    }
    
    /**
     * Get all active plugins
     */
    fun getAllActivePlugins(): List<Plugin> {
        return _pluginStates.value
            .filter { it.value.isEnabled }
            .mapNotNull { it.value.plugin }
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
        
        // Check permissions
        if (!permissionManager.hasPermission(pluginId, PluginCapability.COLLECT_DATA)) {
            securityMonitor.recordSecurityEvent(
                SecurityEvent.PermissionDenied(
                    pluginId = pluginId,
                    capability = PluginCapability.COLLECT_DATA,
                    reason = "Manual entry permission denied"
                )
            )
            return@withContext null
        }
        
        try {
            val dataPoint = plugin.createManualEntry(data)
            
            if (dataPoint != null) {
                // Save to repository
                dataRepository.saveDataPoint(dataPoint)
                
                // Update last collection time
                database.pluginStateDao().updateCollectionTime(pluginId, Instant.now())
                
                // Log data access
                securityMonitor.recordSecurityEvent(
                    SecurityEvent.DataAccess(
                        pluginId = pluginId,
                        dataType = dataPoint.type,
                        accessType = AccessType.WRITE,
                        recordCount = 1
                    )
                )
            }
            
            dataPoint
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
            null
        }
    }
    
    /**
     * Start automatic data collection for a plugin
     */
    suspend fun startDataCollection(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId] ?: return@withContext
        
        if (!plugin.supportsAutomaticCollection) return@withContext
        
        // Check permissions
        if (!permissionManager.hasPermission(pluginId, PluginCapability.COLLECT_DATA)) {
            securityMonitor.recordSecurityEvent(
                SecurityEvent.PermissionDenied(
                    pluginId = pluginId,
                    capability = PluginCapability.COLLECT_DATA,
                    reason = "Automatic collection permission denied"
                )
            )
            return@withContext
        }
        
        try {
            database.pluginStateDao().updateCollectingState(pluginId, true)
            
            // Plugin-specific collection logic would go here
            // This is a placeholder for the actual implementation
            
            EventBus.post(Event.PluginStartedCollecting(pluginId))
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Stop automatic data collection for a plugin
     */
    suspend fun stopDataCollection(pluginId: String) = withContext(Dispatchers.IO) {
        try {
            database.pluginStateDao().updateCollectingState(pluginId, false)
            
            EventBus.post(Event.PluginStoppedCollecting(pluginId))
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Handle plugin errors
     */
    private suspend fun handlePluginError(pluginId: String, error: Exception) {
        Timber.e(error, "Plugin error: $pluginId")
        
        // Record error in database
        database.pluginStateDao().recordError(pluginId, error.message ?: "Unknown error")
        
        // Record security event
        securityMonitor.recordSecurityEvent(
            SecurityEvent.SecurityViolation(
                pluginId = pluginId,
                violationType = "PLUGIN_ERROR",
                details = error.message ?: "Unknown error",
                severity = ViolationSeverity.MEDIUM
            )
        )
        
        // Disable plugin if too many errors
        val state = getPluginState(pluginId)
        if (state != null && state.errorCount > 10) {
            disablePlugin(pluginId)
            EventBus.post(Event.PluginError(pluginId, Exception("Plugin disabled due to repeated errors")))
        }
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        scope.cancel()
        activePlugins.clear()
    }
}

/**
 * Plugin state data class
 */
data class PluginState(
    val pluginId: String,
    val plugin: Plugin? = null,
    val isEnabled: Boolean = false,
    val isCollecting: Boolean = false,
    val lastCollection: Instant? = null,
    val errorCount: Int = 0
)
