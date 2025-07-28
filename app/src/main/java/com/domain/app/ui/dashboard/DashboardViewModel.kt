package com.domain.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataRepository
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
    
    init {
        loadPlugins()
        observePluginStates()
        loadDataCounts()
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
    
    fun onPluginTileClick(plugin: Plugin) {
        viewModelScope.launch {
            if (plugin.isCollecting()) {
                pluginManager.disablePlugin(plugin.id)
            } else {
                pluginManager.enablePlugin(plugin.id)
            }
        }
    }
    
    fun onQuickAdd(plugin: Plugin) {
        viewModelScope.launch {
            when (plugin.id) {
                "water" -> {
                    pluginManager.createManualEntry(
                        plugin.id,
                        mapOf("amount" to 250, "unit" to "ml")
                    )
                }
                "counter" -> {
                    pluginManager.createManualEntry(
                        plugin.id,
                        mapOf("label" to "Item")
                    )
                }
                // Add more plugin-specific quick add logic here
            }
        }
    }
}

data class DashboardUiState(
    val plugins: List<Plugin> = emptyList(),
    val manualEntryPlugins: List<Plugin> = emptyList(),
    val pluginStates: Map<String, PluginState> = emptyMap(),
    val todayEntryCount: Int = 0,
    val weekEntryCount: Int = 0,
    val activePluginCount: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)
