package com.domain.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.PluginState
import com.domain.app.core.plugin.security.PluginPermissionManager
import com.domain.app.core.plugin.security.PluginTrustLevel
import com.domain.app.core.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val dataRepository: DataRepository,
    private val preferencesManager: PreferencesManager,
    private val permissionManager: PluginPermissionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadPlugins()
        observePluginStates()
        observeDashboardPlugins()
    }
    
    private fun loadPlugins() {
        val plugins = pluginManager.getAllActivePlugins()
        _uiState.update { it.copy(plugins = plugins) }
    }
    
    private fun observePluginStates() {
        pluginManager.pluginStates
            .onEach { states ->
                _uiState.update { it.copy(pluginStates = states) }
            }
            .launchIn(viewModelScope)
    }
    
    private fun observeDashboardPlugins() {
        preferencesManager.dashboardPlugins
            .onEach { dashboardIds ->
                _uiState.update { 
                    it.copy(dashboardPluginIds = dashboardIds.toSet()) 
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun togglePlugin(pluginId: String) {
        viewModelScope.launch {
            val plugin = pluginManager.getPlugin(pluginId) ?: return@launch
            val currentState = _uiState.value.pluginStates[pluginId]
            
            // Check if plugin is currently collecting (enabled)
            if (currentState?.isCollecting == true) {
                // Disabling - just disable
                pluginManager.disablePlugin(pluginId)
                _uiState.update { 
                    it.copy(message = "${plugin.metadata.name} disabled") 
                }
            } else {
                // Enabling - check permissions first
                val hasPermissions = permissionManager.hasPermission(
                    pluginId, 
                    PluginCapability.COLLECT_DATA
                )
                
                if (!hasPermissions && plugin.trustLevel != PluginTrustLevel.OFFICIAL) {
                    // Show permission request dialog
                    _uiState.update {
                        it.copy(
                            pendingPlugin = plugin,
                            showPermissionRequest = true
                        )
                    }
                } else {
                    // Already has permissions or is official plugin
                    pluginManager.enablePlugin(pluginId)
                    _uiState.update { 
                        it.copy(message = "${plugin.metadata.name} enabled") 
                    }
                }
            }
        }
    }
    
    fun grantPendingPermissions() {
        viewModelScope.launch {
            val plugin = _uiState.value.pendingPlugin ?: return@launch
            
            // Grant all requested permissions
            permissionManager.grantPermissions(
                pluginId = plugin.id,
                permissions = plugin.securityManifest.requestedCapabilities,
                grantedBy = "user_settings"
            )
            
            // Now enable the plugin
            pluginManager.enablePlugin(plugin.id)
            
            _uiState.update {
                it.copy(
                    pendingPlugin = null,
                    showPermissionRequest = false,
                    message = "${plugin.metadata.name} enabled with permissions granted"
                )
            }
        }
    }
    
    fun denyPendingPermissions() {
        val plugin = _uiState.value.pendingPlugin
        _uiState.update {
            it.copy(
                pendingPlugin = null,
                showPermissionRequest = false,
                message = if (plugin != null) 
                    "${plugin.metadata.name} cannot be enabled without permissions" 
                else null
            )
        }
    }
    
    fun navigateToPluginSecurity(pluginId: String) {
        _uiState.update {
            it.copy(navigateToSecurity = pluginId)
        }
    }
    
    fun clearNavigation() {
        _uiState.update {
            it.copy(navigateToSecurity = null)
        }
    }
    
    fun toggleDashboard(pluginId: String) {
        viewModelScope.launch {
            val plugin = pluginManager.getPlugin(pluginId) ?: return@launch
            
            if (_uiState.value.dashboardPluginIds.contains(pluginId)) {
                preferencesManager.removeFromDashboard(pluginId)
                _uiState.update { 
                    it.copy(message = "${plugin.metadata.name} removed from dashboard") 
                }
            } else {
                // Check if we've reached the maximum
                if (_uiState.value.dashboardPluginIds.size >= 6) {
                    _uiState.update { 
                        it.copy(message = "Maximum 6 plugins allowed on dashboard") 
                    }
                    return@launch
                }
                
                preferencesManager.addToDashboard(pluginId)
                _uiState.update { 
                    it.copy(message = "${plugin.metadata.name} added to dashboard") 
                }
            }
        }
    }
    
    fun reorderDashboard(pluginIds: List<String>) {
        viewModelScope.launch {
            preferencesManager.updateDashboardPlugins(pluginIds)
        }
    }
    
    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(message = "Export feature coming soon") }
        }
    }
    
    fun importData() {
        viewModelScope.launch {
            _uiState.update { it.copy(message = "Import feature coming soon") }
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            // Show confirmation dialog
            _uiState.update { 
                it.copy(showClearDataConfirmation = true) 
            }
        }
    }
    
    fun confirmClearAllData() {
        viewModelScope.launch {
            dataRepository.cleanupOldData(0) // This will delete all data
            _uiState.update { 
                it.copy(
                    message = "All data cleared",
                    showClearDataConfirmation = false
                ) 
            }
        }
    }
    
    fun cancelClearData() {
        _uiState.update { 
            it.copy(showClearDataConfirmation = false) 
        }
    }
    
    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

data class SettingsUiState(
    val plugins: List<Plugin> = emptyList(),
    val pluginStates: Map<String, PluginState> = emptyMap(),
    val dashboardPluginIds: Set<String> = emptySet(),
    val pendingPlugin: Plugin? = null,
    val showPermissionRequest: Boolean = false,
    val showClearDataConfirmation: Boolean = false,
    val navigateToSecurity: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null
)
