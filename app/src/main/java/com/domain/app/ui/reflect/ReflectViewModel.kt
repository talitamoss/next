// app/src/main/java/com/domain/app/ui/reflect/ReflectViewModel.kt
package com.domain.app.ui.reflect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// Enhanced Data Models with raw data support
data class ReflectUiState(
    val currentYearMonth: YearMonth = YearMonth.now(),
    val currentMonthYear: String = "",
    val selectedDate: LocalDate? = null,
    val dayActivityMap: Map<LocalDate, DayActivity> = emptyMap(),
    val filteredDayActivityMap: Map<LocalDate, DayActivity> = emptyMap(),
    val selectedDayData: DayData = DayData(),
    val currentStreak: Int = 0,
    val monthlyTotal: Int = 0,
    val mostActiveDay: String = "None",
    val availablePlugins: List<Plugin> = emptyList(),
    val selectedFilterPlugin: Plugin? = null,
    val expandedEntryIds: Set<String> = emptySet(),  // Track which cards are expanded
    val isLoading: Boolean = false,
    val error: String? = null
)

data class DayActivity(
    val date: LocalDate,
    val entryCount: Int,
    val intensity: ActivityIntensity,
    val pluginCounts: Map<String, Int> = emptyMap()
)

enum class ActivityIntensity {
    NONE,     // 0 entries
    LOW,      // 1-3 entries
    MEDIUM,   // 4-6 entries
    HIGH,     // 7-10 entries
    VERY_HIGH // 10+ entries
}

data class DayData(
    val date: LocalDate = LocalDate.now(),
    val entries: List<DataEntry> = emptyList(),
    val totalCount: Int = 0
)

data class DataEntry(
    val id: String,
    val pluginId: String,
    val pluginName: String,
    val displayValue: String,  // Formatted summary
    val rawValue: Map<String, Any?>,  // Raw data from DataPoint
    val formattedDetails: List<DataField>,  // NEW: Formatted details for display
    val metadata: Map<String, Any?>?,
    val note: String?,
    val time: String,
    val timestamp: Instant,
    val source: String?
)

