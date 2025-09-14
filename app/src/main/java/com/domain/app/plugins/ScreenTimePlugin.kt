package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.time.LocalDateTime
import kotlin.math.roundToInt

/**
 * Screen Time Tracking Plugin
 * Tracks device usage (handheld/laptop/large screen) with duration and feeling
 * Hours stored as Float (0.5h = 30min valid), Feeling as Integer (0-10 scale)
 */
class ScreenTimePlugin : Plugin {
    override val id = "screen_time"
    
    override val metadata = PluginMetadata(
        name = "Screen Time",
        description = "Track your screen time across different devices",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.LIFESTYLE,
        tags = listOf("screen", "digital", "device", "technology", "wellness"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.CAROUSEL,  // Primary type for multi-input
        supportsMultiStage = false,
        relatedPlugins = listOf("mood", "productivity", "sleep"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "screen time", "phone time", "computer time",
            "used phone", "on laptop", "watching TV",
            "scrolling", "browsing", "gaming"
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
        privacyPolicy = "Screen time data is stored locally and never shared without your permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your screen time",
        PluginCapability.READ_OWN_DATA to "View your screen time history",
        PluginCapability.LOCAL_STORAGE to "Save your screen time data on your device",
        PluginCapability.EXPORT_DATA to "Export your screen time data for personal use"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "screen_time_entry",
        title = "Log Screen Time",
        inputType = InputType.CAROUSEL,  // Primary type, but uses inputs list
        inputs = listOf(
            // Input 1: Device type carousel selector
            QuickAddInput(
                id = "device",
                label = "Device",
                type = InputType.CAROUSEL,
                defaultValue = "handheld",
                options = listOf(
                    QuickOption(label = "Hand-held", value = "handheld"),
                    QuickOption(label = "Laptop", value = "laptop"),
                    QuickOption(label = "Large Screen", value = "large_screen")
                ),
                required = true
            ),
            // Input 2: Hours horizontal slider (KEEP AS FLOAT - 0.5h is valid)
            QuickAddInput(
                id = "hours",
                label = "Hours",
                type = InputType.HORIZONTAL_SLIDER,
                required = true,
                min = 0.5f,
                max = 12f,
                defaultValue = 2f,
                unit = "hours"
                // showValue controlled by QuickAddDialog implementation
            ),
            // Input 3: Feeling about screen time (INTEGER SCALE 0-10)
            QuickAddInput(
                id = "feeling",
                label = "How do we feel about the screen time today?",
                type = InputType.HORIZONTAL_SLIDER,
                required = false,  // Optional - defaults to neutral if not set
                min = 0f,
                max = 10f,  // 0-10 scale for easier understanding
                defaultValue = 5f,  // Middle of scale
                unit = "",  // No unit for feeling scale
                // Using bottomLabel for left side (low values) and topLabel for right side (high values)
                // This makes semantic sense: bottom = low, top = high
                bottomLabel = "nah",    // Left side of horizontal slider (low values)
                topLabel = "yeah"       // Right side of horizontal slider (high values)
            )
        ),
        showValue = false,  // Hide numeric value on feeling slider
        primaryColor = "#6B46C1",    // Purple for digital wellness
        secondaryColor = "#9F7AEA"   // Lighter purple
    )
    
    override suspend fun collectData(): DataPoint? {
        // Manual entry only
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Extract the composite input data
        val device = data["device"] as? String ?: return null
        val hours = (data["hours"] as? Number)?.toFloat() ?: return null  // Keep as Float
        
        // Convert feeling to integer
        val rawFeeling = (data["feeling"] as? Number)?.toFloat() ?: 5f  // Default to neutral
        val feeling = rawFeeling.roundToInt()  // Round to nearest integer
        
        // Validate the entry
        val validationResult = validateDataPoint(mapOf(
            "device" to device,
            "hours" to hours,
            "feeling" to feeling
        ))
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        // Create feeling description for better data interpretation (0-10 scale)
        val feelingDescription = when (feeling) {
            in 0..1 -> "very_negative"
            in 2..3 -> "negative"
            in 4..6 -> "neutral"
            in 7..8 -> "positive"
            else -> "very_positive"
        }
        
        return DataPoint(
            id = "screen_time_${System.currentTimeMillis()}",
            pluginId = id,
            timestamp = Instant.now(),
            type = "screen_time_session",
            value = mapOf(
                "device" to device,
                "hours" to hours,  // Float value preserved
                "feeling" to feeling,  // Integer value
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
        val feeling = when (val f = data["feeling"]) {
            is Number -> f.toInt()
            else -> null
        }
        
        return when {
            device == null -> ValidationResult.Error("Device type is required")
            device !in listOf("handheld", "laptop", "large_screen") -> 
                ValidationResult.Error("Invalid device type")
            hours == null -> ValidationResult.Error("Hours is required")
            hours < 0 || hours > 24 -> ValidationResult.Error("Hours must be between 0 and 24")
            feeling != null && (feeling < 0 || feeling > 10) -> 
                ValidationResult.Error("Feeling must be between 0 and 10")
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
        
        // Format hours with decimal if needed
        val hours = when (val h = dataPoint.value["hours"]) {
            is Number -> {
                val floatValue = h.toFloat()
                if (floatValue % 1 == 0f) {
                    floatValue.toInt().toString()
                } else {
                    String.format("%.1f", floatValue)
                }
            }
            else -> ""
        }
        
        // Format feeling as integer
        val feeling = when (val f = dataPoint.value["feeling"]) {
            is Number -> f.toInt().toString()
            else -> ""
        }
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Device" to device,
            "Hours" to hours,
            "Feeling Score" to feeling,
            "Feeling Category" to (dataPoint.value["feeling_category"]?.toString() ?: ""),
            "Day of Week" to (dataPoint.metadata?.get("dayOfWeek") ?: "")
        )
    }
    
    /**
     * Custom formatter to ensure proper display of hours (float) and feeling (integer)
     */
    override fun getDataFormatter(): PluginDataFormatter = ScreenTimeDataFormatter()
    
    /**
     * Inner class for custom screen time data formatting
     */
    private inner class ScreenTimeDataFormatter : PluginDataFormatter {
        
        override fun formatSummary(dataPoint: DataPoint): String {
            val device = when (dataPoint.value["device"]) {
                "handheld" -> "📱"
                "laptop" -> "💻"
                "large_screen" -> "📺"
                else -> "📱"
            }
            
            // Format hours with appropriate precision
            val hours = when (val h = dataPoint.value["hours"]) {
                is Number -> {
                    val floatValue = h.toFloat()
                    if (floatValue % 1 == 0f) {
                        "${floatValue.toInt()}h"
                    } else {
                        String.format("%.1fh", floatValue)
                    }
                }
                else -> "0h"
            }
            
            // Format feeling as integer
            val feeling = when (val f = dataPoint.value["feeling"]) {
                is Number -> {
                    val intValue = f.toInt()
                    when {
                        intValue <= 3 -> "😟"
                        intValue <= 6 -> "😐"
                        else -> "😊"
                    }
                }
                else -> ""
            }
            
            return "$device $hours $feeling"
        }
        
        override fun formatDetails(dataPoint: DataPoint): List<DataField> {
            val fields = mutableListOf<DataField>()
            
            // Device
            val device = when (dataPoint.value["device"]) {
                "handheld" -> "Hand-held Device"
                "laptop" -> "Laptop"
                "large_screen" -> "Large Screen (TV/Monitor)"
                else -> dataPoint.value["device"]?.toString() ?: "Unknown"
            }
            fields.add(
                DataField(
                    label = "Device",
                    value = device
                )
            )
            
            // Hours - show with appropriate precision
            val hours = when (val h = dataPoint.value["hours"]) {
                is Number -> {
                    val floatValue = h.toFloat()
                    val formatted = if (floatValue % 1 == 0f) {
                        "${floatValue.toInt()} hours"
                    } else {
                        String.format("%.1f hours", floatValue)
                    }
                    formatted
                }
                else -> "0 hours"
            }
            fields.add(
                DataField(
                    label = "Duration",
                    value = hours,
                    isImportant = true
                )
            )
            
            // Feeling - formatted as integer
            val feeling = when (val f = dataPoint.value["feeling"]) {
                is Number -> f.toInt()
                else -> 5
            }
            val feelingText = when (feeling) {
                in 0..1 -> "Very Negative"
                in 2..3 -> "Negative"
                in 4..6 -> "Neutral"
                in 7..8 -> "Positive"
                else -> "Very Positive"
            }
            fields.add(
                DataField(
                    label = "Feeling",
                    value = "$feeling/10 ($feelingText)",
                    isImportant = true
                )
            )
            
            // Day of week
            dataPoint.metadata?.get("dayOfWeek")?.let { day ->
                fields.add(
                    DataField(
                        label = "Day",
                        value = day.toString()
                    )
                )
            }
            
            // Time
            val timeParts = dataPoint.timestamp.toString().split("T")
            if (timeParts.size > 1) {
                val timeSubParts = timeParts[1].split(".")
                if (timeSubParts.isNotEmpty() && timeSubParts[0].length >= 5) {
                    fields.add(
                        DataField(
                            label = "Time",
                            value = timeSubParts[0].substring(0, 5)  // HH:mm
                        )
                    )
                }
            }
            
            return fields
        }
        
        override fun getHiddenFields(): List<String> = listOf(
            "metadata", 
            "timestamp", 
            "source", 
            "version", 
            "inputType",
            "feeling_category"  // Hide since we show it with the feeling value
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
            (it.value["feeling"] as? Number)?.toInt() 
        }.average().roundToInt()
        
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
        val averageFeeling: Int = 5,  // Default to middle of 0-10 scale
        val deviceBreakdown: Map<Any?, Double> = emptyMap(),
        val sessionCount: Int = 0
    )
}
