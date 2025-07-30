package com.domain.app.core.plugin.security

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.PluginManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing plugin security settings
 * 
 * File location: app/src/main/java/com/domain/app/core/plugin/security/PluginSecurityViewModel.kt
 */
@HiltViewModel
class PluginSecurityViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val permissionManager: PluginPermissionManager,
    private val auditLogger: SecurityAuditLogger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val pluginId: String = checkNotNull(savedStateHandle["pluginId"])
    
    private val _uiState = MutableStateFlow(PluginSecurityUiState())
    val uiState: StateFlow<PluginSecurityUiState> = _uiState.asStateFlow()
    
    init {
        loadPlugin()
        observePermissions()
        loadAuditLog()
    }
    
    private fun loadPlugin() {
        pluginManager.getPlugin(pluginId)?.let { plugin ->
            _uiState.update { it.copy(plugin = plugin) }
        }
    }
    
    private fun observePermissions() {
        viewModelScope.launch {
            permissionManager.getGrantedPermissionsFlow(pluginId).collect { permissions ->
                _uiState.update { it.copy(grantedPermissions = permissions) }
            }
        }
    }
    
    private fun loadAuditLog() {
        viewModelScope.launch {
            auditLogger.getRecentEvents(pluginId, limit = 50).collect { events ->
                // Count data access by type
                val dataAccessCounts = events
                    .filterIsInstance<SecurityEvent.DataAccess>()
                    .groupBy { it.dataType }
                    .mapValues { it.value.size }
                _uiState.update { it.copy(dataAccessCount = dataAccessCounts) }
            }
        }
    }
    
    fun requestPermission(capability: PluginCapability) {
        _uiState.update { 
            it.copy(
                pendingPermission = capability,
                showPermissionDialog = true
            )
        }
    }
    
    fun grantPendingPermission() {
        viewModelScope.launch {
            val plugin = _uiState.value.plugin
            val capability = _uiState.value.pendingPermission
            
            if (plugin != null && capability != null) {
                permissionManager.grantPermissions(
                    pluginId = plugin.id,
                    capabilities = setOf(capability),
                    grantedBy = "user_manual"
                )
            }
            
            _uiState.update { 
                it.copy(
                    pendingPermission = null,
                    showPermissionDialog = false
                )
            }
        }
    }
    
    fun dismissPermissionDialog() {
        _uiState.update { 
            it.copy(
                pendingPermission = null,
                showPermissionDialog = false
            )
        }
    }
    
    fun revokePermission(capability: PluginCapability) {
        viewModelScope.launch {
            _uiState.value.plugin?.let { plugin ->
                permissionManager.revokeSpecificPermissions(
                    pluginId = plugin.id,
                    capabilities = setOf(capability)
                )
            }
        }
    }
    
    fun revokeAllPermissions() {
        viewModelScope.launch {
            _uiState.value.plugin?.let { plugin ->
                permissionManager.revokeAllPermissions(plugin.id)
            }
        }
    }
    
    fun toggleCapability(capability: PluginCapability) {
        viewModelScope.launch {
            val plugin = _uiState.value.plugin ?: return@launch
            val isGranted = _uiState.value.grantedPermissions.contains(capability)
            
            if (isGranted) {
                permissionManager.revokePermissions(
                    pluginId = plugin.id,
                    capabilities = setOf(capability)
                )
            } else {
                permissionManager.grantPermissions(
                    pluginId = plugin.id,
                    capabilities = setOf(capability),
                    grantedBy = "user_toggle"
                )
            }
        }
    }
}

/**
 * UI State for plugin security screen
 */
data class PluginSecurityUiState(
    val plugin: Plugin? = null,
    val grantedPermissions: Set<PluginCapability> = emptySet(),
    val pendingPermission: PluginCapability? = null,
    val showPermissionDialog: Boolean = false,
    val dataAccessCount: Map<String, Int> = emptyMap(),
    val securityEvents: List<SecurityEvent> = emptyList(),
    val isLoading: Boolean = false
)
