package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant

/**
 * Sleep tracking plugin
 * Uses range slider to select sleep time range (bedtime to wake time)
 */
class SleepPlugin : Plugin {
    override val id = "sleep"
    
    override val metadata = PluginMetadata(
        name = "Sleep",
        description = "Track your sleep duration and quality",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("sleep", "rest", "health", "daily", "recovery"),
        dataPattern = DataPattern.OCCURRENCE,  // Fixed: OCCURRENCE exists
        inputType = InputType.TIME_RANGE,  // Uses RangeSlider for time selection
        supportsMultiStage = false,
        relatedPlugins = listOf("mood", "energy"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "sleep", "slept", "rest", "nap", "dozed",
            "went to bed", "woke up", "sleeping"
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
        privacyPolicy = "Sleep data is stored locally and never shared without your permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your sleep patterns",
        PluginCapability.READ_OWN_DATA to "View your sleep history",
        PluginCapability.LOCAL_STORAGE to "Save your sleep data on your device",
        PluginCapability.EXPORT_DATA to "Export your sleep data for personal use"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "sleep",
        title = "Log Sleep",
        inputType = InputType.TIME_RANGE,
        min = 12f,        // 0:00 (midnight)
        max = 36f,       // 48 hours to handle crossing midnight
        step = 1f,       // 1-hour increments for simplicity
        defaultValue = mapOf(
            "bedtime" to 23f,   // 11 PM default
            "waketime" to 7f    // 7 AM default
        ),
        unit = null,
        showValue = true,
        primaryColor = "#1E3A8A",    // Deep blue for sleep
        secondaryColor = "#60A5FA"   // Light blue for wake
    )
    
    override suspend fun collectData(): DataPoint? {
        // Automatic collection not implemented for sleep
        // Would require integration with device sensors or sleep tracking apps
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val bedtime = (data["bedtime"] as? Number)?.toFloat() ?: return null
        val waketime = (data["waketime"] as? Number)?.toFloat() ?: return null
        
        // Calculate duration accounting for crossing midnight
        val duration = if (waketime > bedtime) {
            waketime - bedtime
        } else {
            (24 - bedtime) + waketime
        }
        
        return DataPoint(
            id = generateDataPointId(),
            pluginId = id,
            timestamp = Instant.now(),
            type = "sleep_session",  // Added required type parameter
            value = mapOf(
                "bedtime" to bedtime,
                "waketime" to waketime,
                "duration" to duration,
                "quality" to calculateSleepQuality(duration)
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
        val bedtime = data["bedtime"] as? Number
        val waketime = data["waketime"] as? Number
        
        if (bedtime == null || waketime == null) {
            return ValidationResult.error(
                "Both bedtime and wake time are required"
            )
        }
        
        val bedtimeFloat = bedtime.toFloat()
        val waketimeFloat = waketime.toFloat()
        
        if (bedtimeFloat !in 0f..24f || waketimeFloat !in 0f..24f) {
            return ValidationResult.error(
                "Times must be between 0:00 and 24:00"
            )
        }
        
        // Calculate duration
        val duration = if (waketimeFloat > bedtimeFloat) {
            waketimeFloat - bedtimeFloat
        } else {
            (24 - bedtimeFloat) + waketimeFloat
        }
        
        // Warn if sleep duration seems unusual
        return when {
            duration < 3 -> ValidationResult.warning(
                "Very short sleep duration (less than 3 hours)"
            )
            duration > 12 -> ValidationResult.warning(
                "Very long sleep duration (more than 12 hours)"
            )
            else -> ValidationResult.success()
        }
    }
    
    override fun exportHeaders(): List<String> {
        return listOf("timestamp", "bedtime", "wake_time", "duration_hours", "quality_score")
    }
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val value = dataPoint.value
        val bedtime = (value["bedtime"] as? Number)?.toFloat() ?: 0f
        val waketime = (value["waketime"] as? Number)?.toFloat() ?: 0f
        val duration = (value["duration"] as? Number)?.toFloat() ?: 0f
        val quality = (value["quality"] as? Number)?.toInt() ?: 0
        
        return mapOf(
            "timestamp" to dataPoint.timestamp.toString(),
            "bedtime" to formatTime(bedtime),
            "wake_time" to formatTime(waketime),
            "duration_hours" to String.format("%.1f", duration),
            "quality_score" to quality.toString()
        )
    }
    
    // Helper functions
    
    private fun generateDataPointId(): String {
        return "sleep_${System.currentTimeMillis()}"
    }
    
    private fun calculateSleepQuality(duration: Float): Int {
        // Simple quality calculation based on duration
        return when {
            duration < 4 -> 20
            duration < 6 -> 40
            duration in 7f..9f -> 100
            duration > 10 -> 60
            else -> 70
        }
    }
    
    private fun formatTime(hour: Float): String {
        val h = hour.toInt()
        val m = ((hour - h) * 60).toInt()
        return String.format("%02d:%02d", h, m)
    }
    override fun getDataFormatter(): PluginDataFormatter {
    return SleepDataFormatter()
   }
}

private inner class SleepDataFormatter : PluginDataFormatter {
    
    override fun formatSummary(dataPoint: DataPoint): String {
        val hours = (dataPoint.value["hours"] as? Number)?.toFloat() ?: 0f
        val quality = dataPoint.value["quality"]?.toString() ?: ""
        
        val hoursText = when {
            hours < 1 -> "${(hours * 60).toInt()} minutes"
            hours == hours.toInt().toFloat() -> "${hours.toInt()} hours"
            else -> String.format("%.1f hours", hours)
        }
        
        val qualityText = when(quality) {
            "deep" -> "Deep Sleep"
            "good" -> "Good Sleep"
            "light" -> "Light Sleep"
            "poor" -> "Poor Sleep"
            else -> quality
        }
        
        return if (qualityText.isNotEmpty()) {
            "$hoursText - $qualityText"
        } else {
            hoursText
        }
    }
    
    override fun formatDetails(dataPoint: DataPoint): List<DataField> {
        val fields = mutableListOf<DataField>()
        
        // Sleep Duration - Important field
        dataPoint.value["hours"]?.let { hours ->
            val hoursFloat = (hours as? Number)?.toFloat() ?: 0f
            val hoursInt = hoursFloat.toInt()
            val minutes = ((hoursFloat - hoursInt) * 60).toInt()
            
            val durationText = when {
                hoursInt == 0 -> "$minutes minutes"
                minutes == 0 -> "$hoursInt hours"
                else -> "$hoursInt hours $minutes minutes"
            }
            
            fields.add(DataField(
                label = "Sleep Duration",
                value = durationText + getSleepDurationQuality(hoursFloat),
                isImportant = true
            ))
        }
        
        // Bedtime
        dataPoint.value["bedtime"]?.let { bedtime ->
            val hour = (bedtime as? Number)?.toFloat() ?: 0f
            fields.add(DataField(
                label = "Bedtime",
                value = formatTimeFromHour(hour)
            ))
        }
        
        // Wake time
        dataPoint.value["waketime"]?.let { waketime ->
            val hour = (waketime as? Number)?.toFloat() ?: 0f
            fields.add(DataField(
                label = "Wake Time",
                value = formatTimeFromHour(hour)
            ))
        }
        
        // Sleep Quality
        dataPoint.value["quality"]?.let { quality ->
            fields.add(DataField(
                label = "Sleep Quality",
                value = when(quality.toString()) {
                    "deep" -> "Deep Sleep"
                    "good" -> "Good Sleep"
                    "light" -> "Light Sleep"
                    "poor" -> "Poor Sleep"
                    else -> quality.toString()
                }
            ))
        }
        
        // Dreams
        dataPoint.value["dreams"]?.let { dreams ->
            if (dreams is Boolean && dreams) {
                fields.add(DataField(
                    label = "Dreams",
                    value = "Yes"
                ))
            }
        }
        
        // Notes
        dataPoint.value["notes"]?.let { notes ->
            if (notes.toString().isNotBlank()) {
                fields.add(DataField(
                    label = "Notes",
                    value = notes.toString(),
                    isLongText = true
                ))
            }
        }
        
        return fields
    }
    
    private fun formatTimeFromHour(hour: Float): String {
        val hourInt = hour.toInt()
        val minutes = ((hour - hourInt) * 60).toInt()
        
        val displayHour = when {
            hourInt == 0 -> 12
            hourInt > 12 -> hourInt - 12
            else -> hourInt
        }
        
        val period = if (hourInt < 12) "AM" else "PM"
        
        return if (minutes == 0) {
            "$displayHour:00 $period"
        } else {
            String.format("%d:%02d %s", displayHour, minutes, period)
        }
    }
    
    private fun getSleepDurationQuality(hours: Float): String {
        return when {
            hours < 4 -> " (Very Short)"
            hours < 6 -> " (Short)"
            hours <= 9 -> " (Normal)"
            hours <= 10 -> " (Long)"
            else -> " (Very Long)"
        }
    }
}
