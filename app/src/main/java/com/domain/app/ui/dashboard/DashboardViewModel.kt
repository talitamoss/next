package com.domain.app.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.event.Event
import com.domain.app.core.event.EventBus
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.PluginState
import com.domain.app.core.plugin.security.PluginPermissionManager
import com.domain.app.core.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val dataRepository: DataRepository,
    private val preferencesManager: PreferencesManager,
    private val permissionManager: PluginPermissionManager
) : ViewModel() {
    
    companion object {
        private const val TAG = "DashboardViewModel"
    }
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        Log.d(TAG, "Initializing DashboardViewModel")
        initializeMVPDashboard()
        loadPluginDataCounts()
        observeEvents()
    }
    
    /**
     * Initialize the MVP dashboard with just the Water plugin
     */
    private fun initializeMVPDashboard() {
        Log.d(TAG, "initializeMVPDashboard() called")
        viewModelScope.launch {
            try {
                // Initialize plugin system
                Log.d(TAG, "Initializing plugins...")
                pluginManager.initializePlugins()
                
                // Get all available plugins (should just be Water for now)
                val allPlugins = pluginManager.getAllActivePlugins()
                Log.d(TAG, "Loaded ${allPlugins.size} active plugins: ${allPlugins.map { it.id }}")
                
                // For MVP, we'll just show Water plugin if available
                val waterPlugin = allPlugins.find { it.id == "water" }
                val dashboardPlugins = if (waterPlugin != null) {
                    listOf(waterPlugin)
                } else {
                    emptyList()
                }
                
                Log.d(TAG, "Dashboard plugins: ${dashboardPlugins.map { it.id }}")
                
                // Update UI state
                _uiState.update {
                    it.copy(
                        allPlugins = allPlugins,
                        dashboardPlugins = dashboardPlugins
                    )
                }
                
                // Ensure Water plugin is added to dashboard preferences
                if (waterPlugin != null) {
                    viewModelScope.launch {
                        // Check if already on dashboard
                        val currentDashboardIds = preferencesManager.dashboardPlugins.first()
                        if (waterPlugin.id !in currentDashboardIds) {
                            Log.d(TAG, "Adding Water plugin to dashboard preferences")
                            preferencesManager.addToDashboard(waterPlugin.id)
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing MVP dashboard", e)
                _uiState.update {
                    it.copy(error = "Failed to load plugins: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Load data counts for dashboard plugins
     */
    private fun loadPluginDataCounts() {
        viewModelScope.launch {
            val todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS)
            
            // Get today's data counts for each plugin
            dataRepository.getRecentData(24)
                .map { dataPoints ->
                    dataPoints
                        .filter { it.timestamp.isAfter(todayStart) }
                        .groupBy { it.pluginId }
                        .mapValues { it.value.size }
                }
                .collect { counts ->
                    Log.d(TAG, "Plugin data counts: $counts")
                    _uiState.update { it.copy(pluginDataCounts = counts) }
                }
        }
    }
    
    /**
     * Observe events from the event bus
     */
    private fun observeEvents() {
        EventBus.events
            .filterIsInstance<Event.DataCollected>()
            .onEach { event ->
                Log.d(TAG, "Data collected event: ${event.dataPoint.pluginId}")
                _uiState.update {
                    it.copy(
                        lastDataPoint = event.dataPoint,
                        showSuccessFeedback = true
                    )
                }
                // Refresh counts when new data is collected
                loadPluginDataCounts()
            }
            .launchIn(viewModelScope)
    }
    
    /**
     * Handle quick add for a plugin
     */
    fun onQuickAdd(plugin: Plugin, data: Map<String, Any>) {
        Log.d(TAG, "Quick add for plugin: ${plugin.id} with data: $data")
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            
            try {
                // Check permissions first
                val hasPermissions = permissionManager.hasPermission(
                    plugin.id,
                    PluginCapability.COLLECT_DATA
                )
                
                if (!hasPermissions) {
                    Log.d(TAG, "Granting permissions for plugin: ${plugin.id}")
                    permissionManager.grantPermissions(
                        pluginId = plugin.id,
                        permissions = plugin.securityManifest.requestedCapabilities,
                        grantedBy = "user_quick_add"
                    )
                }
                
                // Create the data point
                val dataPoint = pluginManager.createManualEntry(plugin.id, data)
                if (dataPoint != null) {
                    Log.d(TAG, "Created data point for plugin: ${plugin.id}")
                    _uiState.update { 
                        it.copy(
                            isProcessing = false,
                            showSuccessFeedback = true,
                            lastDataPoint = dataPoint
                        )
                    }
                } else {
                    Log.e(TAG, "Failed to create data point for plugin: ${plugin.id}")
                    _uiState.update {
                        it.copy(
                            error = "Failed to create entry",
                            isProcessing = false
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating manual entry", e)
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isProcessing = false
                    )
                }
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    /**
     * Clear success feedback
     */
    fun clearSuccessFeedback() {
        _uiState.update { it.copy(showSuccessFeedback = false) }
    }
}

/**
 * UI state for the dashboard
 */
data class DashboardUiState(
    val allPlugins: List<Plugin> = emptyList(),
    val dashboardPlugins: List<Plugin> = emptyList(),
    val pluginDataCounts: Map<String, Int> = emptyMap(),
    val isProcessing: Boolean = false,
    val error: String? = null,
    val lastDataPoint: com.domain.app.core.data.DataPoint? = null,
    val showSuccessFeedback: Boolean = false
)
