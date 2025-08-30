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

// Data Models
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
    val displayValue: String,
    val note: String? = null,
    val time: String,
    val timestamp: Instant
)

@HiltViewModel
class ReflectViewModel @Inject constructor(
    private val dataRepository: DataRepository,
    private val pluginManager: PluginManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ReflectUiState())
    val uiState: StateFlow<ReflectUiState> = _uiState.asStateFlow()
    
    private val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    
    private var allMonthDataPoints: List<DataPoint> = emptyList()
    
    init {
        loadAvailablePlugins()
        loadCurrentMonth()
    }
    
    private fun loadAvailablePlugins() {
        viewModelScope.launch {
            val plugins = pluginManager.getAllActivePlugins()
            _uiState.update { it.copy(availablePlugins = plugins) }
        }
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
        _uiState.update {
            it.copy(
                currentYearMonth = yearMonth,
                currentMonthYear = yearMonth.format(monthFormatter)
            )
        }
        loadCurrentMonth()
    }
    
    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        loadDayDetails(date)
    }
    
    fun setPluginFilter(plugin: Plugin?) {
        _uiState.update { it.copy(selectedFilterPlugin = plugin) }
        applyFilter()
    }
    
    private fun applyFilter() {
        val selectedPlugin = _uiState.value.selectedFilterPlugin
        val allActivityMap = _uiState.value.dayActivityMap
        
        val filteredMap = if (selectedPlugin == null) {
            // Show all data
            allActivityMap
        } else {
            // Filter by selected plugin
            allActivityMap.mapNotNull { (date, activity) ->
                val pluginCount = activity.pluginCounts[selectedPlugin.id] ?: 0
                if (pluginCount > 0) {
                    date to activity.copy(
                        entryCount = pluginCount,
                        intensity = getIntensityForCount(pluginCount)
                    )
                } else {
                    null
                }
            }.toMap()
        }
        
        // Recalculate stats for filtered data
        val filteredDataPoints = if (selectedPlugin == null) {
            allMonthDataPoints
        } else {
            allMonthDataPoints.filter { it.pluginId == selectedPlugin.id }
        }
        
        val monthTotal = filteredDataPoints.size
        val mostActive = calculateMostActiveDay(filteredMap)
        val streak = calculateStreak(filteredDataPoints)
        
        _uiState.update {
            it.copy(
                filteredDayActivityMap = filteredMap,
                monthlyTotal = monthTotal,
                mostActiveDay = mostActive,
                currentStreak = streak
            )
        }
    }
    
    private fun loadCurrentMonth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val yearMonth = _uiState.value.currentYearMonth
                val startOfMonth = yearMonth.atDay(1).atStartOfDay()
                val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)
                
                dataRepository.getDataInRange(
                    startTime = startOfMonth.toInstant(ZoneOffset.UTC),
                    endTime = endOfMonth.toInstant(ZoneOffset.UTC)
                ).first().let { dataPoints ->
                    
                    allMonthDataPoints = dataPoints
                    
                    // Create activity map with plugin counts
                    val activityMap = createActivityMapWithPluginCounts(dataPoints)
                    
                    // Calculate initial stats (unfiltered)
                    val streak = calculateStreak(dataPoints)
                    val monthTotal = dataPoints.size
                    val mostActive = calculateMostActiveDay(activityMap)
                    
                    _uiState.update {
                        it.copy(
                            currentMonthYear = yearMonth.format(monthFormatter),
                            dayActivityMap = activityMap,
                            filteredDayActivityMap = activityMap,
                            currentStreak = streak,
                            monthlyTotal = monthTotal,
                            mostActiveDay = mostActive,
                            isLoading = false
                        )
                    }
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
    }
    
    private fun loadDayDetails(date: LocalDate) {
        viewModelScope.launch {
            try {
                val startOfDay = date.atStartOfDay()
                val endOfDay = date.atTime(23, 59, 59)
                
                dataRepository.getDataInRange(
                    startTime = startOfDay.toInstant(ZoneOffset.UTC),
                    endTime = endOfDay.toInstant(ZoneOffset.UTC)
                ).first().let { dataPoints ->
                    
                    val plugins = pluginManager.getAllActivePlugins()
                    val pluginMap = plugins.associateBy { it.id }
                    
                    val entries = dataPoints.map { dp ->
                        DataEntry(
                            id = dp.id,
                            pluginId = dp.pluginId,
                            pluginName = pluginMap[dp.pluginId]?.metadata?.name ?: dp.pluginId,
                            displayValue = formatDataPointValue(dp),
                            note = dp.metadata?.get("note"),
                            time = dp.timestamp.atZone(ZoneId.systemDefault())
                                .toLocalTime()
                                .format(timeFormatter),
                            timestamp = dp.timestamp
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
                }?.let { "${it.value}" } ?: "Recorded"
            }
        }
    }
    
    private fun formatDuration(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
            else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
