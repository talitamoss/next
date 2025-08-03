package com.domain.app.ui.dashboard

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
 * ViewModel for Dashboard screen
 * Manages plugin display and data collection states
 * 
 * File location: app/src/main/java/com/domain/app/ui/dashboard/DashboardViewModel.kt
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        loadDashboard()
        observePluginStates()
    }
    
    /**
     * Load dashboard configuration and plugins
     */
    private fun loadDashboard() {
        viewModelScope.launch {
            // Load enabled plugins
            val enabledPlugins = pluginManager.getEnabledPlugins()
            
            // Load dashboard plugin IDs from preferences
            val dashboardPluginIds = preferencesManager.getDashboardPluginIds()
            
            // Filter enabled plugins that are on the dashboard
            val dashboardPlugins = enabledPlugins.filter { plugin ->
                dashboardPluginIds.contains(plugin.id)
            }
            
            _uiState.update { state ->
                state.copy(
                    plugins = enabledPlugins,
                    dashboardPlugins = dashboardPlugins,
                    isLoading = false
                )
            }
        }
    }
    
    /**
     * Observe plugin state changes
     */
    private fun observePluginStates() {
        viewModelScope.launch {
            pluginManager.pluginStates.collect { states ->
                _uiState.update { uiState ->
                    uiState.copy(
                        pluginStates = states.associateBy { it.pluginId }
                    )
                }
            }
        }
    }
    
    /**
     * Toggle plugin data collection
     */
    fun toggleCollection(pluginId: String) {
        viewModelScope.launch {
            val state = _uiState.value.pluginStates[pluginId]
            if (state?.isCollecting == true) {
                pluginManager.stopDataCollection(pluginId)
            } else {
                pluginManager.startDataCollection(pluginId)
            }
        }
    }
    
    /**
     * Add plugin to dashboard
     */
    fun addToDashboard(plugin: Plugin) {
        viewModelScope.launch {
            try {
                // Get current dashboard plugin IDs
                val currentIds = preferencesManager.getDashboardPluginIds().toMutableSet()
                
                // Add new plugin ID
                currentIds.add(plugin.id)
                
                // Save updated list
                preferencesManager.setDashboardPluginIds(currentIds)
                
                // Update UI state
                val updatedDashboardPlugins = _uiState.value.dashboardPlugins + plugin
                _uiState.update { it.copy(dashboardPlugins = updatedDashboardPlugins) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to add plugin to dashboard") }
            }
        }
    }
    
    /**
     * Remove plugin from dashboard
     */
    fun removeFromDashboard(pluginId: String) {
        viewModelScope.launch {
            try {
                // Get current dashboard plugin IDs
                val currentIds = preferencesManager.getDashboardPluginIds().toMutableSet()
                
                // Remove plugin ID
                currentIds.remove(pluginId)
                
                // Save updated list
                preferencesManager.setDashboardPluginIds(currentIds)
                
                // Update UI state
                val updatedDashboardPlugins = _uiState.value.dashboardPlugins.filter { it.id != pluginId }
                _uiState.update { it.copy(dashboardPlugins = updatedDashboardPlugins) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to remove plugin from dashboard") }
            }
        }
    }
    
    /**
     * Refresh dashboard data
     */
    fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        loadDashboard()
        _uiState.update { it.copy(isRefreshing = false) }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * Dashboard UI state
 */
data class DashboardUiState(
    val plugins: List<Plugin> = emptyList(),
    val dashboardPlugins: List<Plugin> = emptyList(),
    val pluginStates: Map<String, PluginState> = emptyMap(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

/**
 * Plugin state information
 */
data class PluginState(
    val pluginId: String,
    val isEnabled: Boolean = false,
    val isCollecting: Boolean = false,
    val lastCollectionTime: Long? = null,
    val dataCount: Int = 0
)
