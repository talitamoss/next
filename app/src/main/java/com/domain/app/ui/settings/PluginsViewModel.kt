package com.domain.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.PluginState
import com.domain.app.core.plugin.security.PluginPermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginsViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val permissionManager: PluginPermissionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(PluginsUiState())
    val uiState: StateFlow<PluginsUiState> = _uiState.asStateFlow()
    
    init {
        loadPlugins()
        observePluginStates()
    }
    
    private fun loadPlugins() {
        viewModelScope.launch {
            // Get all available plugins
            val allPlugins = pluginManager.getAllActivePlugins()
            _uiState.update { state ->
                state.copy(plugins = allPlugins)
            }
        }
    }
    
    private fun observePluginStates() {
        // Observe plugin states to track which are enabled
        pluginManager.pluginStates
            .onEach { states ->
                val enabledIds = states
                    .filter { (_, state) -> state.isEnabled }
                    .keys
                    .toSet()
                
                _uiState.update { uiState ->
                    uiState.copy(
                        enabledPlugins = enabledIds,
                        pluginStates = states
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun enablePlugin(plugin: Plugin) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Check if plugin has required permissions
                val grantedPermissions = permissionManager.getGrantedPermissions(plugin.id)
                val requiredPermissions = plugin.securityManifest.requestedCapabilities
                val hasAllPermissions = grantedPermissions.containsAll(requiredPermissions)
                
                if (!hasAllPermissions) {
                    // Grant permissions (this happens after user approval in UI)
                    permissionManager.grantPermissions(
                        pluginId = plugin.id,
                        permissions = plugin.securityManifest.requestedCapabilities,
                        grantedBy = "user"
                    )
                }
                
                // Enable the plugin
                pluginManager.enablePlugin(plugin.id)
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        lastEnabledPlugin = plugin,
                        message = "${plugin.metadata.name} enabled successfully"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Failed to enable ${plugin.metadata.name}: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun disablePlugin(plugin: Plugin) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                // Disable the plugin
                pluginManager.disablePlugin(plugin.id)
                
                // Optionally revoke permissions
                if (_uiState.value.revokePermissionsOnDisable) {
                    permissionManager.revokePermissions(plugin.id)
                }
                
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        message = "${plugin.metadata.name} disabled"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        error = "Failed to disable ${plugin.metadata.name}: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun togglePlugin(plugin: Plugin) {
        if (_uiState.value.enabledPlugins.contains(plugin.id)) {
            disablePlugin(plugin)
        } else {
            enablePlugin(plugin)
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun setRevokePermissionsOnDisable(revoke: Boolean) {
        _uiState.update { it.copy(revokePermissionsOnDisable = revoke) }
    }
    
    fun filterPlugins(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }
    
    fun searchPlugins(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }
}

data class PluginsUiState(
    // Main data
    val plugins: List<Plugin> = emptyList(),
    val enabledPlugins: Set<String> = emptySet(),  // Set of plugin IDs that are enabled
    val pluginStates: Map<String, PluginState> = emptyMap(),
    
    // UI state
    val isLoading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    
    // Settings
    val revokePermissionsOnDisable: Boolean = false,
    
    // Filtering
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    
    // Last action tracking
    val lastEnabledPlugin: Plugin? = null
) {
    // Computed properties for UI convenience
    val filteredPlugins: List<Plugin>
        get() = plugins
            .filter { plugin ->
                // Filter by search query
                (searchQuery.isBlank() || 
                 plugin.metadata.name.contains(searchQuery, ignoreCase = true) ||
                 plugin.metadata.description.contains(searchQuery, ignoreCase = true))
            }
            .filter { plugin ->
                // Filter by category
                selectedCategory == null || 
                plugin.metadata.category.name == selectedCategory
            }
    
    val enabledCount: Int
        get() = enabledPlugins.size
    
    val totalCount: Int
        get() = plugins.size
}