@HiltViewModel
class ReflectViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val pluginManager: PluginManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReflectUiState())
    val uiState: StateFlow<ReflectUiState> = _uiState.asStateFlow()
    
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val defaultFormatter = DefaultDataFormatter()

    init {
        loadInitialData()
        observeDataChanges()
    }
    
    private fun loadInitialData() {
        viewModelScope.launch {
            val currentMonth = YearMonth.now()
            updateMonthDisplay(currentMonth)
            loadMonthData(currentMonth)
            loadAvailablePlugins()
        }
    }
    
    private fun observeDataChanges() {
        dataRepository.getAllDataPoints()
            .onEach { dataPoints ->
                val currentMonth = _uiState.value.currentYearMonth
                val monthData = filterDataPointsForMonth(dataPoints, currentMonth)
                updateActivityMap(monthData)
                updateStats(monthData)
            }
            .launchIn(viewModelScope)
    }
    
    fun previousMonth() {
        val newMonth = _uiState.value.currentYearMonth.minusMonths(1)
        updateMonth(newMonth)
    }
    
    fun nextMonth() {
        val newMonth = _uiState.value.currentYearMonth.plusMonths(1)
        updateMonth(newMonth)
    }
    
    private fun updateMonth(yearMonth: YearMonth) {
        viewModelScope.launch {
            updateMonthDisplay(yearMonth)
            loadMonthData(yearMonth)
            // Clear selected date and expanded cards when changing months
            _uiState.update { 
                it.copy(
                    selectedDate = null, 
                    selectedDayData = DayData(),
                    expandedEntryIds = emptySet()
                )
            }
        }
    }
    
    private fun updateMonthDisplay(yearMonth: YearMonth) {
        val display = yearMonth.format(monthYearFormatter)
        _uiState.update {
            it.copy(
                currentYearMonth = yearMonth,
                currentMonthYear = display
            )
        }
    }
    
    fun selectDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    selectedDate = date,
                    expandedEntryIds = emptySet()  // Reset expanded cards when selecting new date
                )
            }
            loadDayDetails(date)
        }
    }
    
    fun toggleEntryExpanded(entryId: String) {
        _uiState.update { state ->
            val expandedIds = if (state.expandedEntryIds.contains(entryId)) {
                state.expandedEntryIds - entryId
            } else {
                state.expandedEntryIds + entryId
            }
            state.copy(expandedEntryIds = expandedIds)
        }
    }
    
    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            try {
                dataRepository.deleteDataPoint(entryId)
                // Refresh the current day's data
                _uiState.value.selectedDate?.let { date ->
                    loadDayDetails(date)
                }
                // Remove from expanded if it was expanded
                _uiState.update { state ->
                    state.copy(expandedEntryIds = state.expandedEntryIds - entryId)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to delete entry: ${e.message}")
                }
            }
        }
    }
    
    fun setFilterPlugin(plugin: Plugin?) {
        _uiState.update { state ->
            val filteredMap = if (plugin != null) {
                state.dayActivityMap.filterValues { activity ->
                    activity.pluginCounts.containsKey(plugin.id)
                }
            } else {
                state.dayActivityMap
            }
            
            state.copy(
                selectedFilterPlugin = plugin,
                filteredDayActivityMap = filteredMap
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    private suspend fun loadAvailablePlugins() {
        // FIXED: Use getAllActivePlugins() instead of getEnabledPlugins()
        val plugins = pluginManager.getAllActivePlugins()
        _uiState.update { it.copy(availablePlugins = plugins) }
    }
    
    private suspend fun loadMonthData(yearMonth: YearMonth) {
        try {
            _uiState.update { it.copy(isLoading = true) }
            
            val startOfMonth = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault()).toInstant()
            
            // FIXED: Use getDataInRange() instead of getDataPointsForDateRange()
            dataRepository.getDataInRange(startOfMonth, endOfMonth)
                .collect { dataPoints: List<DataPoint> ->  // FIXED: Explicit type declaration
                    updateActivityMap(dataPoints)
                    updateStats(dataPoints)
                    _uiState.update { it.copy(isLoading = false) }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Failed to load month data: ${e.message}"
                )
            }
        }
    }
    
    private fun updateActivityMap(dataPoints: List<DataPoint>) {
        val activityMap = createActivityMapWithPluginCounts(dataPoints)
        
        _uiState.update { state ->
            val filteredMap = if (state.selectedFilterPlugin != null) {
                activityMap.filterValues { activity ->
                    activity.pluginCounts.containsKey(state.selectedFilterPlugin.id)
                }
            } else {
                activityMap
            }
            
            state.copy(
                dayActivityMap = activityMap,
                filteredDayActivityMap = filteredMap
            )
        }
    }
    
    private fun updateStats(dataPoints: List<DataPoint>) {
        val streak = calculateStreak(dataPoints)
        val monthTotal = dataPoints.size
        val mostActiveDay = calculateMostActiveDay(
            createActivityMapWithPluginCounts(dataPoints)
        )
        
        _uiState.update {
            it.copy(
                currentStreak = streak,
                monthlyTotal = monthTotal,
                mostActiveDay = mostActiveDay
            )
        }
    }
    
private suspend fun loadDayDetails(date: LocalDate) {
    try {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
        
        dataRepository.getDataInRange(startOfDay, endOfDay)
            .onSuccess { dataPoints ->
                val entries = dataPoints.map { dp ->
                    val plugin = pluginManager.getPlugin(dp.pluginId)
                    val formatter = plugin?.getDataFormatter() ?: defaultFormatter
                    
                    // Get formatted summary and details
                    val displayValue = formatter.formatSummary(dp)
                    val formattedDetails = formatter.formatDetails(dp)
                    
                    // Extract note if exists
                    val noteFromValue = dp.value["note"] as? String 
                        ?: dp.value["notes"] as? String
                    val noteFromMetadata = dp.metadata?.get("note")
                    val finalNote = noteFromValue ?: noteFromMetadata
                    
                    // Get source
                    val sourceFromValue = dp.value["source"] as? String
                    val sourceFromMetadata = dp.metadata?.get("source")
                    val finalSource = sourceFromValue ?: sourceFromMetadata ?: dp.source
                    
                    DataEntry(
                        id = dp.id,
                        pluginId = dp.pluginId,
                        pluginName = plugin?.metadata?.name ?: dp.pluginId,
                        displayValue = displayValue,  // Now using formatter
                        rawValue = dp.value,
                        formattedDetails = formattedDetails,  // NEW: Formatted details
                        metadata = dp.metadata?.mapValues { it.value as Any? },
                        note = finalNote,
                        time = dp.timestamp.atZone(ZoneId.systemDefault())
                            .toLocalTime()
                            .format(timeFormatter),
                        timestamp = dp.timestamp,
                        source = finalSource
                    )
                }.sortedByDescending { it.timestamp }
                
                _uiState.update {
                    it.copy(
                        selectedDayData = DayData(
                            date = date,
                            entries = entries,
                            totalCount = entries.size
                        )
                    )
                }
            }
    } catch (e: Exception) {
        _uiState.update {
            it.copy(error = "Failed to load day details: ${e.message}")
        }
    }
}    
    private fun filterDataPointsForMonth(
        dataPoints: List<DataPoint>, 
        yearMonth: YearMonth
    ): List<DataPoint> {
        val startOfMonth = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault()).toInstant()
        
        return dataPoints.filter { dp ->
            dp.timestamp in startOfMonth..endOfMonth
        }
    }
    
    private fun createActivityMapWithPluginCounts(dataPoints: List<DataPoint>): Map<LocalDate, DayActivity> {
        return dataPoints
            .groupBy { dp ->
                dp.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .mapValues { (date, points) ->
                val pluginCounts = points.groupBy { it.pluginId }
                    .mapValues { it.value.size }
                val totalCount = points.size
                
                DayActivity(
                    date = date,
                    entryCount = totalCount,
                    intensity = getIntensityForCount(totalCount),
                    pluginCounts = pluginCounts
                )
            }
    }
    
    private fun getIntensityForCount(count: Int): ActivityIntensity {
        return when {
            count == 0 -> ActivityIntensity.NONE
            count in 1..3 -> ActivityIntensity.LOW
            count in 4..6 -> ActivityIntensity.MEDIUM
            count in 7..10 -> ActivityIntensity.HIGH
            else -> ActivityIntensity.VERY_HIGH
        }
    }
    
    private fun calculateStreak(dataPoints: List<DataPoint>): Int {
        if (dataPoints.isEmpty()) return 0
        
        val dates = dataPoints
            .map { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sorted()
            .reversed()
        
        var streak = 0
        var currentDate = LocalDate.now()
        
        for (date in dates) {
            if (date == currentDate || date == currentDate.minusDays(1)) {
                streak++
                currentDate = date
            } else {
                break
            }
        }
        
        return streak
    }
    
    private fun calculateMostActiveDay(activityMap: Map<LocalDate, DayActivity>): String {
        if (activityMap.isEmpty()) return "None"
        
        val dayOfWeekCounts = activityMap.values
            .groupBy { it.date.dayOfWeek }
            .mapValues { (_, activities) ->
                activities.sumOf { it.entryCount }
            }
        
        val mostActiveDay = dayOfWeekCounts.maxByOrNull { it.value }?.key
        
        return mostActiveDay?.getDisplayName(
            java.time.format.TextStyle.FULL,
            java.util.Locale.getDefault()
        ) ?: "None"
    }
    
    private fun formatDataPointValue(dataPoint: DataPoint): String {
        val value = dataPoint.value
        
        return when {
            value.containsKey("amount") -> {
                val amount = value["amount"]
                val unit = value["unit"] ?: ""
                "$amount $unit".trim()
            }
            value.containsKey("value") -> {
                val mainValue = value["value"]
                val unit = value["unit"] ?: ""
                "$mainValue $unit".trim()
            }
            value.containsKey("duration_seconds") -> {
                val seconds = (value["duration_seconds"] as? Number)?.toLong() ?: 0
                formatDuration(seconds)
            }
            value.containsKey("hours") -> {
                val hours = value["hours"]
                "$hours hours"
            }
            value.containsKey("mood") -> {
                val mood = value["mood"]
                val score = value["score"]
                if (score != null) {
                    "$mood ($score/10)"
                } else {
                    mood.toString()
                }
            }
            value.containsKey("file_name") -> {
                val duration = value["duration_seconds"]
                if (duration != null) {
                    "Audio: ${formatDuration((duration as Number).toLong())}"
                } else {
                    "Audio recording"
                }
            }
            else -> {
                value.entries.firstOrNull { 
                    it.key !in listOf("note", "metadata", "timestamp", "source")
                }?.let { "${it.value}" } ?: "Data entry"
            }
        }
    }
    
    private fun formatDuration(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
            minutes > 0 -> String.format("%d:%02d", minutes, secs)
            else -> String.format("%ds", secs)
        }
    }
}
