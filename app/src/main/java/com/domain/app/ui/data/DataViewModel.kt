package com.domain.app.ui.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.PluginManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val pluginManager: PluginManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataUiState())
    val uiState: StateFlow<DataUiState> = _uiState.asStateFlow()
    
    private val selectedPluginFilter = MutableStateFlow<String?>(null)
    
    init {
        loadPluginInfo()
        observeDataPoints()
    }
    
    private fun loadPluginInfo() {
        val plugins = pluginManager.getAllActivePlugins()
        val pluginNames = plugins.associate { plugin -> 
            plugin.id to plugin.metadata.name 
        }
        val availablePlugins = plugins.map { plugin -> 
            plugin.id to plugin.metadata.name 
        }
        
        _uiState.update {
            it.copy(
                pluginNames = pluginNames,
                availablePlugins = availablePlugins
            )
        }
    }
    
    private fun observeDataPoints() {
        selectedPluginFilter
            .flatMapLatest { pluginId ->
                when (pluginId) {
                    null -> dataRepository.getLatestDataPoints(100)
                    else -> dataRepository.getPluginData(pluginId)
                }
            }
            .onEach { dataPoints ->
                _uiState.update {
                    it.copy(
                        dataPoints = dataPoints,
                        isLoading = false
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    fun filterByPlugin(pluginId: String?) {
        selectedPluginFilter.value = pluginId
        _uiState.update { it.copy(selectedPluginFilter = pluginId) }
    }
    
    fun refreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            // Trigger refresh by updating the filter
            val currentFilter = selectedPluginFilter.value
            selectedPluginFilter.value = currentFilter
        }
    }
}

data class DataUiState(
    val dataPoints: List<DataPoint> = emptyList(),
    val pluginNames: Map<String, String> = emptyMap(),
    val availablePlugins: List<Pair<String, String>> = emptyList(),
    val selectedPluginFilter: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
