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
    
    private val _selectedPlugin = MutableStateFlow<Plugin?>(null)
    val selectedPlugin: StateFlow<Plugin?> = _selectedPlugin.asStateFlow()
    
    init {
        Log.d(TAG, "Initializing DashboardViewModel")
        loadPlugins()
        observePluginStates()
        observeDashboardPlugins()
        observePluginPermissions()
        loadDataCounts()
        observeEvents()
        calculateStreak()
        loadPluginDataCounts()
    }
    
    private fun loadPlugins() {
        Log.d(TAG, "loadPlugins() called")
        viewModelScope.launch {
            try {
                Log.d(TAG, "Initializing plugins...")
                pluginManager.initializePlugins()
                
                val allPlugins = pluginManager.getAllActivePlugins()
                Log.d(TAG, "Loaded ${allPlugins.size} active plugins: ${allPlugins.map { it.id }}")
                
                _uiState.update {
                    it.copy(allPlugins = allPlugins)
                }
                
                // Debug: Check what's in preferences
                preferencesManager.dashboardPlugins.collect { dashboardIds ->
                    Log.d(TAG, "Dashboard plugin IDs from preferences: $dashboardIds")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading plugins", e)
            }
        }
    }
    
    private fun observeDashboardPlugins() {
        Log.d(TAG, "observeDashboardPlugins() setting up")
        
        // Combine dashboard plugin IDs with actual plugin instances
        combine(
            preferencesManager.dashboardPlugins,
            _uiState.map { it.allPlugins }
        ) { dashboardIds, allPlugins ->
            Log.d(TAG, "Combining dashboard IDs: $dashboardIds with ${allPlugins.size} plugins")
            
            val dashboardPlugins = dashboardIds.mapNotNull { id ->
                val plugin = allPlugins.find { it.id == id }
                if (plugin == null) {
                    Log.w(TAG, "Plugin with ID $id not found in active plugins")
                }
                plugin
            }
            
            Log.d(TAG, "Mapped to ${dashboardPlugins.size} dashboard plugins: ${dashboardPlugins.map { it.id }}")
            dashboardPlugins
        }
        .onEach { dashboardPlugins ->
            Log.d(TAG, "Updating UI state with ${dashboardPlugins.size} dashboard plugins")
            _uiState.update {
                it.copy(dashboardPlugins = dashboardPlugins)
            }
        }
        .launchIn(viewModelScope)
        
        // Track dashboard count
        preferencesManager.getDashboardPluginCount()
            .onEach { count ->
                Log.d(TAG, "Dashboard plugin count: $count")
                _uiState.update {
                    it.copy(
                        dashboardPluginCount = count,
                        canAddMorePlugins = count < 6
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    private fun observePluginStates() {
        pluginManager.pluginStates
            .onEach { states ->
                Log.d(TAG, "Plugin states updated: ${states.size} states")
                _uiState.update {
                    it.copy(
                        pluginStates = states,
                        activePluginCount = states.values.count { state -> state.isCollecting }
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    private fun observePluginPermissions() {
        // Reactively observe permission changes for all dashboard plugins
        combine(
            _uiState.map { it.allPlugins },
            _uiState.map { it.allPlugins }.flatMapLatest { plugins ->
                if (plugins.isEmpty()) {
                    flowOf(emptyMap<String, Boolean>())
                } else {
                    combine(
                        plugins.map { plugin ->
                            permissionManager.getGrantedPermissionsFlow(plugin.id)
                                .map { grantedPermissions ->
                                    plugin.id to grantedPermissions.contains(PluginCapability.COLLECT_DATA)
                                }
                        }
                    ) { permissionPairs ->
                        permissionPairs.toMap()
                    }
                }
            }
        ) { _, permissionMap ->
            permissionMap
        }
        .onEach { permissionMap ->
            Log.d(TAG, "Permission map updated: $permissionMap")
            _uiState.update {
                it.copy(pluginPermissions = permissionMap)
            }
        }
        .launchIn(viewModelScope)
    }
    
    private fun loadDataCounts() {
        viewModelScope.launch {
            val todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS)
            dataRepository.getRecentData(24)
                .map { dataPoints ->
                    dataPoints.count { it.timestamp.isAfter(todayStart) }
                }
                .collect { count ->
                    _uiState.update { it.copy(todayEntryCount = count) }
                }
        }
        
        viewModelScope.launch {
            val weekStart = Instant.now().minus(7, ChronoUnit.DAYS)
            dataRepository.getRecentData(24 * 7)
                .map { dataPoints ->
                    dataPoints.count { it.timestamp.isAfter(weekStart) }
                }
                .collect { count ->
                    _uiState.update { it.copy(weekEntryCount = count) }
                }
        }
    }
    
    private fun calculateStreak() {
        viewModelScope.launch {
            // Calculate consecutive days with data
            dataRepository.getRecentData(365)
                .map { dataPoints ->
                    if (dataPoints.isEmpty()) return@map 0
                    
                    val dayGroups = dataPoints.groupBy { dataPoint ->
                        dataPoint.timestamp.truncatedTo(ChronoUnit.DAYS)
                    }
                    
                    var streak = 0
                    var currentDate = Instant.now().truncatedTo(ChronoUnit.DAYS)
                    
                    while (dayGroups.containsKey(currentDate)) {
                        streak++
                        currentDate = currentDate.minus(1, ChronoUnit.DAYS)
                    }
                    
                    streak
                }
                .collect { streak ->
                    _uiState.update { it.copy(currentStreak = streak) }
                }
        }
    }
    
    private fun loadPluginDataCounts() {
        viewModelScope.launch {
            // Get today's data counts for each plugin
            val todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS)
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
                calculateStreak()
            }
            .launchIn(viewModelScope)
    }
    
    fun onPluginTileClick(plugin: Plugin) {
        Log.d(TAG, "Plugin tile clicked: ${plugin.id}")
        viewModelScope.launch {
            val hasPermissions = permissionManager.hasPermission(
                plugin.id,
                PluginCapability.COLLECT_DATA
            )
            
            Log.d(TAG, "Plugin ${plugin.id} has permissions: $hasPermissions")
            
            if (!hasPermissions) {
                _selectedPlugin.value = plugin
                _uiState.update { 
                    it.copy(
                        showQuickAdd = true,
                        needsPermission = true
                    )
                }
            } else if (plugin.supportsManualEntry()) {
                _selectedPlugin.value = plugin
                _uiState.update { 
                    it.copy(
                        showQuickAdd = true,
                        needsPermission = false
                    )
                }
            }
        }
    }
    
    fun grantQuickAddPermission() {
        viewModelScope.launch {
            val plugin = _selectedPlugin.value ?: return@launch
            
            Log.d(TAG, "Granting permissions for plugin: ${plugin.id}")
            
            permissionManager.grantPermissions(
                pluginId = plugin.id,
                permissions = plugin.securityManifest.requestedCapabilities,
                grantedBy = "user_quick_add"
            )
            
            // Continue with quick add - permissions will be updated reactively
            _uiState.update {
                it.copy(needsPermission = false)
            }
        }
    }
    
    fun onAddPluginClick() {
        Log.d(TAG, "Add plugin button clicked")
        _uiState.update { it.copy(showPluginSelector = true) }
    }
    
    fun dismissPluginSelector() {
        Log.d(TAG, "Dismissing plugin selector")
        _uiState.update { it.copy(showPluginSelector = false) }
    }
    
    fun addPluginToDashboard(pluginId: String) {
        Log.d(TAG, "=== ADD PLUGIN TO DASHBOARD CALLED ===")
        Log.d(TAG, "Plugin ID to add: $pluginId")
        
        viewModelScope.launch {
            try {
                // Check current state
                val currentState = _uiState.value
                Log.d(TAG, "Current all plugins: ${currentState.allPlugins.map { it.id }}")
                Log.d(TAG, "Current dashboard plugins: ${currentState.dashboardPlugins.map { it.id }}")
                
                // Verify plugin exists
                val pluginExists = currentState.allPlugins.any { it.id == pluginId }
                Log.d(TAG, "Plugin $pluginId exists in all plugins: $pluginExists")
                
                if (!pluginExists) {
                    Log.e(TAG, "ERROR: Plugin $pluginId not found in active plugins!")
                    return@launch
                }
                
                // Check if already on dashboard
                val alreadyOnDashboard = currentState.dashboardPlugins.any { it.id == pluginId }
                Log.d(TAG, "Plugin $pluginId already on dashboard: $alreadyOnDashboard")
                
                if (alreadyOnDashboard) {
                    Log.w(TAG, "Plugin $pluginId is already on dashboard, skipping")
                    return@launch
                }
                
                // Add to preferences
                Log.d(TAG, "Adding plugin $pluginId to preferences...")
                preferencesManager.addToDashboard(pluginId)
                Log.d(TAG, "Successfully added $pluginId to preferences")
                
                // Verify it was added
                preferencesManager.dashboardPlugins.take(1).collect { ids ->
                    Log.d(TAG, "Dashboard IDs after adding: $ids")
                    if (pluginId in ids) {
                        Log.d(TAG, "SUCCESS: Plugin $pluginId is now in dashboard preferences")
                    } else {
                        Log.e(TAG, "ERROR: Plugin $pluginId was NOT added to preferences!")
                    }
                }
                
                // Refresh plugin data counts for the new plugin
                loadPluginDataCounts()
                
                Log.d(TAG, "=== ADD PLUGIN TO DASHBOARD COMPLETED ===")
            } catch (e: Exception) {
                Log.e(TAG, "Error adding plugin to dashboard", e)
            }
        }
    }
    
    fun onQuickAdd(plugin: Plugin, data: Map<String, Any>) {
        Log.d(TAG, "Quick add for plugin: ${plugin.id} with data: $data")
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            
            try {
                val dataPoint = pluginManager.createManualEntry(plugin.id, data)
                if (dataPoint != null) {
                    Log.d(TAG, "Created data point for plugin: ${plugin.id}")
                    _uiState.update { it.copy(showQuickAdd = false, isProcessing = false) }
                    // Success handled by event observer
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
    
    fun dismissQuickAdd() {
        Log.d(TAG, "Dismissing quick add")
        _selectedPlugin.value = null
        _uiState.update { 
            it.copy(
                showQuickAdd = false,
                needsPermission = false
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearSuccessFeedback() {
        _uiState.update { it.copy(showSuccessFeedback = false) }
    }
}

data class DashboardUiState(
    val allPlugins: List<Plugin> = emptyList(),
    val dashboardPlugins: List<Plugin> = emptyList(),
    val pluginStates: Map<String, PluginState> = emptyMap(),
    val pluginPermissions: Map<String, Boolean> = emptyMap(),
    val todayEntryCount: Int = 0,
    val weekEntryCount: Int = 0,
    val activePluginCount: Int = 0,
    val dashboardPluginCount: Int = 0,
    val canAddMorePlugins: Boolean = true,
    val showQuickAdd: Boolean = false,
    val showPluginSelector: Boolean = false,
    val needsPermission: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val lastDataPoint: com.domain.app.core.data.DataPoint? = null,
    val showSuccessFeedback: Boolean = false,
    val currentStreak: Int = 0,
    val pluginDataCounts: Map<String, Int> = emptyMap()
)
