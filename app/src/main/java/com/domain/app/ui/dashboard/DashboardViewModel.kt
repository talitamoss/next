package com.domain.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.PluginPermissionManager
import com.domain.app.core.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Dashboard screen
 * 
 * File location: app/src/main/java/com/domain/app/ui/dashboard/DashboardViewModel.kt
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val dataRepository: DataRepository,
    private val preferencesManager: PreferencesManager,
    private val permissionManager: PluginPermissionManager
) : ViewModel() {
    
    // UI State
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    // Selected plugin for quick add
    private val _selectedPlugin = MutableStateFlow<Plugin?>(null)
    val selectedPlugin: StateFlow<Plugin?> = _selectedPlugin.asStateFlow()
    
    init {
        loadDashboardData()
        loadDashboardPlugins()
        loadPluginStates()
    }
    
    private fun loadDashboardPlugins() {
        viewModelScope.launch {
            combine(
                preferencesManager.dashboardPlugins,
                pluginManager.pluginStates
            ) { dashboardIds, pluginStates ->
                dashboardIds.mapNotNull { pluginId ->
                    pluginStates[pluginId]?.plugin
                }
            }.collect { plugins ->
                _uiState.update { it.copy(dashboardPlugins = plugins) }
            }
        }
    }
    
    private fun loadPluginStates() {
        viewModelScope.launch {
            pluginManager.pluginStates.collect { states ->
                val permissions = states.mapValues { (pluginId, _) ->
                    permissionManager.getGrantedPermissions(pluginId)
                }
                
                _uiState.update { 
                    it.copy(
                        pluginStates = states,
                        pluginPermissions = permissions,
                        activePluginCount = states.count { it.value.isEnabled }
                    )
                }
            }
        }
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            // Load entry counts
            val now = System.currentTimeMillis()
            val startOfDay = now - (now % (24 * 60 * 60 * 1000))
            val startOfWeek = now - (7 * 24 * 60 * 60 * 1000)
            
            dataRepository.getDataInTimeRange(startOfDay, now).collect { todayData ->
                _uiState.update { it.copy(todayEntryCount = todayData.size) }
            }
            
            dataRepository.getDataInTimeRange(startOfWeek, now).collect { weekData ->
                _uiState.update { it.copy(weekEntryCount = weekData.size) }
            }
        }
        
        // Load all plugins for selector
        viewModelScope.launch {
            val allPlugins = pluginManager.getAllPlugins()
            _uiState.update { it.copy(allPlugins = allPlugins) }
        }
    }
    
    fun onPluginTileClick(plugin: Plugin) {
        viewModelScope.launch {
            val hasPermission = permissionManager.hasPermission(
                plugin.id,
                PluginCapability.COLLECT_DATA
            )
            
            if (hasPermission) {
                _selectedPlugin.value = plugin
                _uiState.update { it.copy(showQuickAdd = true, needsPermission = false) }
            } else {
                _selectedPlugin.value = plugin
                _uiState.update { it.copy(showQuickAdd = true, needsPermission = true) }
            }
        }
    }
    
    fun onAddPluginClick() {
        _uiState.update { it.copy(showPluginSelector = true) }
    }
    
    fun dismissPluginSelector() {
        _uiState.update { it.copy(showPluginSelector = false) }
    }
    
    fun grantQuickAddPermission() {
        viewModelScope.launch {
            _selectedPlugin.value?.let { plugin ->
                permissionManager.grantPermission(plugin.id, PluginCapability.COLLECT_DATA)
                _uiState.update { it.copy(needsPermission = false) }
            }
        }
    }
    
    fun dismissQuickAdd() {
        _selectedPlugin.value = null
        _uiState.update { it.copy(showQuickAdd = false, needsPermission = false) }
    }
    
    fun onQuickAdd(plugin: Plugin, data: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val dataPoint = plugin.createManualEntry(data)
                if (dataPoint != null) {
                    dataRepository.insertData(dataPoint)
                    _uiState.update { 
                        it.copy(
                            showQuickAdd = false,
                            showSuccessFeedback = true
                        )
                    }
                    _selectedPlugin.value = null
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = "Failed to add data: ${e.message}")
                }
            }
        }
    }
    
    fun clearSuccessFeedback() {
        _uiState.update { it.copy(showSuccessFeedback = false) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
