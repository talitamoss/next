// app/src/main/java/com/domain/app/plugins/SleepPlugin.kt
package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult

/**
 * Sleep tracking plugin using RangeSlider for bedtime and wake time selection
 */
class SleepPlugin : Plugin {
    override val id = "sleep"
    
    override val metadata = PluginMetadata(
        name = "Sleep",
        description = "Track your sleep patterns",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("sleep", "rest", "health", "recovery"),
        dataPattern = DataPattern.DURATION,
        inputType = InputType.TIME_RANGE,  // New input type for time ranges
        supportsMultiStage = false,
        relatedPlugins = listOf("mood", "exercise", "energy"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "sleep", "slept", "went to bed", "woke up",
            "bedtime", "wake time", "rest", "nap"
        ),
        contextualTriggers = listOf(
            ContextTrigger.TIME_OF_DAY,
            ContextTrigger.PATTERN_BASED
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
        privacyPolicy = "Sleep data is stored locally and encrypted. We never share this data without your explicit consent.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your sleep times",
        PluginCapability.READ_OWN_DATA to "View your sleep history and patterns",
        PluginCapability.LOCAL_STORAGE to "Save your sleep data on your device",
        PluginCapability.EXPORT_DATA to "Export your sleep data for analysis"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Track Sleep",
        inputType = InputType.TIME_RANGE,
        // Default to 11 PM - 7 AM (8 hours)
        defaultValue = mapOf(
            "bedtime" to 23.0f,  // 11 PM in 24-hour format
            "waketime" to 7.0f   // 7 AM
        ),
        min = 0f,   // Midnight
        max = 24f,  // Full 24-hour range
        // Note: The dialog will use RangeSlider with special time handling
        // The component will display:
        // - Total sleep duration prominently
        // - Bedtime and wake time
        // - Visual range on 12PM-12PM timeline
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val bedtimeHours = (data["bedtime"] as? Number)?.toFloat() ?: return null
        val waketimeHours = (data["waketime"] as? Number)?.toFloat() ?: return null
        
        val validationResult = validateDataPoint(data)
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        // Calculate duration
        var duration = waketimeHours - bedtimeHours
        if (duration < 0) duration += 24  // Handle overnight sleep
        
        // Format times for storage
        val bedtimeFormatted = formatHoursToTime(bedtimeHours)
        val waketimeFormatted = formatHoursToTime(waketimeHours)
        
        return DataPoint(
            pluginId = id,
            type = "sleep_session",
            value = mapOf(
                "bedtime" to bedtimeFormatted,
                "waketime" to waketimeFormatted,
                "duration_hours" to duration,
                "duration_formatted" to formatDuration(duration)
            ),
            metadata = mapOf(
                "quick_add" to "true"
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val bedtime = (data["bedtime"] as? Number)?.toFloat()
        val waketime = (data["waketime"] as? Number)?.toFloat()
        
        return when {
            bedtime == null || waketime == null -> 
                ValidationResult.Error("Both bedtime and wake time are required")
            bedtime !in 0f..24f || waketime !in 0f..24f -> 
                ValidationResult.Error("Times must be in 24-hour format")
            else -> {
                // Calculate duration to ensure minimum sleep
                var duration = waketime - bedtime
                if (duration < 0) duration += 24
                if (duration < 0.5f) {
                    ValidationResult.Error("Sleep duration must be at least 30 minutes")
                } else {
                    ValidationResult.Success
                }
            }
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Bedtime", "WakeTime", "Duration", "DurationHours"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        
        return mapOf(
            "Date" to date,
            "Bedtime" to (dataPoint.value["bedtime"]?.toString() ?: ""),
            "WakeTime" to (dataPoint.value["waketime"]?.toString() ?: ""),
            "Duration" to (dataPoint.value["duration_formatted"]?.toString() ?: ""),
            "DurationHours" to (dataPoint.value["duration_hours"]?.toString() ?: "")
        )
    }
    
    /**
     * Format hours (24-hour float) to time string
     */
    private fun formatHoursToTime(hours: Float): String {
        val h = hours.toInt()
        val m = ((hours - h) * 60).toInt()
        val period = if (h >= 12) "PM" else "AM"
        val displayHour = when {
            h == 0 -> 12
            h > 12 -> h - 12
            else -> h
        }
        return String.format("%d:%02d %s", displayHour, m, period)
    }
    
    /**
     * Format duration in hours to readable string
     */
    private fun formatDuration(hours: Float): String {
        val h = hours.toInt()
        val m = ((hours - h) * 60).toInt()
        return "${h}h ${m}m"
    }
}
