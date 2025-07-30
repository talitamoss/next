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
    
    // Dashboard plugins
    val dashboardPlugins: Flow<List<Plugin>> = combine(
        preferencesManager.dashboardPlugins,
        pluginManager.pluginStates
    ) { dashboardIds, pluginStates ->
        dashboardIds.mapNotNull { pluginId ->
            pluginStates[pluginId]?.plugin
        }
    }
    
    init {
        loadDashboardData()
        monitorPluginStates()
    }
    
    private fun loadDashboardData() {
        viewModelScope.launch {
            // Load recent data points
            dataRepository.getAllData()
                .map { dataPoints ->
                    dataPoints
                        .sortedByDescending { it.timestamp }
                        .take(10)
                }
                .collect { recentData ->
                    _uiState.update { it.copy(recentDataPoints = recentData) }
                }
        }
    }
    
    private fun monitorPluginStates() {
        viewModelScope.launch {
            pluginManager.pluginStates.collect { states ->
                _uiState.update { 
                    it.copy(
                        activePluginsCount = states.count { it.value.isEnabled },
                        collectingPluginsCount = states.count { it.value.isCollecting }
                    )
                }
            }
        }
    }
    
    fun refreshDashboard() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            
            // Reload data
            loadDashboardData()
            
            // Update last refresh time
            _uiState.update { 
                it.copy(
                    isRefreshing = false,
                    lastRefreshTime = System.currentTimeMillis()
                )
            }
        }
    }
    
    fun quickAddData(pluginId: String, value: Any) {
        viewModelScope.launch {
            val plugin = pluginManager.getPlugin(pluginId) ?: return@launch
            
            // Check permissions
            val hasPermission = permissionManager.hasPermission(
                pluginId, 
                PluginCapability.COLLECT_DATA
            )
            
            if (!hasPermission) {
                _uiState.update { 
                    it.copy(
                        errorMessage = "Plugin lacks permission to collect data"
                    )
                }
                return@launch
            }
            
            // Create data point
            val dataPoint = plugin.createManualEntry(
                mapOf("value" to value)
            )
            
            if (dataPoint != null) {
                dataRepository.insertData(dataPoint)
                
                _uiState.update { 
                    it.copy(
                        successMessage = "Data added successfully"
                    )
                }
            }
        }
    }
    
    fun removeFromDashboard(pluginId: String) {
        viewModelScope.launch {
            preferencesManager.removeFromDashboard(pluginId)
        }
    }
    
    fun navigateToPlugin(pluginId: String) {
        _uiState.update { 
            it.copy(
                navigationEvent = NavigationEvent.ToPlugin(pluginId)
            )
        }
    }
    
    fun clearNavigation() {
        _uiState.update { 
            it.copy(navigationEvent = null)
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun dismissSuccess() {
        _uiState.update { it.copy(successMessage = null) }
    }
}

/**
 * UI state for the Dashboard
 */
data class DashboardUiState(
    val activePluginsCount: Int = 0,
    val collectingPluginsCount: Int = 0,
    val recentDataPoints: List<com.domain.app.core.data.DataPoint> = emptyList(),
    val isRefreshing: Boolean = false,
    val lastRefreshTime: Long? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val navigationEvent: NavigationEvent? = null
)

/**
 * Navigation events from the Dashboard
 */
sealed class NavigationEvent {
    data class ToPlugin(val pluginId: String) : NavigationEvent()
    object ToSettings : NavigationEvent()
    object ToData : NavigationEvent()
}
