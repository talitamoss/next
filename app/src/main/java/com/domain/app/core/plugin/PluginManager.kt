package com.domain.app.core.plugin

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.data.toEntity
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
    private val pluginSandboxes = mutableMapOf<String, PluginSandbox>()
    private val secureRepositories = mutableMapOf<String, SecureDataRepository>()
    
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
        
        // Monitor security violations
        scope.launch {
            securityMonitor.activeViolations.collect { violations ->
                violations.forEach { (pluginId, pluginViolations) ->
                    if (pluginViolations.any { it.severity == ViolationSeverity.CRITICAL }) {
                        quarantinePlugin(pluginId)
                    }
                }
            }
        }
    }
    
    /**
     * Initialize all registered plugins with security checks
     */
    suspend fun initializePlugins() = withContext(Dispatchers.IO) {
        val registeredPlugins = pluginRegistry.getAllPlugins()
        
        registeredPlugins.forEach { plugin ->
            try {
                // Check if plugin is quarantined
                if (securityMonitor.shouldQuarantine(plugin.id)) {
                    securityMonitor.recordSecurityEvent(
                        SecurityEvent.SecurityViolation(
                            pluginId = plugin.id,
                            violationType = "QUARANTINE_ON_INIT",
                            details = "Plugin quarantined due to security violations",
                            severity = ViolationSeverity.HIGH
                        )
                    )
                    return@forEach
                }
                
                // Request initial permissions for official plugins
                if (plugin.trustLevel == PluginTrustLevel.OFFICIAL) {
                    permissionManager.grantPermissions(
                        pluginId = plugin.id,
                        permissions = plugin.securityManifest.requestedCapabilities,
                        grantedBy = "system_auto"
                    )
                }
                
                // Create sandbox
                val grantedPermissions = permissionManager.getGrantedPermissions(plugin.id)
                val sandbox = PluginSandbox(plugin, grantedPermissions, securityMonitor)
                pluginSandboxes[plugin.id] = sandbox
                
                // Create secure data repository
                val secureRepo = SecureDataRepository(
                    actualRepository = dataRepository,
                    pluginId = plugin.id,
                    grantedCapabilities = grantedPermissions,
                    dataAccessScopes = plugin.securityManifest.dataAccess,
                    securityMonitor = securityMonitor
                )
                secureRepositories[plugin.id] = secureRepo
                
                // Initialize plugin in sandbox
                sandbox.executeInSandbox("initialize") {
                    plugin.initialize(context)
                }
                
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
     * Enable a plugin with permission check
     */
    suspend fun enablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId] ?: return@withContext
        
        try {
            // Check permissions
            val grantedPermissions = permissionManager.getGrantedPermissions(pluginId)
            val requiredPermissions = plugin.securityManifest.requestedCapabilities
            
            if (!grantedPermissions.containsAll(requiredPermissions)) {
                // Request missing permissions
                val missingPermissions = requiredPermissions - grantedPermissions
                
                securityMonitor.recordSecurityEvent(
                    SecurityEvent.PermissionRequested(
                        pluginId = pluginId,
                        capability = missingPermissions.first() // Log first missing
                    )
                )
                
                // In real app, this would trigger UI permission request
                // For now, just log
                return@withContext
            }
            
            database.pluginStateDao().updateCollectingState(pluginId, true)
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
     * Create manual data entry with security checks
     */
    suspend fun createManualEntry(
        pluginId: String, 
        data: Map<String, Any>
    ): DataPoint? = withContext(Dispatchers.IO) {
        val plugin = activePlugins[pluginId] ?: return@withContext null
        val sandbox = pluginSandboxes[pluginId] ?: return@withContext null
        
        if (!plugin.supportsManualEntry()) {
            return@withContext null
        }
        
        // Check COLLECT_DATA permission
        if (!permissionManager.hasPermission(pluginId, PluginCapability.COLLECT_DATA)) {
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
            // Create data point in sandbox
            val result = sandbox.executeInSandbox("data.write") {
                plugin.createManualEntry(data)
            }
            
            val dataPoint = result.getOrNull()
            if (dataPoint != null) {
                // Use secure repository to save
                val secureRepo = secureRepositories[pluginId]
                if (secureRepo != null) {
                    secureRepo.saveDataPoint(dataPoint)
                } else {
                    // Fallback to direct save (shouldn't happen)
                    database.dataPointDao().insert(dataPoint.toEntity())
                }
                
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
            }
            return@withContext dataPoint
        } catch (e: Exception) {
            handlePluginError(pluginId, e)
            return@withContext null
        }
    }
    
    /**
     * Get secure data repository for a plugin
     */
    fun getSecureRepository(pluginId: String): SecureDataRepository? {
        return secureRepositories[pluginId]
    }
    
    /**
     * Check if plugin has specific permission
     */
    suspend fun hasPermission(pluginId: String, capability: PluginCapability): Boolean {
        return permissionManager.hasPermission(pluginId, capability)
    }    
    /**
     * Request permissions for a plugin
     */
    suspend fun requestPermissions(
        pluginId: String,
        capabilities: Set<PluginCapability>
    ): Boolean {
        val plugin = activePlugins[pluginId] ?: return false
        
        // Record permission requests
        capabilities.forEach { capability ->
            securityMonitor.recordSecurityEvent(
                SecurityEvent.PermissionRequested(
                    pluginId = pluginId,
                    capability = capability
                )
            )
        }
        
        // In real app, this would trigger UI flow
        // For now, return false (denied)
        return false
    }
    
    /**
     * Quarantine a plugin due to security violations
     */
    private suspend fun quarantinePlugin(pluginId: String) {
        // Disable plugin
        disablePlugin(pluginId)
        
        // Revoke all permissions
        permissionManager.revokePermissions(pluginId)
        
        // Clean up resources
        pluginSandboxes[pluginId]?.cleanup()
        pluginSandboxes.remove(pluginId)
        secureRepositories.remove(pluginId)
        
        // Update state
        database.pluginStateDao().insertOrUpdate(
            database.pluginStateDao().getState(pluginId)?.copy(
                isEnabled = false,
                isCollecting = false,
                lastError = "Plugin quarantined due to security violations"
            ) ?: PluginStateEntity(
                pluginId = pluginId,
                isEnabled = false,
                isCollecting = false,
                lastError = "Plugin quarantined due to security violations"
            )
        )
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
        // Clean up sandboxes
        pluginSandboxes.values.forEach { it.cleanup() }
        pluginSandboxes.clear()
        
        // Clean up secure repositories
        secureRepositories.clear()
        
        // Clean up plugins
        activePlugins.values.forEach { plugin ->
            try {
                plugin.cleanup()
            } catch (e: Exception) {
                // Log error but continue cleanup
            }
        }
        activePlugins.clear()
        
        scope.cancel()
    }
    
    private suspend fun handlePluginError(pluginId: String, error: Throwable) {
        database.pluginStateDao().recordError(
            pluginId, 
            error.message ?: "Unknown error"
        )
        
        // Record security event for errors
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
