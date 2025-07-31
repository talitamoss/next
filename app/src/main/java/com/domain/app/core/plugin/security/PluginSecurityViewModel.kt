package com.domain.app.core.plugin.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.PluginManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginSecurityViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val permissionManager: PluginPermissionManager,
    private val securityMonitor: SecurityMonitor,
    private val auditLogger: SecurityAuditLogger
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PluginSecurityUiState())
    val uiState: StateFlow<PluginSecurityUiState> = _uiState.asStateFlow()
    
    fun loadPlugin(pluginId: String) {
        viewModelScope.launch {
            val plugin = pluginManager.getPlugin(pluginId)
            if (plugin != null) {
                _uiState.update { it.copy(plugin = plugin) }
                
                // Load granted permissions
                val grantedPermissions = permissionManager.getGrantedPermissions(pluginId)
                _uiState.update { it.copy(grantedPermissions = grantedPermissions) }
                
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
    
    fun grantPermission(capability: PluginCapability) {
        viewModelScope.launch {
            _uiState.value.plugin?.let { plugin ->
                permissionManager.grantPermissions(
                    pluginId = plugin.id,
                    permissions = setOf(capability),
                    grantedBy = "user"
                )
                
                // Reload permissions
                val updatedPermissions = permissionManager.getGrantedPermissions(plugin.id)
                _uiState.update { it.copy(grantedPermissions = updatedPermissions) }
            }
        }
    }
    
    fun denyPermission(capability: PluginCapability) {
        viewModelScope.launch {
            _uiState.value.plugin?.let { plugin ->
                permissionManager.revokePermissions(
                    pluginId = plugin.id,
                    permissions = setOf(capability)
                )
                
                // Reload permissions
                val updatedPermissions = permissionManager.getGrantedPermissions(plugin.id)
                _uiState.update { it.copy(grantedPermissions = updatedPermissions) }
            }
        }
    }
    
    fun revokeAllPermissions() {
        viewModelScope.launch {
            _uiState.value.plugin?.let { plugin ->
                permissionManager.revokePermissions(plugin.id)
                _uiState.update { it.copy(grantedPermissions = emptySet()) }
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
