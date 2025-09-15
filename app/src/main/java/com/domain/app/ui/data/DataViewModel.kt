// app/src/main/java/com/domain/app/ui/data/DataViewModel.kt
package com.domain.app.ui.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.export.ExportManager
import com.domain.app.core.export.ExportResult
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.ExportFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DataViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val pluginManager: PluginManager,
    private val exportManager: ExportManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DataUiState())
    val uiState: StateFlow<DataUiState> = _uiState.asStateFlow()
    
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    
    init {
        loadData()
        loadPlugins()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            // Load all data points
            dataRepository.getAllDataPoints().collect { dataPoints ->
                _uiState.update { state ->
                    state.copy(
                        dataPoints = dataPoints,
                        weeklyDataPoints = dataPoints.filter { dataPoint ->
                            val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
                            dataPoint.timestamp.toEpochMilli() >= weekAgo
                        }
                    )
                }
            }
        }
    }
    
    private fun loadPlugins() {
        viewModelScope.launch {
            val plugins = pluginManager.getAllActivePlugins()
            val pluginNames = plugins.associate { it.id to it.metadata.name }
            
            // Calculate plugin summaries
            val summaries = plugins.map { plugin ->
                val count = _uiState.value.dataPoints.count { it.pluginId == plugin.id }
                plugin.id to "$count entries"
            }
            
            _uiState.update { state ->
                state.copy(
                    plugins = plugins,
                    pluginNames = pluginNames,
                    pluginSummaries = summaries
                )
            }
        }
    }
    
    fun filterByPlugin(pluginId: String?) {
        _uiState.update { it.copy(selectedPluginFilter = pluginId) }
        _filterState.update { it.copy(selectedPlugin = _uiState.value.plugins.find { p -> p.id == pluginId }) }
    }
    
    fun searchDataPoints(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _filterState.update { it.copy(searchQuery = query) }
    }
    
    fun deleteDataPoint(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                dataRepository.deleteDataPoint(id)
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        message = "Data point deleted"
                    )
                }
                loadData() // Reload data after deletion
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
    
    fun enterSelectionMode() {
        _uiState.update { it.copy(isInSelectionMode = true) }
    }
    
    fun exitSelectionMode() {
        _uiState.update {
            it.copy(
                isInSelectionMode = false,
                selectedDataPoints = emptySet()
            )
        }
    }
    
    fun toggleDataPointSelection(id: String) {
        _uiState.update { state ->
            val newSelection = if (id in state.selectedDataPoints) {
                state.selectedDataPoints - id
            } else {
                state.selectedDataPoints + id
            }
            state.copy(selectedDataPoints = newSelection)
        }
    }
    
    fun deleteSelectedDataPoints() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                _uiState.value.selectedDataPoints.forEach { id ->
                    dataRepository.deleteDataPoint(id)
                }
                _uiState.update {
                    it.copy(
                        isDeleting = false,
                        isInSelectionMode = false,
                        selectedDataPoints = emptySet(),
                        message = "${it.selectedDataPoints.size} data points deleted"
                    )
                }
                loadData() // Reload data after deletion
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
    
    fun updateFilters(filterState: FilterState) {
        _filterState.value = filterState
        
        // Apply plugin filter
        filterState.selectedPlugin?.let { plugin ->
            filterByPlugin(plugin.id)
        } ?: filterByPlugin(null)
        
        // Apply search query
        if (filterState.searchQuery.isNotEmpty()) {
            searchDataPoints(filterState.searchQuery)
        } else {
            searchDataPoints("")
        }
    }
    
    /**
     * Export data with enhanced options
     * ENHANCED: Now accepts ExportOptions instead of just format
     */
    fun exportData(options: ExportOptions) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "Preparing export...") }
            
            try {
                val (startDate, endDate) = options.getDateRange()
                
                val result = exportManager.exportFilteredData(
                    context = context,
                    format = options.format,
                    pluginIds = if (options.selectedPlugins.isEmpty()) null else options.selectedPlugins,
                    startDate = startDate,
                    endDate = endDate,
                    encrypt = options.encrypt
                )
                
                when (result) {
                    is ExportResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                message = "Export successful: ${result.fileName}"
                            )
                        }
                    }
                    is ExportResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Export failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Export data with format only (backward compatibility)
     * KEPT: For backward compatibility with existing code
     */
    fun exportData(format: ExportFormat) {
        val options = ExportOptions(
            format = format,
            timeFrame = TimeFrame.ALL,
            selectedPlugins = _uiState.value.plugins.map { it.id }.toSet(),
            encrypt = false
        )
        exportData(options)
    }
    
    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class DataUiState(
    val dataPoints: List<DataPoint> = emptyList(),
    val weeklyDataPoints: List<DataPoint> = emptyList(),
    val plugins: List<Plugin> = emptyList(),
    val pluginNames: Map<String, String> = emptyMap(),
    val pluginSummaries: List<Pair<String, String>> = emptyList(),
    val selectedPluginFilter: String? = null,
    val selectedDataPoints: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isDeleting: Boolean = false,
    val isInSelectionMode: Boolean = false,
    val error: String? = null,
    val message: String? = null
)

data class FilterState(
    val selectedPlugin: Plugin? = null,
    val dateRange: Pair<Long, Long>? = null,
    val searchQuery: String = ""
)
