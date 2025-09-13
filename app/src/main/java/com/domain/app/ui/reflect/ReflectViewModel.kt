// app/src/main/java/com/domain/app/ui/reflect/ReflectViewModel.kt
package com.domain.app.ui.reflect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.PluginRegistry
import com.domain.app.core.plugin.DataField
import com.domain.app.core.plugin.DefaultDataFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

// ============== DATA MODELS ==============

enum class TimeFrame {
    DAY, WEEK, MONTH
}

data class ReflectUiState(
    // Calendar state
    val currentYearMonth: YearMonth = YearMonth.now(),
    val currentMonthYear: String = "",
    val selectedDate: LocalDate? = null,
    
    // Activity data
    val dayActivityMap: Map<LocalDate, DayActivity> = emptyMap(),
    val filteredDayActivityMap: Map<LocalDate, DayActivity> = emptyMap(),
    
    // Plugin filtering
    val availablePlugins: List<Plugin> = emptyList(),
    val selectedPluginIds: Set<String> = emptySet(),
    val showAllPlugins: Boolean = true,
    
    // Time frame data
    val selectedTimeFrame: TimeFrame = TimeFrame.DAY,
    val selectedDayData: DayData = DayData(),
    val weekData: WeekData? = null,
    val monthData: MonthData? = null,
    
    // Statistics
    val currentStreak: Int = 0,
    val monthlyTotal: Int = 0,
    val mostActiveDay: String = "None",
    
    // UI state
    val expandedEntryIds: Set<String> = emptySet(),
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
    NONE, LOW, MEDIUM, HIGH, VERY_HIGH
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
    val displayValue: String,
    val rawValue: Map<String, Any?>,
    val formattedDetails: List<DataField>,
    val metadata: Map<String, Any?>?,
    val note: String?,
    val time: String,
    val timestamp: Instant,
    val source: String?
)

data class WeekData(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val dailyBreakdown: Map<LocalDate, DayStats>,
    val totalEntries: Int,
    val dailyAverage: Float,
    val pluginStats: Map<String, Map<String, String>>,
    val timePatterns: Map<String, Float>
)

data class DayStats(
    val date: LocalDate,
    val totalEntries: Int,
    val pluginCounts: Map<String, Int>
)

data class MonthData(
    val yearMonth: YearMonth,
    val weeklyBreakdown: Map<Int, WeekStats>,
    val totalEntries: Int,
    val dailyAverage: Float,
    val activeDays: Int,
    val pluginTotals: Map<String, Int>,
    val trends: Map<String, Float>,
    val bestDay: Pair<String, Int>?,
    val worstDay: Pair<String, Int>?
)

data class WeekStats(
    val weekNumber: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalEntries: Int,
    val trend: Float? = null
)

// ============== VIEW MODEL ==============

