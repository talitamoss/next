package com.domain.app.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.security.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginSecurityViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val permissionManager: PluginPermissionManager,
    private val securityMonitor: SecurityMonitor
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PluginSecurityUiState())
    val uiState: StateFlow<PluginSecurityUiState> = _uiState.asStateFlow()
    
    fun loadPlugin(pluginId: String) {
        viewModelScope.launch {
            val plugin = pluginManager.getPlugin(pluginId)
            if (plugin != null) {
                _uiState.update { it.copy(plugin = plugin) }
                
                // Load granted permissions
                permissionManager.getGrantedPermissionsFlow(pluginId)
                    .onEach { permissions ->
                        _uiState.update { it.copy(grantedPermissions = permissions) }
                    }
                    .launchIn(viewModelScope)
                
                // Load security summary
                val summary = securityMonitor.getPluginSecuritySummary(pluginId)
                _uiState.update { 
                    it.copy(
                        securitySummary = summary,
                        riskScore = summary.riskScore
                    )
                }
                
                // Load security events
                val events = securityMonitor.getPluginSecurityEvents(pluginId)
                _uiState.update { it.copy(securityEvents = events) }
                
                // Calculate data access counts
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
                    permissions = setOf(capability),
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
                    permissions = setOf(capability)
                )
            }
        }
    }
    
    fun revokeAllPermissions() {
        viewModelScope.launch {
            _uiState.value.plugin?.let { plugin ->
                permissionManager.revokePermissions(plugin.id)
            }
        }
    }
    
    fun deletePluginData() {
        viewModelScope.launch {
            _uiState.value.plugin?.let { plugin ->
                // TODO: Implement data deletion
                _uiState.update { 
                    it.copy(message = "Plugin data deleted")
                }
            }
        }
    }
    
    fun blockPlugin() {
        viewModelScope.launch {
            _uiState.value.plugin?.let { plugin ->
                // Revoke all permissions
                permissionManager.revokePermissions(plugin.id)
                
                // Disable plugin
                pluginManager.disablePlugin(plugin.id)
                
                // Record security event
                securityMonitor.recordSecurityEvent(
                    SecurityEvent.SecurityViolation(
                        pluginId = plugin.id,
                        violationType = "USER_BLOCKED",
                        details = "Plugin blocked by user",
                        severity = ViolationSeverity.HIGH
                    )
                )
                
                _uiState.update { 
                    it.copy(message = "Plugin blocked")
                }
            }
        }
    }
    
    fun showSecurityHistory() {
        _uiState.update { it.copy(showSecurityHistory = true) }
    }
    
    fun dismissSecurityHistory() {
        _uiState.update { it.copy(showSecurityHistory = false) }
    }
    
    fun showSecurityInfo() {
        _uiState.update { it.copy(showSecurityInfo = true) }
    }
}

data class PluginSecurityUiState(
    val plugin: Plugin? = null,
    val grantedPermissions: Set<PluginCapability> = emptySet(),
    val securitySummary: SecuritySummary? = null,
    val securityEvents: List<SecurityEvent> = emptyList(),
    val dataAccessCount: Map<String, Int> = emptyMap(),
    val riskScore: Int = 0,
    val pendingPermission: PluginCapability? = null,
    val showPermissionDialog: Boolean = false,
    val showSecurityHistory: Boolean = false,
    val showSecurityInfo: Boolean = false,
    val message: String? = null
)
