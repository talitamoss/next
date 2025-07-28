package com.domain.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.PluginState
import com.domain.app.core.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val dataRepository: DataRepository,
    private val preferencesManager: PreferencesManager
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
            val currentState = _uiState.value.pluginStates[pluginId]
            if (currentState?.isEnabled == true) {
                pluginManager.disablePlugin(pluginId)
            } else {
                pluginManager.enablePlugin(pluginId)
            }
        }
    }
    
    fun toggleDashboard(pluginId: String) {
        viewModelScope.launch {
            if (_uiState.value.dashboardPluginIds.contains(pluginId)) {
                preferencesManager.removeFromDashboard(pluginId)
            } else {
                preferencesManager.addToDashboard(pluginId)
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
            dataRepository.cleanupOldData(0)
            _uiState.update { it.copy(message = "All data cleared") }
        }
    }
}

data class SettingsUiState(
    val plugins: List<Plugin> = emptyList(),
    val pluginStates: Map<String, PluginState> = emptyMap(),
    val dashboardPluginIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val message: String? = null
)
