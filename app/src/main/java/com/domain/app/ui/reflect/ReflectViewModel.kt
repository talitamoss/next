package com.domain.app.ui.reflect

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.core.plugin.PluginManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.min

// Data Models
data class ReflectUiState(
    val currentYearMonth: YearMonth = YearMonth.now(),
    val currentMonthYear: String = "",
    val selectedDate: LocalDate? = LocalDate.now(),
    val dayActivityMap: Map<LocalDate, DayActivity> = emptyMap(),
    val selectedDayData: DayData = DayData(),
    val currentStreak: Int = 0,
    val monthlyTotal: Int = 0,
    val mostActiveDay: String = "None",
    val isLoading: Boolean = false,
    val error: String? = null
)

data class DayActivity(
    val date: LocalDate,
    val entryCount: Int,
    val intensity: ActivityIntensity
)

enum class ActivityIntensity {
    LOW,    // 1-2 entries
    MEDIUM, // 3-5 entries
    HIGH    // 6+ entries
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
    
    init {
        loadCurrentMonth()
        selectDate(LocalDate.now())
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
    
    private fun loadCurrentMonth() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val yearMonth = _uiState.value.currentYearMonth
                val startOfMonth = yearMonth.atDay(1).atStartOfDay()
                val endOfMonth = yearMonth.atEndOfMonth().atTime(23, 59, 59)
                
                // Get all data points for the month
                dataRepository.getDataInRange(
                    startTime = startOfMonth.toInstant(ZoneOffset.UTC),
                    endTime = endOfMonth.toInstant(ZoneOffset.UTC)
                ).first().let { dataPoints ->
                    
                    // Group by date and create activity map
                    val activityMap = createActivityMap(dataPoints)
                    
                    // Calculate stats
                    val streak = calculateStreak(dataPoints)
                    val monthTotal = dataPoints.size
                    val mostActive = calculateMostActiveDay(activityMap)
                    
                    _uiState.update {
                        it.copy(
                            currentMonthYear = yearMonth.format(monthFormatter),
                            dayActivityMap = activityMap,
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
    
    private fun createActivityMap(dataPoints: List<DataPoint>): Map<LocalDate, DayActivity> {
        return dataPoints
            .groupBy { dp ->
                dp.timestamp.atZone(ZoneId.systemDefault()).toLocalDate()
            }
            .mapValues { (date, points) ->
                val count = points.size
                DayActivity(
                    date = date,
                    entryCount = count,
                    intensity = when {
                        count >= 6 -> ActivityIntensity.HIGH
                        count >= 3 -> ActivityIntensity.MEDIUM
                        else -> ActivityIntensity.LOW
                    }
                )
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
        
        // Group by day of week and sum entry counts
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
        // Format the main value from the data point
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
                // Try to find the first meaningful value
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

// Add this extension function to DataRepository if the getDataInRange method doesn't exist:
// fun getDataInRange(startTime: Instant, endTime: Instant): Flow<List<DataPoint>> {
//     return dataPointDao.getDataInRange(startTime, endTime)
//         .map { entities -> entities.map { entityToDataPoint(it) } }
// }
