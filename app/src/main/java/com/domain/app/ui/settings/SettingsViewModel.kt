package com.domain.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.PluginState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val dataRepository: DataRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    init {
        loadPlugins()
        observePluginStates()
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
    
    fun exportData() {
        viewModelScope.launch {
            // TODO: Implement data export
            // 1. Get all data from repository
            // 2. Convert to CSV format
            // 3. Save to file or share
            _uiState.update { it.copy(message = "Export feature coming soon") }
        }
    }
    
    fun importData() {
        viewModelScope.launch {
            // TODO: Implement data import
            // 1. File picker
            // 2. Parse CSV
            // 3. Validate and import
            _uiState.update { it.copy(message = "Import feature coming soon") }
        }
    }
    
    fun clearAllData() {
        viewModelScope.launch {
            // TODO: Show confirmation dialog first
            dataRepository.cleanupOldData(0) // This will delete all data
            _uiState.update { it.copy(message = "All data cleared") }
        }
    }
}

data class SettingsUiState(
    val plugins: List<Plugin> = emptyList(),
    val pluginStates: Map<String, PluginState> = emptyMap(),
    val isLoading: Boolean = false,
    val message: String? = null
)
