package com.domain.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
import com.domain.app.core.event.Event
import com.domain.app.core.event.EventBus
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.PluginState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val pluginManager: PluginManager,
    private val dataRepository: DataRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    private val _selectedPlugin = MutableStateFlow<Plugin?>(null)
    val selectedPlugin: StateFlow<Plugin?> = _selectedPlugin.asStateFlow()
    
    init {
        loadPlugins()
        observePluginStates()
        loadDataCounts()
        observeEvents()
    }
    
    private fun loadPlugins() {
        viewModelScope.launch {
            pluginManager.initializePlugins()
            
            val allPlugins = pluginManager.getAllActivePlugins()
            val manualEntryPlugins = allPlugins.filter { it.supportsManualEntry() }
            
            _uiState.update {
                it.copy(
                    plugins = allPlugins,
                    manualEntryPlugins = manualEntryPlugins
                )
            }
        }
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
    
    private fun loadDataCounts() {
        viewModelScope.launch {
            // Today's count
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
            // This week's count
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
                // Show success feedback
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
        if (plugin.supportsManualEntry()) {
            _selectedPlugin.value = plugin
            _uiState.update { it.copy(showQuickAdd = true) }
        }
    }
    
    fun onQuickAdd(plugin: Plugin, data: Map<String, Any>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            
            try {
                val dataPoint = pluginManager.createManualEntry(plugin.id, data)
                if (dataPoint != null) {
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
        _uiState.update { it.copy(showQuickAdd = false) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun clearSuccessFeedback() {
        _uiState.update { it.copy(showSuccessFeedback = false) }
    }
}

data class DashboardUiState(
    val plugins: List<Plugin> = emptyList(),
    val manualEntryPlugins: List<Plugin> = emptyList(),
    val pluginStates: Map<String, PluginState> = emptyMap(),
    val todayEntryCount: Int = 0,
    val weekEntryCount: Int = 0,
    val activePluginCount: Int = 0,
    val showQuickAdd: Boolean = false,
    val isProcessing: Boolean = false,
    val error: String? = null,
    val lastDataPoint: com.domain.app.core.data.DataPoint? = null,
    val showSuccessFeedback: Boolean = false
)
