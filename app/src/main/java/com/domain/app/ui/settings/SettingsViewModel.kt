package com.domain.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Settings screen
 * 
 * File location: app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val preferencesManager: PreferencesManager,
    private val dataRepository: DataRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        observePlugins()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            // Load theme preference
            preferencesManager.themeMode.collect { theme ->
                _uiState.update { it.copy(currentTheme = theme.capitalize()) }
            }
        }
    }
    
    private fun observePlugins() {
        viewModelScope.launch {
            // Get all plugins
            val allPlugins = pluginManager.getAllPlugins()
            _uiState.update { it.copy(allPlugins = allPlugins) }
            
            // Observe enabled plugins
            preferencesManager.enabledPlugins.collect { enabledIds ->
                _uiState.update { 
                    it.copy(
                        enabledPluginIds = enabledIds,
                        enabledPluginsCount = enabledIds.size
                    )
                }
            }
        }
        
        viewModelScope.launch {
            // Observe dashboard plugins
            preferencesManager.dashboardPlugins.collect { dashboardIds ->
                _uiState.update { it.copy(dashboardPluginIds = dashboardIds) }
            }
        }
    }
    
    fun showThemeDialog() {
        _uiState.update { it.copy(showThemeDialog = true) }
    }
    
    fun hideThemeDialog() {
        _uiState.update { it.copy(showThemeDialog = false) }
    }
    
    fun setTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(theme.lowercase())
        }
    }
    
    fun showPluginManagement() {
        _uiState.update { it.copy(showPluginManagement = true) }
    }
    
    fun hidePluginManagement() {
        _uiState.update { it.copy(showPluginManagement = false) }
    }
    
    fun togglePlugin(pluginId: String, enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                // Initialize and enable plugin
                pluginManager.initializePlugin(pluginId).fold(
                    onSuccess = {
                        _uiState.update { 
                            it.copy(message = "Plugin enabled successfully")
                        }
                    },
                    onFailure = { error ->
                        _uiState.update { 
                            it.copy(message = "Failed to enable plugin: ${error.message}")
                        }
                    }
                )
            } else {
                // Disable plugin
                pluginManager.disablePlugin(pluginId)
                preferencesManager.removeFromDashboard(pluginId)
            }
        }
    }
    
    fun toggleDashboard(pluginId: String, onDashboard: Boolean) {
        viewModelScope.launch {
            if (onDashboard) {
                preferencesManager.addToDashboard(pluginId)
            } else {
                preferencesManager.removeFromDashboard(pluginId)
            }
        }
    }
    
    fun showExportDialog() {
        _uiState.update { it.copy(showExportDialog = true) }
    }
    
    fun hideExportDialog() {
        _uiState.update { it.copy(showExportDialog = false) }
    }
    
    fun exportData(format: ExportFormat) {
        viewModelScope.launch {
            // TODO: Implement data export
            _uiState.update { 
                it.copy(message = "Export feature coming soon")
            }
        }
    }
    
    fun showClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = true) }
    }
    
    fun hideClearDataDialog() {
        _uiState.update { it.copy(showClearDataDialog = false) }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            // Clear all data
            preferencesManager.clearAll()
            // TODO: Clear database
            
            _uiState.update { 
                it.copy(
                    message = "All data cleared successfully",
                    showClearDataDialog = false
                )
            }
        }
    }
    
    fun dismissMessage() {
        _uiState.update { it.copy(message = null) }
    }
}

/**
 * UI State for Settings screen
 */
data class SettingsUiState(
    val currentTheme: String = "System",
    val enabledPluginsCount: Int = 0,
    val allPlugins: List<Plugin> = emptyList(),
    val enabledPluginIds: Set<String> = emptySet(),
    val dashboardPluginIds: Set<String> = emptySet(),
    val showThemeDialog: Boolean = false,
    val showPluginManagement: Boolean = false,
    val showExportDialog: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val message: String? = null
)
