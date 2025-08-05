package com.domain.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.export.ExportManager
import com.domain.app.core.export.ExportResult
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.PluginState
import com.domain.app.core.plugin.security.PluginPermissionManager
import com.domain.app.core.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pluginManager: PluginManager,
    private val dataRepository: DataRepository,
    private val preferencesManager: PreferencesManager,
    private val permissionManager: PluginPermissionManager,
    private val exportManager: ExportManager
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
        val isCurrentlyEnabled = currentState?.isCollecting ?: false
        
        try {
            if (isCurrentlyEnabled) {
                // Disable the plugin
                pluginManager.disablePlugin(pluginId)
                _uiState.update { 
                    it.copy(message = "${plugin.metadata.name} disabled") 
                }
            } else {
                // Enable the plugin
                pluginManager.enablePlugin(pluginId)
                _uiState.update { 
                    it.copy(message = "${plugin.metadata.name} enabled") 
                }
            }
        } catch (e: Exception) {
            _uiState.update { 
                it.copy(message = "Failed to toggle ${plugin.metadata.name}: ${e.message}") 
            }
        }
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
            _uiState.update { it.copy(isExporting = true, message = null) }
            
            when (val result = exportManager.exportAllDataToCsv(context)) {
                is ExportResult.Success -> {
                    val sizeKb = result.fileSize / 1024
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            message = "Exported ${result.recordCount} records (${sizeKb}KB) to Downloads/BehavioralData/${result.fileName}",
                            lastExportPath = result.filePath
                        )
                    }
                }
                is ExportResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            message = result.message
                        )
                    }
                }
            }
        }
    }
    
    fun exportPluginData(pluginId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = null) }
            
            when (val result = exportManager.exportPluginData(context, pluginId)) {
                is ExportResult.Success -> {
                    val plugin = pluginManager.getPlugin(pluginId)
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            message = "Exported ${plugin?.metadata?.name ?: "plugin"} data: ${result.recordCount} records"
                        )
                    }
                }
                is ExportResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            message = result.message
                        )
                    }
                }
            }
        }
    }
    
    fun importData() {
        viewModelScope.launch {
            _uiState.update { it.copy(message = "Import feature coming soon") }
        }
    }
    
    fun clearAllData() {
        _uiState.update { 
            it.copy(showClearDataConfirmation = true) 
        }
    }
    
    fun confirmClearAllData() {
        viewModelScope.launch {
            dataRepository.cleanupOldData(0)
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
    
    fun grantPendingPermissions() {
        viewModelScope.launch {
            val plugin = _uiState.value.pendingPlugin ?: return@launch
            
            permissionManager.grantPermissions(
                pluginId = plugin.id,
                permissions = plugin.securityManifest.requestedCapabilities,
                grantedBy = "user_settings"
            )
            
            _uiState.update { 
                it.copy(
                    pendingPlugin = null,
                    showPermissionRequest = false,
                    message = "Permissions granted for ${plugin.metadata.name}"
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
                message = plugin?.let { "Permissions denied for ${it.metadata.name}" }
            )
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
    val isExporting: Boolean = false,
    val message: String? = null,
    val lastExportPath: String? = null
)
