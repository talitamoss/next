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
            // Check permissions
            val grantedPermissions = permissionManager.getGrantedPermissions(pluginId)
            val requiredPermissions = plugin.securityManifest.requestedCapabilities
            
            // Official plugins can be enabled without explicit permissions
            if (plugin.trustLevel == PluginTrustLevel.OFFICIAL && !grantedPermissions.containsAll(requiredPermissions)) {
                // Auto-grant permissions for official plugins
                permissionManager.grantPermissions(
                    pluginId = pluginId,
                    permissions = requiredPermissions,
                    grantedBy = "system_auto"
                )
            } else if (!grantedPermissions.containsAll(requiredPermissions)) {
                // Non-official plugins need explicit permissions
                val missingPermissions = requiredPermissions - grantedPermissions
                
                securityMonitor.recordSecurityEvent(
                    SecurityEvent.PermissionDenied(
                        pluginId = pluginId,
                        capability = missingPermissions.first(),
                        reason = "Missing required permissions to enable plugin"
                    )
                )
                
                return@withContext
            }
            
            // Update database state
            database.pluginStateDao().updateCollectingState(pluginId, true)
            database.pluginStateDao().insertOrUpdate(
                database.pluginStateDao().getState(pluginId)?.copy(
                    isEnabled = true,
                    isCollecting = true
                ) ?: PluginStateEntity(
                    pluginId = pluginId,
                    isEnabled = true,
                    isCollecting = true
                )
            )
            database.pluginStateDao().clearErrors(pluginId)
            
            EventBus.emit(Event.PluginStateChanged(pluginId, true))
            
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
        }
    }
    
    /**
     * Disable a plugin
     */
    suspend fun disablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId] ?: return@withContext
        
        try {
            // Update database state
            database.pluginStateDao().updateCollectingState(pluginId, false)
            database.pluginStateDao().insertOrUpdate(
                database.pluginStateDao().getState(pluginId)?.copy(
                    isEnabled = false,
                    isCollecting = false
                ) ?: PluginStateEntity(
                    pluginId = pluginId,
                    isEnabled = false,
                    isCollecting = false
                )
            )
            
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
        
        // Create sandbox for secure execution
        val grantedCapabilities = permissionManager.getGrantedPermissions(pluginId)
        val sandbox = PluginSandbox(plugin, grantedCapabilities, securityMonitor)
        
        try {
            val result = sandbox.executeInSandbox("data.write") {
                plugin.createManualEntry(data)
            }
            
            result.getOrNull()?.let { dataPoint ->
                // Save to database
                dataRepository.saveDataPoint(dataPoint)
                
                // Emit event
                EventBus.emit(Event.DataCollected(dataPoint))
                
                // Update last collection time
                database.pluginStateDao().insertOrUpdate(
                    database.pluginStateDao().getState(pluginId)?.copy(
                        lastCollection = Instant.now()
                    ) ?: PluginStateEntity(
                        pluginId = pluginId,
                        lastCollection = Instant.now()
                    )
                )
                
                return@withContext dataPoint
            }
            
            return@withContext null
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
            return@withContext null
        } finally {
            sandbox.cleanup()
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
