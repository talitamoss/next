package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Work tracking plugin
 * Uses range slider to select work session time range (start to end time)
 * Supports multiple work sessions per day for accurate time tracking
 */
class WorkPlugin : Plugin {
    override val id = "work"
    
    override val metadata = PluginMetadata(
        name = "Work",
        description = "Track your work sessions and productivity",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.PRODUCTIVITY,
        tags = listOf("work", "productivity", "time", "tracking", "business", "focus"),
        dataPattern = DataPattern.OCCURRENCE,  // Each work session is a discrete occurrence
        inputType = InputType.TIME_RANGE,  // Uses RangeSlider for time selection
        supportsMultiStage = false,
        relatedPlugins = listOf("energy", "mood", "focus"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "work", "worked", "working", "job", "office",
            "shift", "hours", "clocked in", "logged time",
            "productive", "focus time", "deep work"
        ),
        contextualTriggers = listOf(
            ContextTrigger.TIME_OF_DAY,
            ContextTrigger.PATTERN_BASED,
            ContextTrigger.AFTER_EVENT
        )
    )
    
    override val securityManifest = PluginSecurityManifest(
        requestedCapabilities = setOf(
            PluginCapability.COLLECT_DATA,
            PluginCapability.READ_OWN_DATA,
            PluginCapability.LOCAL_STORAGE,
            PluginCapability.EXPORT_DATA
        ),
        dataSensitivity = DataSensitivity.NORMAL,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Work tracking data is stored locally and never shared without your permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your work sessions",
        PluginCapability.READ_OWN_DATA to "View your work history and patterns",
        PluginCapability.LOCAL_STORAGE to "Save your work data on your device",
        PluginCapability.EXPORT_DATA to "Export your work logs for timesheets or analysis"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "work",  // Using "work" as the key for consistency
        title = "Log Work Session",
        inputType = InputType.TIME_RANGE,
        min = 0f,        // 0:00 (midnight)
        max = 24f,       // 24 hours to handle 
        step = 0.25f,    // 15-minute increments for more precise tracking
        defaultValue = mapOf(
            "start_time" to 9f,   // 9 AM default start
            "end_time" to 17f     // 5 PM default end
        ),
        unit = null,
        showValue = true,
        primaryColor = "#1976D2",    // Professional blue
        secondaryColor = "#BBDEFB"   // Light blue for track
    )
    
