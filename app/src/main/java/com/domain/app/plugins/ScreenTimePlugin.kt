package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Screen Time tracking plugin
 * Tracks hours spent on different device types with a feelings reflection slider
 * No judgment, just awareness and self-reflection
 */
class ScreenTimePlugin : Plugin {
    override val id = "screen_time"
    
    // Simple device categories
    private val deviceTypes = listOf(
        "Hand-held",      // Phones, tablets, etc.
        "Laptop",         // Laptops, computers
        "Large Screen"    // TVs, monitors, projectors
    )
    
    override val metadata = PluginMetadata(
        name = "Screen Time",
        description = "Track device usage and how you feel about it",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.LIFESTYLE,
        tags = listOf("screen", "device", "digital", "wellness", "reflection"),
        dataPattern = DataPattern.COMPOSITE,  // Multiple values: device, hours, feeling
        inputType = InputType.CHOICE,  // Start with device selection
        supportsMultiStage = true,  // Multi-stage input
        relatedPlugins = listOf("mood", "sleep", "focus"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "screen time", "phone time", "device", "screen",
            "watched", "scrolling", "computer time", "tv time"
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
        privacyPolicy = "Screen time and feeling data is stored locally. " +
                       "We only track hours and your feelings, no specific content or apps.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your screen time and feelings",
        PluginCapability.READ_OWN_DATA to "View your screen time patterns",
        PluginCapability.LOCAL_STORAGE to "Save your data on your device",
        PluginCapability.EXPORT_DATA to "Export your screen time data"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "screen_time",
        title = "Log Screen Time",
        inputType = InputType.CHOICE,
        // Multi-stage configuration
        stages = listOf(
            // Stage 1: Device selection (tiles)
            QuickAddStage(
                id = "device",
                title = "Which device?",
                inputType = InputType.CHOICE,
                options = listOf(
                    QuickOption(
                        label = "Hand-held",
                        value = "handheld",
                        icon = "📱"
                    ),
                    QuickOption(
                        label = "Laptop",
                        value = "laptop",
                        icon = "💻"
                    ),
                    QuickOption(
                        label = "Large Screen",
                        value = "large_screen",
                        icon = "🖥️"
                    )
                )
            ),
            // Stage 2: Hours spent
            QuickAddStage(
                id = "hours",
                title = "How many hours?",
                inputType = InputType.HORIZONTAL_SLIDER,
                min = 0.5f,
                max = 12f,
                step = 0.5f,
                defaultValue = 2f,
                unit = "hours",
                showValue = true
            ),
            // Stage 3: Feeling about the screen time
            QuickAddStage(
                id = "feeling",
                title = "How do we feel about the screen time today?",
                inputType = InputType.HORIZONTAL_SLIDER,
                min = 0f,
                max = 100f,
                step = 1f,
                defaultValue = 50f,
                showValue = false,  // Hide the numeric value
                leftLabel = "😔",   // Not great about it
                rightLabel = "😊",  // Good about it
                centerLabel = "😐"  // Neutral (optional)
            )
        ),
        primaryColor = "#6B46C1",    // Purple for digital wellness
        secondaryColor = "#9F7AEA"   // Lighter purple
    )
    
    override suspend fun collectData(): DataPoint? {
        // Manual entry only
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Extract the multi-stage input data
        val device = data["device"] as? String ?: return null
        val hours = (data["hours"] as? Number)?.toFloat() ?: return null
        val feeling = (data["feeling"] as? Number)?.toFloat() ?: 50f  // Default to neutral
        
        // Validate the entry
        val validationResult = validateDataPoint(data)
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        // Create feeling description for better data interpretation
        val feelingDescription = when {
            feeling < 20 -> "very_negative"
            feeling < 40 -> "negative"
            feeling < 60 -> "neutral"
            feeling < 80 -> "positive"
            else -> "very_positive"
        }
        
        return DataPoint(
            id = "screen_time_${System.currentTimeMillis()}",
            pluginId = id,
            timestamp = Instant.now(),
            type = "screen_time_session",
            value = mapOf(
                "device" to device,
                "hours" to hours,
                "feeling" to feeling,
                "feeling_category" to feelingDescription
            ),
            metadata = mapOf(
                "version" to metadata.version,
                "inputType" to "manual",
                "dayOfWeek" to LocalDateTime.now().dayOfWeek.toString()
            )
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val device = data["device"] as? String
        val hours = (data["hours"] as? Number)?.toFloat()
        val feeling = (data["feeling"] as? Number)?.toFloat()
        
        return when {
            device == null -> ValidationResult.Error("Device type is required")
            device !in listOf("handheld", "laptop", "large_screen") -> 
                ValidationResult.Error("Invalid device type")
            hours == null -> ValidationResult.Error("Hours is required")
            hours < 0 || hours > 24 -> ValidationResult.Error("Hours must be between 0 and 24")
            feeling != null && (feeling < 0 || feeling > 100) -> 
                ValidationResult.Error("Feeling must be between 0 and 100")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders(): List<String> {
        return listOf(
            "Date",
            "Time", 
            "Device",
            "Hours",
            "Feeling Score",
            "Feeling Category",
            "Day of Week"
        )
    }
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        
        val device = when (dataPoint.value["device"]) {
            "handheld" -> "Hand-held"
            "laptop" -> "Laptop"
            "large_screen" -> "Large Screen"
            else -> dataPoint.value["device"]?.toString() ?: ""
        }
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Device" to device,
            "Hours" to (dataPoint.value["hours"]?.toString() ?: ""),
            "Feeling Score" to (dataPoint.value["feeling"]?.toString() ?: ""),
            "Feeling Category" to (dataPoint.value["feeling_category"]?.toString() ?: ""),
            "Day of Week" to (dataPoint.metadata["dayOfWeek"]?.toString() ?: "")
        )
    }
    
    /**
     * Get aggregated stats for dashboard display
     */
    fun getScreenTimeStats(dataPoints: List<DataPoint>): ScreenTimeStats {
        if (dataPoints.isEmpty()) {
            return ScreenTimeStats()
        }
        
        val totalHours = dataPoints.sumOf { 
            (it.value["hours"] as? Number)?.toDouble() ?: 0.0 
        }
        
        val averageFeeling = dataPoints.mapNotNull { 
            (it.value["feeling"] as? Number)?.toFloat() 
        }.average().toFloat()
        
        val deviceBreakdown = dataPoints
            .groupBy { it.value["device"] }
            .mapValues { (_, points) ->
                points.sumOf { (it.value["hours"] as? Number)?.toDouble() ?: 0.0 }
            }
        
        return ScreenTimeStats(
            totalHours = totalHours,
            averageFeeling = averageFeeling,
            deviceBreakdown = deviceBreakdown,
            sessionCount = dataPoints.size
        )
    }
    
    data class ScreenTimeStats(
        val totalHours: Double = 0.0,
        val averageFeeling: Float = 50f,
        val deviceBreakdown: Map<Any?, Double> = emptyMap(),
        val sessionCount: Int = 0
    )
}

/**
 * Extension for QuickAddStage to support feeling slider
 */
data class QuickAddStage(
    val id: String,
    val title: String,
    val inputType: InputType,
    val options: List<QuickOption>? = null,
    val min: Float? = null,
    val max: Float? = null,
    val step: Float? = null,
    val defaultValue: Any? = null,
    val unit: String? = null,
    val showValue: Boolean = true,
    val leftLabel: String? = null,    // For slider endpoints
    val rightLabel: String? = null,   // For slider endpoints
    val centerLabel: String? = null,  // Optional center label
    val allowCustom: Boolean = false
)
