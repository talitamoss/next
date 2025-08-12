package com.domain.app.ui.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.export.ExportManager
import com.domain.app.core.export.ExportResult
import com.domain.app.core.plugin.ExportFormat
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
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
    
    private val selectedPluginFilter = MutableStateFlow<String?>(null)
    private val refreshTrigger = MutableStateFlow(0)
    private val searchQuery = MutableStateFlow("")
    
    init {
        loadPluginInfo()
        observeDataPoints()
        loadWeeklyDataPoints()
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
            searchQuery,
            refreshTrigger
        ) { filter, query, _ -> 
            Pair(filter, query)
        }
            .flatMapLatest { (pluginId, query) ->
                when {
                    query.isNotEmpty() -> dataRepository.searchDataPoints(query)
                    pluginId != null -> dataRepository.getPluginData(pluginId)
                    else -> dataRepository.getLatestDataPoints(100)
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
    
    private fun loadWeeklyDataPoints() {
        viewModelScope.launch {
            val weekStart = Instant.now().minus(7, ChronoUnit.DAYS)
            dataRepository.getRecentData(24 * 7)
                .map { dataPoints ->
                    dataPoints.filter { it.timestamp.isAfter(weekStart) }
                }
                .collect { weeklyData ->
                    _uiState.update {
                        it.copy(weeklyDataPoints = weeklyData)
                    }
                }
        }
    }
    
    fun filterByPlugin(pluginId: String?) {
        selectedPluginFilter.value = pluginId
        _uiState.update { it.copy(selectedPluginFilter = pluginId) }
    }
    
    fun searchDataPoints(query: String) {
        searchQuery.value = query
        _uiState.update { it.copy(searchQuery = query) }
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
                dataRepository.deleteDataPointsByIds(ids)
                
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
        _uiState.update { state ->
            state.copy(
                selectedDataPoints = emptySet(),
                isInSelectionMode = false
            )
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
    
    fun deleteSelectedDataPoints() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedDataPoints.toList()
            if (ids.isNotEmpty()) {
                deleteMultipleDataPoints(ids)
            }
        }
    }
    
    fun updateFilters(filterState: FilterState) {
        _uiState.update {
            it.copy(
                selectedPluginFilter = filterState.selectedPlugin?.id,
                searchQuery = filterState.searchQuery
            )
        }
        
        // Apply the filters
        filterState.selectedPlugin?.let { 
            filterByPlugin(it.id) 
        } ?: filterByPlugin(null)
        
        if (filterState.searchQuery.isNotEmpty()) {
            searchDataPoints(filterState.searchQuery)
        } else {
            searchDataPoints("")
        }
    }
    
    fun exportData(format: ExportFormat) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = "Preparing export...") }
            
            try {
                val result = when (format) {
                    ExportFormat.CSV -> exportManager.exportAllDataToCsv(context)
                    ExportFormat.JSON -> ExportResult.Error("JSON export not yet implemented")
                    ExportFormat.XML -> ExportResult.Error("XML export not yet implemented")
                    ExportFormat.CUSTOM -> ExportResult.Error("Custom export not available")
                }
                
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
