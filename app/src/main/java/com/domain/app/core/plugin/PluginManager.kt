package com.domain.app.core.plugin

import com.domain.app.core.EventBus
import com.domain.app.core.plugin.security.PluginPermissionManager
import com.domain.app.core.plugin.security.SecurityEvent
import com.domain.app.core.plugin.security.SecurityMonitor
import com.domain.app.core.plugin.security.ViolationSeverity
import com.domain.app.core.storage.AppDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages plugin lifecycle and operations
 * Central coordinator for all plugin-related functionality
 * 
 * File location: app/src/main/java/com/domain/app/core/plugin/PluginManager.kt
 */
@Singleton
class PluginManager @Inject constructor(
    private val database: AppDatabase,
    private val permissionManager: PluginPermissionManager,
    private val securityMonitor: SecurityMonitor
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val activePlugins = mutableMapOf<String, Plugin>()
    
    private val _pluginStates = MutableStateFlow<List<PluginState>>(emptyList())
    val pluginStates: StateFlow<List<PluginState>> = _pluginStates.asStateFlow()
    
    /**
     * Initialize plugin manager
     */
    suspend fun initialize() {
        loadPlugins()
    }
    
    /**
     * Load all available plugins
     */
    private suspend fun loadPlugins() = withContext(Dispatchers.IO) {
        // In a real implementation, this would discover and load plugins
        // For now, we'll load built-in plugins
        
        val plugins = listOf(
            // Built-in plugins would be instantiated here
        )
        
        plugins.forEach { plugin ->
            registerPlugin(plugin)
        }
    }
    
    /**
     * Register a plugin
     */
    suspend fun registerPlugin(plugin: Plugin) = withContext(Dispatchers.IO) {
        try {
            // Validate plugin
            validatePlugin(plugin)
            
            // Check required permissions
            val hasRequiredPermissions = plugin.requiredCapabilities.all { capability ->
                permissionManager.requestPermission(plugin.id, capability)
            }
            
            if (!hasRequiredPermissions) {
                throw SecurityException("Plugin ${plugin.id} missing required permissions")
            }
            
            // Initialize plugin
            plugin.initialize()
            
            // Store plugin
            activePlugins[plugin.id] = plugin
            
            // Update database
            database.pluginStateDao().insert(
                com.domain.app.core.storage.entities.PluginStateEntity(
                    pluginId = plugin.id,
                    isEnabled = false,
                    isCollecting = false,
                    lastCollection = null,
                    errorCount = 0,
                    lastError = null
                )
            )
            
            updatePluginStates()
            
            EventBus.post(Event.PluginRegistered(plugin.id))
        } catch (e: Exception) {
            Timber.e(e, "Failed to register plugin: ${plugin.id}")
            throw e
        }
    }
    
    /**
     * Get a plugin by ID with null safety
     */
    fun getPlugin(pluginId: String): Plugin? {
        return activePlugins[pluginId]
    }
    
    /**
     * Get all enabled plugins
     */
    suspend fun getEnabledPlugins(): List<Plugin> = withContext(Dispatchers.IO) {
        val enabledStates = database.pluginStateDao().getEnabledPlugins()
        return@withContext enabledStates.mapNotNull { state ->
            activePlugins[state.pluginId]
        }
    }
    
    /**
     * Enable a plugin
     */
    suspend fun enablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId]
        if (plugin == null) {
            Timber.w("Cannot enable plugin: $pluginId not found")
            return@withContext
        }
        
        try {
            // Check permissions again
            val hasPermissions = permissionManager.hasAllCapabilities(pluginId, plugin.requiredCapabilities)
            if (!hasPermissions) {
                throw SecurityException("Plugin missing required permissions")
            }
            
            // Enable in database
            database.pluginStateDao().updateEnabledState(pluginId, true)
            
            updatePluginStates()
            
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
            // Stop data collection if active
            stopDataCollection(pluginId)
            
            // Disable in database
            database.pluginStateDao().updateEnabledState(pluginId, false)
            
            updatePluginStates()
            
            EventBus.post(Event.PluginDisabled(pluginId))
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Validate plugin
     */
    private fun validatePlugin(plugin: Plugin) {
        require(plugin.id.isNotBlank()) { "Plugin ID cannot be blank" }
        require(plugin.metadata.name.isNotBlank()) { "Plugin name cannot be blank" }
        require(plugin.metadata.version.isNotBlank()) { "Plugin version cannot be blank" }
    }
    
    /**
     * Update plugin states from database
     */
    private suspend fun updatePluginStates() = withContext(Dispatchers.IO) {
        val states = database.pluginStateDao().getAllStates().map { entity ->
            PluginState(
                pluginId = entity.pluginId,
                plugin = activePlugins[entity.pluginId],
                isEnabled = entity.isEnabled,
                isCollecting = entity.isCollecting,
                lastCollection = entity.lastCollection?.let { Instant.ofEpochMilli(it) },
                errorCount = entity.errorCount
            )
        }
        _pluginStates.value = states
    }
    
    /**
     * Get plugin state
     */
    suspend fun getPluginState(pluginId: String): PluginState? = withContext(Dispatchers.IO) {
        val entity = database.pluginStateDao().getState(pluginId) ?: return@withContext null
        
        return@withContext PluginState(
            pluginId = entity.pluginId,
            plugin = activePlugins[entity.pluginId],
            isEnabled = entity.isEnabled,
            isCollecting = entity.isCollecting,
            lastCollection = entity.lastCollection?.let { Instant.ofEpochMilli(it) },
            errorCount = entity.errorCount
        )
    }
    
    /**
     * Start data collection for a plugin
     */
    suspend fun startDataCollection(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId]
        if (plugin == null) {
            Timber.w("Cannot start collection: plugin $pluginId not found")
            return@withContext
        }
        
        if (!plugin.supportsAutomaticCollection) return@withContext
        
        // Check permissions
        if (!permissionManager.hasCapability(pluginId, PluginCapability.COLLECT_DATA)) {
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
        
        // Record error in database with null-safe message handling
        val errorMessage = error.message ?: "Unknown error"
        database.pluginStateDao().recordError(pluginId, errorMessage)
        
        // Record security event
        securityMonitor.recordSecurityEvent(
            SecurityEvent.SecurityViolation(
                pluginId = pluginId,
                violationType = "PLUGIN_ERROR",
                details = errorMessage,
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

/**
 * Plugin-related events
 */
sealed class Event {
    data class PluginRegistered(val pluginId: String) : Event()
    data class PluginEnabled(val pluginId: String) : Event()
    data class PluginDisabled(val pluginId: String) : Event()
    data class PluginStartedCollecting(val pluginId: String) : Event()
    data class PluginStoppedCollecting(val pluginId: String) : Event()
    data class PluginError(val pluginId: String, val error: Exception) : Event()
}