    override suspend fun collectData(): DataPoint? {
        // Automatic collection not implemented for work tracking
        // Could potentially integrate with calendar or time tracking apps in future
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Handle both naming conventions for flexibility
        val startTime = (data["start_time"] as? Number)?.toFloat() 
            ?: (data["startTime"] as? Number)?.toFloat()
            ?: (data["bedtime"] as? Number)?.toFloat()  // Also accept sleep plugin keys
            ?: return null
            
        val endTime = (data["end_time"] as? Number)?.toFloat()
            ?: (data["endTime"] as? Number)?.toFloat()
            ?: (data["waketime"] as? Number)?.toFloat()  // Also accept sleep plugin keys
            ?: return null
        
        // Calculate duration accounting for crossing midnight
        val duration = if (endTime > startTime) {
            endTime - startTime
        } else {
            (24 - startTime) + endTime
        }
        
        // Get current time info for metadata
        val now = LocalDateTime.now()
        val dayOfWeek = now.dayOfWeek.toString()
        
        // Determine session type based on duration and time
        val sessionType = when {
            duration >= 8 -> "full_day"
            duration >= 4 -> "half_day"
            duration >= 2 -> "focused_session"
            else -> "quick_task"
        }
        
        return DataPoint(
            id = generateDataPointId(),
            pluginId = id,
            timestamp = Instant.now(),
            type = "work_session",
            value = mapOf<String, Any>(
                "start_time" to startTime,
                "end_time" to endTime,
                "duration" to duration,
                "session_type" to sessionType,
                "overtime" to (duration > 8),  // Flag for overtime work
                "productivity_score" to calculateProductivityScore(duration, startTime)
            ),
            metadata = mapOf(
	    "startKey" to "bedtime",
	    "endKey" to "waketime",
	    "startLabel" to "Bedtime",
	    "endLabel" to "Wake",
	    "durationLabel" to "{duration} sleep",
	    "timeFormat" to "12h"
	    )
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val startTime = data["start_time"] as? Number
            ?: data["startTime"] as? Number
            ?: data["bedtime"] as? Number  // Also accept sleep plugin keys
        val endTime = data["end_time"] as? Number
            ?: data["endTime"] as? Number
            ?: data["waketime"] as? Number  // Also accept sleep plugin keys
        
        return when {
            startTime == null -> ValidationResult.Error("Start time is required")
            endTime == null -> ValidationResult.Error("End time is required")
            startTime.toFloat() < 0 || startTime.toFloat() > 48 -> 
                ValidationResult.Error("Start time must be between 0 and 48")
            endTime.toFloat() < 0 || endTime.toFloat() > 48 -> 
                ValidationResult.Error("End time must be between 0 and 48")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders(): List<String> {
        return listOf(
            "Date",
            "Start Time",
            "End Time", 
            "Duration (hours)",
            "Session Type",
            "Overtime",
            "Productivity Score",
            "Day of Week"
        )
    }
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val value = dataPoint.value as? Map<String, Any> ?: return emptyMap()
        val metadata = dataPoint.metadata as? Map<String, Any> ?: emptyMap()
        
        // Extract values with proper type handling
        val startTime = value["start_time"] as? Float ?: 0f
        val endTime = value["end_time"] as? Float ?: 0f
        val duration = value["duration"] as? Float ?: 0f
        val sessionType = value["session_type"] as? String ?: ""
        val overtime = value["overtime"] as? Boolean ?: false
        val productivityScore = value["productivity_score"] as? Int ?: 0
        val dayOfWeek = metadata["dayOfWeek"] as? String ?: ""
        val formattedStart = metadata["formatted_start"] as? String ?: formatTime(startTime)
        val formattedEnd = metadata["formatted_end"] as? String ?: formatTime(endTime)
        
        return mapOf(
            "Date" to dataPoint.timestamp.toString().split("T")[0],
            "Start Time" to formattedStart,
            "End Time" to formattedEnd,
            "Duration (hours)" to duration.toString(),
            "Session Type" to sessionType,
            "Overtime" to overtime.toString(),
            "Productivity Score" to productivityScore.toString(),
            "Day of Week" to dayOfWeek
        )
    }
    
    override suspend fun cleanup() {
        // No special cleanup needed for Work plugin
    }
    
    // Helper methods
    private fun generateDataPointId(): String {
        return "work_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }
    
    private fun calculateProductivityScore(duration: Float, startTime: Float): Int {
        // Simple productivity score based on duration and time of day
        // Morning work (6-12) gets bonus, very long sessions get penalty
        val timeBonus = when (startTime.toInt()) {
            in 6..12 -> 10  // Morning bonus
            in 13..17 -> 5  // Afternoon standard
            else -> 0        // Evening/night no bonus
        }
        
        val durationScore = when {
            duration in 2f..4f -> 30  // Focused session
            duration in 4f..6f -> 25  // Good session
            duration in 6f..8f -> 20  // Full day
            duration > 10f -> 5       // Too long, fatigue likely
            else -> 10                // Short task
        }
        
        return (timeBonus + durationScore).coerceIn(0, 100)
    }
    
    private fun formatTime(time: Float): String {
        val hours = time.toInt()
        val minutes = ((time - hours) * 60).toInt()
        return "%02d:%02d".format(hours % 24, minutes)
    }
    
    private fun formatDuration(duration: Float): String {
        val hours = duration.toInt()
        val minutes = ((duration - hours) * 60).toInt()
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}
