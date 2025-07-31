package com.domain.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Settings screen
 * 
 * File location: app/src/main/java/com/domain/app/ui/settings/SettingsViewModel.kt
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadSettings()
        loadPlugins()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            preferencesManager.currentTheme.collect { theme ->
                _uiState.update { it.copy(currentTheme = theme) }
            }
        }
    }
    
    private fun loadPlugins() {
        viewModelScope.launch {
            // Load all plugins
            val allPlugins = pluginManager.getAllPlugins()
            _uiState.update { it.copy(allPlugins = allPlugins) }
            
            // Monitor enabled plugins
            pluginManager.pluginStates.collect { states ->
                val enabledIds = states.filter { it.value.isEnabled }.keys
                _uiState.update { 
                    it.copy(
                        enabledPluginIds = enabledIds,
                        enabledPluginsCount = enabledIds.size
                    )
                }
            }
        }
        
        // Monitor dashboard plugins
        viewModelScope.launch {
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
    
    fun updateTheme(theme: String) {
        viewModelScope.launch {
            preferencesManager.updateTheme(theme)
            _uiState.update { 
                it.copy(
                    currentTheme = theme,
                    showThemeDialog = false
                )
            }
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
            val plugin = pluginManager.getPlugin(pluginId) ?: return@launch
            if (enabled) {
                pluginManager.enablePlugin(plugin)
            } else {
                pluginManager.disablePlugin(plugin)
            }
        }
    }
    
    fun toggleDashboardPlugin(pluginId: String, onDashboard: Boolean) {
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

/**
 * Export format options
 */
enum class ExportFormat {
    CSV,
    JSON,
    FHIR,
    OPEN_MHEALTH
}

    fun setTheme(theme: String) {
        // TODO: Implement theme setting
    }
    
    fun toggleDashboard() {
        // TODO: Implement dashboard toggle
    }
