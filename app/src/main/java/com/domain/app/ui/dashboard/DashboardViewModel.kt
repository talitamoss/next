package com.domain.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataPoint
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
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val dataRepository: DataRepository,
    private val preferencesManager: PreferencesManager,
    private val permissionManager: PluginPermissionManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val _selectedPlugin = MutableStateFlow<Plugin?>(null)
    val selectedPlugin: StateFlow<Plugin?> = _selectedPlugin.asStateFlow()
    
    init {
        loadPlugins()
        observePluginStates()
        observeDashboardPlugins()
        observePluginPermissions()
        loadDataCounts()
        observeEvents()
        calculateStreak()
    }
    
    private fun loadPlugins() {
        viewModelScope.launch {
            pluginManager.initializePlugins()
            
            val allPlugins = pluginManager.getAllActivePlugins()
            _uiState.update { state ->
                state.copy(
                    allPlugins = allPlugins,
                    availablePlugins = allPlugins, // All plugins are available by default
                    activePlugins = allPlugins.filter { plugin ->
                        pluginManager.pluginStates.value[plugin.id]?.isCollecting == true
                    }
                )
            }
            
            // Update plugin data counts
            updatePluginDataCounts(allPlugins)
        }
    }
    
    private fun updatePluginDataCounts(plugins: List<Plugin>) {
        viewModelScope.launch {
            val counts = mutableMapOf<String, Int>()
            val todayStart = Instant.now().truncatedTo(ChronoUnit.DAYS)
            
            plugins.forEach { plugin ->
                // Fixed: getPluginData only takes pluginId parameter
                dataRepository.getPluginData(plugin.id)
                    .take(1)
                    .collect { dataPoints ->
                        counts[plugin.id] = dataPoints.count { 
                            it.timestamp.isAfter(todayStart) 
                        }
                    }
            }
            
            _uiState.update { it.copy(pluginDataCounts = counts) }
        }
    }
    
    private fun calculateStreak() {
        viewModelScope.launch {
            // Fixed: Method is actually getAllDataPoints()
            dataRepository.getAllDataPoints()
                .take(1)
                .collect { allData ->
                    val streak = calculateDataStreak(allData)
                    _uiState.update { it.copy(currentStreak = streak) }
                }
        }
    }
    
    private fun calculateDataStreak(dataPoints: List<DataPoint>): Int {
        if (dataPoints.isEmpty()) return 0
        
        val zone = ZoneId.systemDefault()
        val dates = dataPoints.map { 
            it.timestamp.atZone(zone).toLocalDate() 
        }.toSet().sorted().reversed()
        
        if (dates.isEmpty()) return 0
        
        val today = LocalDate.now()
        var streak = 0
        var currentDate = today
        
        for (date in dates) {
            if (date == currentDate || date == currentDate.minusDays(1)) {
                streak++
                currentDate = date
            } else if (date.isBefore(currentDate.minusDays(1))) {
                break
            }
        }
        
        return streak
    }
    
    private fun observeDashboardPlugins() {
        // Combine dashboard plugin IDs with actual plugin instances
        combine(
            preferencesManager.dashboardPlugins,
            _uiState.map { it.allPlugins }
        ) { dashboardIds, allPlugins ->
            dashboardIds.mapNotNull { id ->
                allPlugins.find { it.id == id }
            }
        }
        .onEach { dashboardPlugins ->
            _uiState.update {
                it.copy(dashboardPlugins = dashboardPlugins)
            }
        }
        .launchIn(viewModelScope)
        
        // Track dashboard count
        preferencesManager.getDashboardPluginCount()
            .onEach { count ->
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
                _uiState.update { currentState ->
                    currentState.copy(
                        pluginStates = states,
                        activePluginCount = states.values.count { state -> state.isCollecting },
                        activePlugins = currentState.allPlugins.filter { plugin ->
                            states[plugin.id]?.isCollecting == true
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
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
                .onEach { dataPoints ->
                    val count = dataPoints.count { it.timestamp.isAfter(todayStart) }
                    _uiState.update { it.copy(todayDataCount = count) }
                }
                .launchIn(viewModelScope)
        }
        
        viewModelScope.launch {
            val weekStart = Instant.now().minus(7, ChronoUnit.DAYS)
            dataRepository.getRecentData(24 * 7)
                .onEach { dataPoints ->
                    val count = dataPoints.count { it.timestamp.isAfter(weekStart) }
                    _uiState.update { it.copy(weekEntryCount = count) }
                }
                .launchIn(viewModelScope)
        }
    }
    
    private fun observeEvents() {
        // Note: EventBus would need to be injected or made available as a singleton
        // For now, using static access pattern common in event bus implementations
        // If EventBus requires injection, add it to constructor parameters
        
        // Commenting out until EventBus is properly configured
        /*
        eventBus.events
            .filterIsInstance<Event.DataCollected>()
            .onEach { event ->
                // Update counts when new data is collected
                updatePluginDataCounts(_uiState.value.allPlugins)
                loadDataCounts()
                calculateStreak()
                
                // Show success feedback
                _uiState.update { 
                    it.copy(
                        lastDataPoint = event.dataPoint,
                        showSuccessFeedback = true
                    )
                }
            }
            .launchIn(viewModelScope)
        */
        
        // TODO: Implement event observation when EventBus is configured
    }
    
    fun selectPlugin(plugin: Plugin) {
        _selectedPlugin.value = plugin
        
        // Check if plugin needs permissions
        viewModelScope.launch {
            val hasPermission = permissionManager.hasPermission(
                plugin.id,
                PluginCapability.COLLECT_DATA
            )
            
            _uiState.update {
                it.copy(
                    showQuickAdd = true,
                    needsPermission = !hasPermission
                )
            }
        }
    }
    
    fun grantPermissionAndContinue() {
        viewModelScope.launch {
            val plugin = _selectedPlugin.value ?: return@launch
            
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
        _uiState.update { it.copy(showPluginSelector = true) }
    }
    
    fun dismissPluginSelector() {
        _uiState.update { it.copy(showPluginSelector = false) }
    }
    
    fun addPluginToDashboard(pluginId: String) {
        viewModelScope.launch {
            preferencesManager.addToDashboard(pluginId)
        }
    }
    
    fun quickAddData(plugin: Plugin, data: Map<String, Any>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            
            try {
                val dataPoint = pluginManager.createManualEntry(plugin.id, data)
                if (dataPoint != null) {
                    _uiState.update { it.copy(showQuickAdd = false) }
                    // Success handled by event observer
                } else {
                    _uiState.update {
                        it.copy(
                            error = "Failed to create entry",
                            isProcessing = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message,
                        isProcessing = false
                    )
                }
            }
        }
    }
    
    fun onQuickAdd(plugin: Plugin, data: Map<String, Any>) {
        quickAddData(plugin, data)
    }
    
    fun dismissQuickAdd() {
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
    // All plugins in the system
    val allPlugins: List<Plugin> = emptyList(),
    
    // Plugins shown on dashboard
    val dashboardPlugins: List<Plugin> = emptyList(),
    
    // Available plugins (can be added to dashboard)
    val availablePlugins: List<Plugin> = emptyList(),
    
    // Currently active (collecting) plugins
    val activePlugins: List<Plugin> = emptyList(),
    
    // Plugin states map
    val pluginStates: Map<String, PluginState> = emptyMap(),
    
    // Plugin permissions map
    val pluginPermissions: Map<String, Boolean> = emptyMap(),
    
    // Data counts per plugin
    val pluginDataCounts: Map<String, Int> = emptyMap(),
    
    // Today's stats
    val todayDataCount: Int = 0,
    val todayEntryCount: Int = 0,
    
    // Week stats
    val weekEntryCount: Int = 0,
    
    // Streak tracking
    val currentStreak: Int = 0,
    
    // Plugin counts
    val activePluginCount: Int = 0,
    val dashboardPluginCount: Int = 0,
    
    // UI state flags
    val canAddMorePlugins: Boolean = true,
    val showQuickAdd: Boolean = false,
    val showPluginSelector: Boolean = false,
    val needsPermission: Boolean = false,
    val isProcessing: Boolean = false,
    val showSuccessFeedback: Boolean = false,
    
    // Error handling
    val error: String? = null,
    
    // Last action feedback
    val lastDataPoint: DataPoint? = null
)