@HiltViewModel
class ReflectViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val pluginManager: PluginManager,
    private val pluginRegistry: PluginRegistry
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReflectUiState())
    val uiState: StateFlow<ReflectUiState> = _uiState.asStateFlow()
    
    // Formatters
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val defaultFormatter = DefaultDataFormatter()
    
    init {
        loadInitialData()
        observeDataChanges()
    }
    
    // ============== INITIALIZATION ==============
    
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
                val filteredPoints = filterDataPointsBySelectedPlugins(dataPoints)
                val monthData = filterDataPointsForMonth(filteredPoints, currentMonth)
                updateActivityMap(monthData)
                updateStats(monthData)
                refreshCurrentView()
            }
            .launchIn(viewModelScope)
    }
    

    private fun loadAvailablePlugins() {
        viewModelScope.launch {
            try {
                val allPlugins = pluginManager.getAllActivePlugins()
                _uiState.update { currentState ->
                    currentState.copy(availablePlugins = allPlugins)
                }
                
            } catch (e: Exception) {
                // Handle any errors gracefully
                _uiState.update { 
                    it.copy(
                        availablePlugins = emptyList(),
                        error = "Failed to load plugins: ${e.message}"
                    )
                }
            }
        }
    }    
    // ============== PLUGIN FILTERING ==============
    
    fun togglePluginFilter(pluginId: String) {
        _uiState.update { state ->
            val currentPlugins = state.availablePlugins
            
            // If we're in "show all" mode and user deselects one plugin,
            // switch to selective mode with all BUT the deselected plugin
            if (state.showAllPlugins) {
                val allPluginIds = currentPlugins.map { it.id }.toSet()
                val newSelectedIds = allPluginIds - pluginId
                
                state.copy(
                    selectedPluginIds = newSelectedIds,
                    showAllPlugins = false
                )
            } else {
                // Normal toggle behavior when not in "show all" mode
                val newSelectedIds = if (pluginId in state.selectedPluginIds) {
                    state.selectedPluginIds - pluginId
                } else {
                    state.selectedPluginIds + pluginId
                }
                
                // If all plugins are now selected, switch to "show all" mode
                val allSelected = currentPlugins.isNotEmpty() && 
                                 newSelectedIds.size == currentPlugins.size
                
                state.copy(
                    selectedPluginIds = if (allSelected) emptySet() else newSelectedIds,
                    showAllPlugins = allSelected
                )
            }
        }
        updateFilteredData()
    }
    
    fun selectAllPlugins() {
        _uiState.update { it.copy(showAllPlugins = true, selectedPluginIds = emptySet()) }
        updateFilteredData()
    }
    
    fun clearAllPlugins() {
        _uiState.update { it.copy(showAllPlugins = false, selectedPluginIds = emptySet()) }
        updateFilteredData()
    }
    
    private fun updateFilteredData() {
        viewModelScope.launch {
            val state = _uiState.value
            
            // Update filtered activity map for calendar
            val filteredMap = if (state.showAllPlugins) {
                state.dayActivityMap
            } else if (state.selectedPluginIds.isNotEmpty()) {
                state.dayActivityMap.mapValues { (_, activity) ->
                    val filteredCounts = activity.pluginCounts.filterKeys { it in state.selectedPluginIds }
                    activity.copy(
                        entryCount = filteredCounts.values.sum(),
                        pluginCounts = filteredCounts,
                        intensity = calculateIntensity(filteredCounts.values.sum())
                    )
                }.filter { it.value.entryCount > 0 }
            } else {
                emptyMap()
            }
            
            _uiState.update { it.copy(filteredDayActivityMap = filteredMap) }
            refreshCurrentView()
        }
    }
    
    // ============== TIME FRAME SELECTION ==============
    
    fun selectTimeFrame(timeFrame: TimeFrame) {
        _uiState.update { it.copy(selectedTimeFrame = timeFrame) }
        refreshCurrentView()
    }
    
    private fun refreshCurrentView() {
        viewModelScope.launch {
            when (_uiState.value.selectedTimeFrame) {
                TimeFrame.DAY -> {
                    _uiState.value.selectedDate?.let { loadDayDetails(it) }
                }
                TimeFrame.WEEK -> {
                    loadWeekData(_uiState.value.selectedDate ?: LocalDate.now())
                }
                TimeFrame.MONTH -> {
                    loadMonthViewData(_uiState.value.currentYearMonth)
                }
            }
        }
    }
    
    // ============== CALENDAR NAVIGATION ==============
    
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
        _uiState.update {
            it.copy(
                currentYearMonth = yearMonth,
                currentMonthYear = yearMonth.format(monthYearFormatter)
            )
        }
    }
    
    fun selectDate(date: LocalDate) {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(selectedDate = date, expandedEntryIds = emptySet())
            }
            refreshCurrentView()
        }
    }
    
    // ============== DATA LOADING ==============
    
    private suspend fun loadMonthData(yearMonth: YearMonth) {
        try {
            _uiState.update { it.copy(isLoading = true) }
            
            val startOfMonth = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault()).toInstant()
            
            dataRepository.getDataInRange(startOfMonth, endOfMonth)
                .collect { dataPoints ->
                    val filteredPoints = filterDataPointsBySelectedPlugins(dataPoints)
                    updateActivityMap(filteredPoints)
                    updateStats(filteredPoints)
                    _uiState.update { it.copy(isLoading = false) }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isLoading = false, error = "Failed to load month data: ${e.message}")
            }
        }
    }
    
    private suspend fun loadDayDetails(date: LocalDate) {
        try {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = date.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            
            dataRepository.getDataInRange(startOfDay, endOfDay)
                .collect { dataPoints ->
                    val filteredPoints = filterDataPointsBySelectedPlugins(dataPoints)
                    val entries = createDataEntries(filteredPoints)
                    
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
    
    private suspend fun loadWeekData(referenceDate: LocalDate) {
        try {
            val weekFields = WeekFields.of(Locale.getDefault())
            val startOfWeek = referenceDate.with(weekFields.dayOfWeek(), 1)
            val endOfWeek = startOfWeek.plusDays(6)
            
            val startInstant = startOfWeek.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endInstant = endOfWeek.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant()
            
            dataRepository.getDataInRange(startInstant, endInstant)
                .collect { dataPoints ->
                    val filteredPoints = filterDataPointsBySelectedPlugins(dataPoints)
                    val weekData = createWeekData(startOfWeek, endOfWeek, filteredPoints)
                    _uiState.update { it.copy(weekData = weekData) }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = "Failed to load week data: ${e.message}")
            }
        }
    }
    
    private suspend fun loadMonthViewData(yearMonth: YearMonth) {
        try {
            val startOfMonth = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)
                .atZone(ZoneId.systemDefault()).toInstant()
            
            dataRepository.getDataInRange(startOfMonth, endOfMonth)
                .collect { dataPoints ->
                    val filteredPoints = filterDataPointsBySelectedPlugins(dataPoints)
                    val monthData = createMonthData(yearMonth, filteredPoints)
                    _uiState.update { it.copy(monthData = monthData) }
                }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(error = "Failed to load month data: ${e.message}")
            }
        }
    }
    
    // ============== DATA PROCESSING ==============
    
    private fun filterDataPointsBySelectedPlugins(dataPoints: List<DataPoint>): List<DataPoint> {
        val state = _uiState.value
        return if (state.showAllPlugins || state.selectedPluginIds.isEmpty()) {
            dataPoints
        } else {
            dataPoints.filter { it.pluginId in state.selectedPluginIds }
        }
    }
    
    private fun filterDataPointsForMonth(dataPoints: List<DataPoint>, yearMonth: YearMonth): List<DataPoint> {
        val startOfMonth = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)
            .atZone(ZoneId.systemDefault()).toInstant()
        
        return dataPoints.filter { dp ->
            dp.timestamp >= startOfMonth && dp.timestamp <= endOfMonth
        }
    }
    
    private fun createDataEntries(dataPoints: List<DataPoint>): List<DataEntry> {
        return dataPoints.map { dp ->
            val plugin = pluginManager.getPlugin(dp.pluginId)
            val formatter = plugin?.getDataFormatter() ?: defaultFormatter
            
            val displayValue = formatter.formatSummary(dp)
            val formattedDetails = formatter.formatDetails(dp)
            
            val note = dp.value["note"] as? String 
                ?: dp.value["notes"] as? String
                ?: dp.metadata?.get("note")
            
            val source = dp.value["source"] as? String
                ?: dp.metadata?.get("source")
                ?: dp.source
            
            DataEntry(
                id = dp.id,
                pluginId = dp.pluginId,
                pluginName = plugin?.metadata?.name ?: dp.pluginId,
                displayValue = displayValue,
                rawValue = dp.value,
                formattedDetails = formattedDetails,
                metadata = dp.metadata,
                note = note as? String,
                time = dp.timestamp.atZone(ZoneId.systemDefault()).format(timeFormatter),
                timestamp = dp.timestamp,
                source = source
            )
        }
    }
    
    private fun createWeekData(startDate: LocalDate, endDate: LocalDate, dataPoints: List<DataPoint>): WeekData {
        val dailyBreakdown = (0..6).associate { dayOffset ->
            val date = startDate.plusDays(dayOffset.toLong())
            val dayPoints = dataPoints.filter { dp ->
                dp.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == date
            }
            date to DayStats(
                date = date,
                totalEntries = dayPoints.size,
                pluginCounts = dayPoints.groupBy { it.pluginId }.mapValues { it.value.size }
            )
        }
        
        return WeekData(
            startDate = startDate,
            endDate = endDate,
            dailyBreakdown = dailyBreakdown,
            totalEntries = dataPoints.size,
            dailyAverage = dataPoints.size / 7f,
            pluginStats = calculatePluginStats(dataPoints),
            timePatterns = calculateTimePatterns(dataPoints)
        )
    }
    
    private fun createMonthData(yearMonth: YearMonth, dataPoints: List<DataPoint>): MonthData {
        val weekFields = WeekFields.of(Locale.getDefault())
        val weeklyBreakdown = dataPoints
            .groupBy { dp ->
                dp.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                    .get(weekFields.weekOfMonth())
            }
            .mapValues { (weekNum, weekPoints) ->
                val dates = weekPoints.map {
                    it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
                }
                WeekStats(
                    weekNumber = weekNum,
                    startDate = dates.minOrNull() ?: yearMonth.atDay(1),
                    endDate = dates.maxOrNull() ?: yearMonth.atEndOfMonth(),
                    totalEntries = weekPoints.size
                )
            }
        
        val dayGroups = dataPoints.groupBy {
            it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
        }.mapValues { it.value.size }
        
        val bestDay = dayGroups.maxByOrNull { it.value }?.let {
            it.key.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()) to it.value
        }
        
        val worstDay = if (dayGroups.isNotEmpty()) {
            dayGroups.minByOrNull { it.value }?.let {
                it.key.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()) to it.value
            }
        } else null
        
        return MonthData(
            yearMonth = yearMonth,
            weeklyBreakdown = weeklyBreakdown,
            totalEntries = dataPoints.size,
            dailyAverage = if (yearMonth.lengthOfMonth() > 0) {
                dataPoints.size.toFloat() / yearMonth.lengthOfMonth()
            } else 0f,
            activeDays = dayGroups.keys.size,
            pluginTotals = dataPoints.groupBy { it.pluginId }.mapValues { it.value.size },
            trends = emptyMap(),
            bestDay = bestDay,
            worstDay = worstDay
        )
    }
    
    // ============== STATISTICS ==============
    
    private fun updateActivityMap(dataPoints: List<DataPoint>) {
        val activityMap = createActivityMapWithPluginCounts(dataPoints)
        
        _uiState.update { state ->
            val filteredMap = if (!state.showAllPlugins && state.selectedPluginIds.isNotEmpty()) {
                activityMap.mapValues { (_, activity) ->
                    val filteredCounts = activity.pluginCounts.filterKeys { it in state.selectedPluginIds }
                    activity.copy(
                        entryCount = filteredCounts.values.sum(),
                        pluginCounts = filteredCounts,
                        intensity = calculateIntensity(filteredCounts.values.sum())
                    )
                }.filter { it.value.entryCount > 0 }
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
        val mostActiveDay = calculateMostActiveDay(createActivityMapWithPluginCounts(dataPoints))
        
        _uiState.update {
            it.copy(
                currentStreak = streak,
                monthlyTotal = monthTotal,
                mostActiveDay = mostActiveDay
            )
        }
    }
    
    private fun createActivityMapWithPluginCounts(dataPoints: List<DataPoint>): Map<LocalDate, DayActivity> {
        return dataPoints
            .groupBy { dp ->
                dp.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .mapValues { (date, points) ->
                DayActivity(
                    date = date,
                    entryCount = points.size,
                    intensity = calculateIntensity(points.size),
                    pluginCounts = points.groupBy { it.pluginId }.mapValues { it.value.size }
                )
            }
    }
    
    private fun calculateIntensity(count: Int): ActivityIntensity {
        return when (count) {
            0 -> ActivityIntensity.NONE
            in 1..3 -> ActivityIntensity.LOW
            in 4..6 -> ActivityIntensity.MEDIUM
            in 7..10 -> ActivityIntensity.HIGH
            else -> ActivityIntensity.VERY_HIGH
        }
    }
    
    private fun calculateStreak(dataPoints: List<DataPoint>): Int {
        if (dataPoints.isEmpty()) return 0
        
        val dates = dataPoints
            .map { it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() }
            .distinct()
            .sorted()
        
        if (dates.isEmpty()) return 0
        
        var currentStreak = 1
        var maxStreak = 1
        
        for (i in 1 until dates.size) {
            if (dates[i].minusDays(1) == dates[i - 1]) {
                currentStreak++
                maxStreak = maxOf(maxStreak, currentStreak)
            } else {
                currentStreak = 1
            }
        }
        
        return maxStreak
    }
    
    private fun calculateMostActiveDay(activityMap: Map<LocalDate, DayActivity>): String {
        val mostActive = activityMap.maxByOrNull { it.value.entryCount }
        return mostActive?.key?.dayOfWeek?.toString()?.lowercase()?.capitalize() ?: "None"
    }
    
    private fun calculatePluginStats(dataPoints: List<DataPoint>): Map<String, Map<String, String>> {
        val stats = mutableMapOf<String, Map<String, String>>()
        
        dataPoints.groupBy { it.pluginId }.forEach { (pluginId, points) ->
            stats[pluginId] = mapOf(
                "Total" to points.size.toString(),
                "Daily Avg" to "%.1f".format(points.size.toFloat() / 7)
            )
        }
        
        return stats
    }
    
    private fun calculateTimePatterns(dataPoints: List<DataPoint>): Map<String, Float> {
        val timeSlots = mapOf(
            "Morning" to (6..11),
            "Afternoon" to (12..17),
            "Evening" to (18..23),
            "Night" to (0..5)
        )
        
        val slotCounts = mutableMapOf<String, Int>()
        
        dataPoints.forEach { dp ->
            val hour = dp.timestamp.atZone(ZoneId.systemDefault()).hour
            timeSlots.forEach { (slotName, range) ->
                if (hour in range) {
                    slotCounts[slotName] = (slotCounts[slotName] ?: 0) + 1
                }
            }
        }
        
        val total = slotCounts.values.sum().toFloat()
        return slotCounts.mapValues { (_, count) ->
            if (total > 0) (count / total) * 100 else 0f
        }
    }
    
    // ============== UI ACTIONS ==============
    
    fun toggleEntryExpanded(entryId: String) {
        _uiState.update { state ->
            val newExpandedIds = if (entryId in state.expandedEntryIds) {
                state.expandedEntryIds - entryId
            } else {
                state.expandedEntryIds + entryId
            }
            state.copy(expandedEntryIds = newExpandedIds)
        }
    }
    
    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            try {
                dataRepository.deleteDataPoint(entryId)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(error = "Failed to delete entry: ${e.message}")
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
