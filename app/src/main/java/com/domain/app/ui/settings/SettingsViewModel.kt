// app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt
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
        
        _uiState.update { 
            it.copy(
                plugins = plugins,
                pluginSummaries = plugins.map { plugin ->
                    plugin.id to plugin.metadata.name
                }
            )
        }
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
            
            // Store state in local variable to avoid smart cast issues
            val currentState = _uiState.value.pluginStates[pluginId]
            val isCurrentlyEnabled = currentState?.isCollecting ?: false
            
            if (!isCurrentlyEnabled) {
                // Check permissions before enabling
                val hasPermissions = permissionManager.hasRequiredPermissions(
                    pluginId = plugin.id,
                    capabilities = plugin.securityManifest.requestedCapabilities
                )
                
                if (!hasPermissions && plugin.securityManifest.requestedCapabilities.isNotEmpty()) {
                    // Store plugin for permission request
                    _uiState.update { 
                        it.copy(
                            pendingPlugin = plugin,
                            showPermissionRequest = true
                        )
                    }
                    return@launch
                }
            }
            
            // Toggle the plugin
            try {
                if (isCurrentlyEnabled) {
                    pluginManager.disablePlugin(pluginId)
                    _uiState.update { 
                        it.copy(message = "${plugin.metadata.name} disabled")
                    }
                } else {
                    pluginManager.enablePlugin(pluginId)
                    _uiState.update { 
                        it.copy(message = "${plugin.metadata.name} enabled")
                    }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to toggle plugin: ${e.message}")
                }
            }
        }
    }
    
    fun toggleDashboard(pluginId: String) {
        viewModelScope.launch {
            val plugin = pluginManager.getPlugin(pluginId) ?: return@launch
            
            val currentDashboardIds = _uiState.value.dashboardPluginIds
            
            if (currentDashboardIds.contains(pluginId)) {
                preferencesManager.removeFromDashboard(pluginId)
                _uiState.update { 
                    it.copy(message = "${plugin.metadata.name} removed from dashboard") 
                }
            } else {
                if (currentDashboardIds.size >= 6) {
                    _uiState.update { 
                        it.copy(error = "Maximum 6 plugins allowed on dashboard") 
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
            try {
                preferencesManager.updateDashboardPlugins(pluginIds)
                _uiState.update { 
                    it.copy(message = "Dashboard order updated")
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to reorder dashboard: ${e.message}")
                }
            }
        }
    }
    
    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = null, error = null) }
            
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
                            error = result.message
                        )
                    }
                }
            }
        }
    }
    
    fun exportPluginData(pluginId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, message = null, error = null) }
            
            when (val result = exportManager.exportPluginData(context, pluginId)) {
                is ExportResult.Success -> {
                    val plugin = pluginManager.getPlugin(pluginId)
                    val sizeKb = result.fileSize / 1024
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            message = "Exported ${plugin?.metadata?.name ?: "plugin"} data: ${result.recordCount} records (${sizeKb}KB)"
                        )
                    }
                }
                is ExportResult.Error -> {
                    _uiState.update { 
                        it.copy(
                            isExporting = false,
                            error = result.message
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
            try {
                _uiState.update { it.copy(isDeleting = true) }
                
                dataRepository.cleanupOldData(0)
                
                _uiState.update { 
                    it.copy(
                        message = "All data cleared successfully",
                        showClearDataConfirmation = false,
                        isDeleting = false
                    ) 
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = "Failed to clear data: ${e.message}",
                        showClearDataConfirmation = false,
                        isDeleting = false
                    )
                }
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
            
            try {
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
                
                // Now enable the plugin
                pluginManager.enablePlugin(plugin.id)
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        pendingPlugin = null,
                        showPermissionRequest = false,
                        error = "Failed to grant permissions: ${e.message}"
                    )
                }
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
    
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for the Settings screen
 */
data class SettingsUiState(
    // Plugin data
    val plugins: List<Plugin> = emptyList(),
    val pluginSummaries: List<Pair<String, String>> = emptyList(), // For dropdowns if needed
    val pluginStates: Map<String, PluginState> = emptyMap(),
    val dashboardPluginIds: Set<String> = emptySet(),
    
    // Permission handling
    val pendingPlugin: Plugin? = null,
    val showPermissionRequest: Boolean = false,
    
    // Dialogs
    val showClearDataConfirmation: Boolean = false,
    
    // Navigation
    val navigateToSecurity: String? = null,
    
    // Export/Import
    val lastExportPath: String? = null,
    
    // Loading states
    val isExporting: Boolean = false,
    val isDeleting: Boolean = false,
    
    // Messages
    val message: String? = null,
    val error: String? = null
)

// Extension function for plugin permissions
fun Plugin.hasRequiredPermissions(context: Context): Boolean {
    return true // Simplified - implement actual permission check as needed
}
