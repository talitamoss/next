package com.domain.app.ui.dashboard

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
    }
    
    private fun loadPlugins() {
        viewModelScope.launch {
            pluginManager.initializePlugins()
            
            val allPlugins = pluginManager.getAllActivePlugins()
            _uiState.update {
                it.copy(allPlugins = allPlugins)
            }
        }
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
                _uiState.update {
                    it.copy(
                        pluginStates = states,
                        activePluginCount = states.values.count { state -> state.isCollecting }
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
    
    private fun observeEvents() {
        EventBus.events
            .filterIsInstance<Event.DataCollected>()
            .onEach { event ->
                _uiState.update {
                    it.copy(
                        lastDataPoint = event.dataPoint,
                        showSuccessFeedback = true
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun onPluginTileClick(plugin: Plugin) {
        viewModelScope.launch {
            val hasPermissions = permissionManager.hasPermission(
                plugin.id,
                PluginCapability.COLLECT_DATA
            )
            
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
    
    // THIS IS THE MISSING METHOD THAT NEEDS TO BE ADDED
    fun addPluginToDashboard(pluginId: String) {
        viewModelScope.launch {
            preferencesManager.addToDashboard(pluginId)
        }
    }
    
    fun onQuickAdd(plugin: Plugin, data: Map<String, Any>) {
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
    val showSuccessFeedback: Boolean = false
)
