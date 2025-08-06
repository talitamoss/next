// app/src/main/java/com/domain/app/ui/data/DataViewModel.kt
package com.domain.app.ui.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.Plugin
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
    private val refreshTrigger = MutableStateFlow(0)
    
    init {
        loadPluginInfo()
        observeDataPoints()
    }
    
    private fun loadPluginInfo() {
        val plugins = pluginManager.getAllActivePlugins()
        
        _uiState.update {
            it.copy(
                plugins = plugins,
                pluginNames = plugins.associate { plugin -> 
                    plugin.id to plugin.metadata.name 
                },
                pluginSummaries = plugins.map { plugin -> 
                    plugin.id to plugin.metadata.name 
                }
            )
        }
    }
    
    private fun observeDataPoints() {
        combine(
            selectedPluginFilter,
            refreshTrigger
        ) { filter, _ -> filter }
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
                        isLoading = false,
                        error = null
                    )
                }
            }
            .catch { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load data: ${exception.message}"
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
            _uiState.update { it.copy(isLoading = true, error = null) }
            // Trigger refresh by incrementing the trigger
            refreshTrigger.value++
        }
    }
    
    fun deleteDataPoint(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            
            try {
                dataRepository.deleteDataPoint(id)
                
                // Remove from UI immediately for better UX
                _uiState.update { state ->
                    state.copy(
                        dataPoints = state.dataPoints.filter { it.id != id },
                        isDeleting = false,
                        message = "Data point deleted successfully"
                    )
                }
                
                // Trigger background refresh
                refreshTrigger.value++
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isDeleting = false,
                        error = "Failed to delete: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun deleteMultipleDataPoints(ids: List<String>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            
            try {
                dataRepository.deleteDataPoints(ids)
                
                // Remove from UI immediately
                _uiState.update { state ->
                    state.copy(
                        dataPoints = state.dataPoints.filter { it.id !in ids },
                        isDeleting = false,
                        selectedDataPoints = emptySet(),
                        isInSelectionMode = false,
                        message = "${ids.size} data points deleted"
                    )
                }
                
                // Trigger background refresh
                refreshTrigger.value++
                
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isDeleting = false,
                        error = "Failed to delete: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun toggleDataPointSelection(id: String) {
        _uiState.update { state ->
            val newSelection = if (state.selectedDataPoints.contains(id)) {
                state.selectedDataPoints - id
            } else {
                state.selectedDataPoints + id
            }
            
            state.copy(
                selectedDataPoints = newSelection,
                isInSelectionMode = newSelection.isNotEmpty()
            )
        }
    }
    
    fun selectAllDataPoints() {
        _uiState.update { state ->
            state.copy(
                selectedDataPoints = state.dataPoints.map { it.id }.toSet(),
                isInSelectionMode = true
            )
        }
    }
    
    fun clearSelection() {
        _uiState.update { 
            it.copy(
                selectedDataPoints = emptySet(),
                isInSelectionMode = false
            )
        }
    }
    
    fun searchDataPoints(query: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(searchQuery = query, isLoading = true) }
            
            if (query.isBlank()) {
                refreshData()
                return@launch
            }
            
            dataRepository.searchDataPoints(query)
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Search failed: ${exception.message}"
                        )
                    }
                }
                .collect { results ->
                    _uiState.update {
                        it.copy(
                            dataPoints = results,
                            isLoading = false
                        )
                    }
                }
        }
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for the Data screen
 */
data class DataUiState(
    // Data
    val dataPoints: List<DataPoint> = emptyList(),
    val plugins: List<Plugin> = emptyList(),
    val pluginNames: Map<String, String> = emptyMap(),
    val pluginSummaries: List<Pair<String, String>> = emptyList(), // For dropdowns
    
    // Filters and search
    val selectedPluginFilter: String? = null,
    val searchQuery: String = "",
    
    // Selection mode
    val isInSelectionMode: Boolean = false,
    val selectedDataPoints: Set<String> = emptySet(),
    
    // Loading states
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    
    // Messages
    val error: String? = null,
    val message: String? = null
)
