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
        // For MVP, we'll show these plugins by default
        private val MVP_PLUGIN_IDS = listOf("water", "sleep", "movement", "work", "caffeine", "alcohol", "screen_time", "social_time")
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
     * Initialize the MVP dashboard with hardcoded plugins for testing
     * In production, this would read from user preferences
     */
    private fun initializeMVPDashboard() {
        Log.d(TAG, "initializeMVPDashboard() called")
        viewModelScope.launch {
            try {
                // Initialize plugin system
                Log.d(TAG, "Initializing plugins...")
                pluginManager.initializePlugins()
                
                // Get all available plugins
                val allPlugins = pluginManager.getAllActivePlugins()
                Log.d(TAG, "Loaded ${allPlugins.size} active plugins: ${allPlugins.map { it.id }}")
                
                // For MVP, filter to show only our enabled plugins
                val dashboardPlugins = allPlugins.filter { plugin ->
                    plugin.id in MVP_PLUGIN_IDS
                }
                
                Log.d(TAG, "Dashboard plugins: ${dashboardPlugins.map { it.id }}")
                
                // Update UI state
                _uiState.update {
                    it.copy(
                        allPlugins = allPlugins,
                        dashboardPlugins = dashboardPlugins
                    )
                }
                
                // Ensure MVP plugins are added to dashboard preferences
                // This maintains consistency even though we're hardcoding for MVP
                ensurePluginsInPreferences(dashboardPlugins)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing MVP dashboard", e)
                _uiState.update {
                    it.copy(error = "Failed to load plugins: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Ensure plugins are registered in preferences
     * This maintains data consistency even in MVP mode
     */
    private suspend fun ensurePluginsInPreferences(plugins: List<Plugin>) {
        val currentDashboardIds = preferencesManager.dashboardPlugins.first()
        
        plugins.forEach { plugin ->
            if (plugin.id !in currentDashboardIds) {
                Log.d(TAG, "Adding ${plugin.id} plugin to dashboard preferences")
                preferencesManager.addToDashboard(plugin.id)
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
     * Add a plugin to the dashboard (for future use when we enable dynamic dashboard)
     */
    fun addPluginToDashboard(pluginId: String) {
        Log.d(TAG, "Adding plugin $pluginId to dashboard")
        viewModelScope.launch {
            try {
                val plugin = pluginManager.getPlugin(pluginId)
                if (plugin != null) {
                    preferencesManager.addToDashboard(pluginId)
                    
                    // Update local state
                    _uiState.update { state ->
                        state.copy(
                            dashboardPlugins = state.dashboardPlugins + plugin
                        )
                    }
                    
                    Log.d(TAG, "Successfully added $pluginId to dashboard")
                } else {
                    Log.e(TAG, "Plugin $pluginId not found")
                    _uiState.update {
                        it.copy(error = "Plugin not found")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding plugin to dashboard", e)
                _uiState.update {
                    it.copy(error = "Failed to add plugin: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Remove a plugin from the dashboard (for future use)
     */
    fun removePluginFromDashboard(pluginId: String) {
        Log.d(TAG, "Removing plugin $pluginId from dashboard")
        viewModelScope.launch {
            try {
                preferencesManager.removeFromDashboard(pluginId)
                
                // Update local state
                _uiState.update { state ->
                    state.copy(
                        dashboardPlugins = state.dashboardPlugins.filter { it.id != pluginId }
                    )
                }
                
                Log.d(TAG, "Successfully removed $pluginId from dashboard")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing plugin from dashboard", e)
                _uiState.update {
                    it.copy(error = "Failed to remove plugin: ${e.message}")
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
