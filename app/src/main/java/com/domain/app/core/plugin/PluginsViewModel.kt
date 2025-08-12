package com.domain.app.ui.settings

import com.domain.app.core.plugin.PluginRegistry
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.security.PluginTrustLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the plugins settings screen
 */
data class PluginsUiState(
    val plugins: List<Plugin> = emptyList(),
    val enabledPlugins: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val showOnlyEnabled: Boolean = false
)

/**
 * ViewModel for managing plugin settings and configuration
 */
@HiltViewModel
class PluginsViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val pluginRegistry: PluginRegistry
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PluginsUiState())
    val uiState: StateFlow<PluginsUiState> = _uiState.asStateFlow()
    
    init {
        loadPlugins()
    }
    
    /**
     * Load all available plugins and their enabled states
     */
    private fun loadPlugins() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val allPlugins = pluginRegistry.getAllPlugins()
                val enabledPlugins = pluginManager.getAllActivePlugins()
                val enabledIds = enabledPlugins.map { it.id }.toSet()
                
                _uiState.update { state ->
                    state.copy(
                        plugins = allPlugins,
                        enabledPlugins = enabledIds,
                        isLoading = false,
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        errorMessage = "Failed to load plugins: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Enable a plugin
     */
    fun enablePlugin(plugin: Plugin) {
        viewModelScope.launch {
            try {
                pluginManager.enablePlugin(plugin.id)
                loadPlugins() // Reload to get updated states
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(errorMessage = "Failed to enable plugin: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Disable a plugin
     */
    fun disablePlugin(plugin: Plugin) {
        viewModelScope.launch {
            try {
                pluginManager.disablePlugin(plugin.id)
                loadPlugins() // Reload to get updated states
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(errorMessage = "Failed to disable plugin: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Toggle plugin enabled state
     */
    fun togglePlugin(plugin: Plugin) {
        if (plugin.id in uiState.value.enabledPlugins) {
            disablePlugin(plugin)
        } else {
            enablePlugin(plugin)
        }
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
    
    /**
     * Update selected category filter
     */
    fun updateSelectedCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
    
    /**
     * Toggle showing only enabled plugins
     */
    fun toggleShowOnlyEnabled() {
        _uiState.update { it.copy(showOnlyEnabled = !it.showOnlyEnabled) }
    }
    
    /**
     * Get filtered plugins based on current UI state
     */
    fun getFilteredPlugins(): List<Plugin> {
        val state = uiState.value
        var filtered = state.plugins
        
        // Apply search filter
        if (state.searchQuery.isNotBlank()) {
            filtered = filtered.filter { plugin ->
                plugin.metadata.name.contains(state.searchQuery, ignoreCase = true) ||
                plugin.metadata.description.contains(state.searchQuery, ignoreCase = true) ||
                plugin.metadata.tags.any { it.contains(state.searchQuery, ignoreCase = true) }
            }
        }
        
        // Apply category filter
        state.selectedCategory?.let { category ->
            filtered = filtered.filter { plugin ->
                plugin.metadata.category.name == category
            }
        }
        
        // Apply enabled filter
        if (state.showOnlyEnabled) {
            filtered = filtered.filter { plugin ->
                plugin.id in state.enabledPlugins
            }
        }
        
        return filtered
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Refresh plugin list
     */
    fun refresh() {
        loadPlugins()
    }
}
