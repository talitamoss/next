package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Movement tracking plugin
 * Tracks physical movement and activities with duration (integer minutes) and intensity (float 0-1)
 */
class MovementPlugin : Plugin {
    
    override val id = "movement"
    
    override val metadata = PluginMetadata(
        name = "Movement",
        version = "1.0.0",
        author = "System",
        description = "Track physical movement and activities",
        category = PluginCategory.HEALTH,
        tags = listOf("movement", "exercise", "activity", "fitness"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.CAROUSEL,  // Primary input type
        supportsMultiStage = false,
        relatedPlugins = emptyList(),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "movement", "exercise", "workout", "activity",
            "run", "walk", "dance", "stretch", "pilates"
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
        privacyPolicy = "Movement data is stored locally and never shared without your permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    // Movement types for the carousel
    val movementTypes = listOf(
        "Stretch",
        "Run", 
        "Work Out",
        "Dance",
        "Ride",
        "Surf",
        "Walk",
        "Sex",
        "Pilates",
        "Other"
    )
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your movement activities",
        PluginCapability.READ_OWN_DATA to "View your movement history",
        PluginCapability.LOCAL_STORAGE to "Save your movement data on your device",
        PluginCapability.EXPORT_DATA to "Export your movement data for personal use"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    // Using composite inputs (handled by config.inputs != null check in QuickAddDialog)
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "movement_entry",
        title = "Log Movement", 
        inputType = InputType.CAROUSEL,  // Primary type, but composite handled by inputs field
        inputs = listOf(
            // Movement type carousel
            QuickAddInput(
                id = "type",
                label = "Type of movement",
                type = InputType.CAROUSEL,
                defaultValue = "Walk",
                options = movementTypes.map { type ->
                    QuickOption(label = type, value = type)
                }
            ),
            // Duration horizontal slider (0 to 120 minutes = 2 hours) - INTEGER
            QuickAddInput(
                id = "duration",
                label = "Duration",
                type = InputType.HORIZONTAL_SLIDER,
                defaultValue = 30f,
                min = 0f,
                max = 120f,
                unit = "min"
            ),
            // Intensity horizontal slider - FLOAT (0.5 is valid for medium intensity)
            QuickAddInput(
                id = "intensity",
                label = "Intensity",
                type = InputType.HORIZONTAL_SLIDER,
                defaultValue = 0.5f,
                min = 0f,
                max = 1f,
                topLabel = "Extreme",
                bottomLabel = "Light"
            )
        )
    )
    
    override suspend fun collectData(): DataPoint? {
        // Automatic collection not implemented
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val movementType = data["type"] as? String ?: "Other"
        
        // Duration as integer minutes
        val durationMinutes = (data["duration"] as? Number)?.toInt() ?: 30
        
        // FIX: Store intensity as Float value (0-1 scale), not as String
        val intensity = (data["intensity"] as? Number)?.toFloat() ?: 0.5f
        
        val notes = data["notes"] as? String
        
        // Convert intensity to descriptive label for additional context
        val intensityLabel = getIntensityLabel(intensity)
        
        return DataPoint(
            id = generateDataPointId(),
            pluginId = id,
            timestamp = Instant.now(),
            type = "movement_entry",
            value = mapOf(
                "type" to movementType,
                "duration_minutes" to durationMinutes,  // Integer
                "intensity" to intensity,  // Float value (0-1)
                "intensity_label" to intensityLabel,  // Descriptive text
                "notes" to (notes ?: "")
            ),
            metadata = mapOf(
                "version" to metadata.version,
                "inputType" to "manual",
                "has_notes" to (notes != null).toString()
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val duration = (data["duration"] as? Number)?.toInt()
        val type = data["type"] as? String
        val intensity = (data["intensity"] as? Number)?.toFloat()
        
        return when {
            type.isNullOrBlank() -> ValidationResult.Error("Movement type is required")
            duration == null -> ValidationResult.Error("Duration is required")
            duration < 0 -> ValidationResult.Error("Duration cannot be negative")
            duration > 720 -> ValidationResult.Warning("That's over 12 hours! Are you sure?")
            intensity != null && (intensity < 0f || intensity > 1f) -> 
                ValidationResult.Error("Intensity must be between 0 and 1")
            else -> ValidationResult.Success
        }
    }
    
    private fun generateDataPointId(): String {
        return "${id}_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    private fun getIntensityLabel(intensity: Float): String {
        return when {
            intensity < 0.2f -> "Very Light"
            intensity < 0.4f -> "Light"
            intensity < 0.6f -> "Moderate"
            intensity < 0.8f -> "Vigorous"
            else -> "Extreme"
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Type", "Duration (min)", "Intensity", "Intensity Level", "Notes"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val dateTime = dataPoint.timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        val date = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        
        // Format intensity with appropriate precision
        val intensity = when (val i = dataPoint.value["intensity"]) {
            is Number -> {
                val floatValue = i.toFloat()
                String.format("%.1f", floatValue)
            }
            else -> "0.5"
        }
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Type" to (dataPoint.value["type"]?.toString() ?: ""),
            "Duration (min)" to (dataPoint.value["duration_minutes"]?.toString() ?: ""),
            "Intensity" to intensity,
            "Intensity Level" to (dataPoint.value["intensity_label"]?.toString() ?: ""),
            "Notes" to (dataPoint.value["notes"]?.toString() ?: "")
        )
    }
    
    /**
     * Custom formatter for movement data display
     */
    override fun getDataFormatter(): PluginDataFormatter = MovementDataFormatter()
    
    /**
     * Inner class for custom movement data formatting
     */
    private inner class MovementDataFormatter : PluginDataFormatter {
        
        override fun formatSummary(dataPoint: DataPoint): String {
            val type = dataPoint.value["type"]?.toString() ?: "Movement"
            val duration = when (val d = dataPoint.value["duration_minutes"]) {
                is Number -> d.toInt()
                else -> 0
            }
            val intensity = when (val i = dataPoint.value["intensity"]) {
                is Number -> i.toFloat()
                else -> 0.5f
            }
            
            // Format duration nicely
            val durationText = when {
                duration < 60 -> "${duration}min"
                duration % 60 == 0 -> "${duration / 60}h"
                else -> "${duration / 60}h ${duration % 60}min"
            }
            
            // Add intensity indicator
            val intensityIcon = when {
                intensity < 0.3f -> "🟢"  // Light
                intensity < 0.7f -> "🟡"  // Moderate
                else -> "🔴"  // Vigorous/Extreme
            }
            
            return "$type - $durationText $intensityIcon"
        }
        
        override fun formatDetails(dataPoint: DataPoint): List<DataField> {
            val fields = mutableListOf<DataField>()
            
            // Movement type
            val type = dataPoint.value["type"]?.toString() ?: "Unknown"
            fields.add(
                DataField(
                    label = "Activity",
                    value = type,
                    isImportant = true
                )
            )
            
            // Duration - formatted as integer minutes
            val duration = when (val d = dataPoint.value["duration_minutes"]) {
                is Number -> d.toInt()
                else -> 0
            }
            val durationText = when {
                duration < 60 -> "$duration minutes"
                duration % 60 == 0 -> "${duration / 60} hour${if (duration / 60 > 1) "s" else ""}"
                else -> "${duration / 60} hour${if (duration / 60 > 1) "s" else ""} ${duration % 60} minutes"
            }
            fields.add(
                DataField(
                    label = "Duration",
                    value = durationText,
                    isImportant = true
                )
            )
            
            // Intensity - show as percentage and label
            val intensity = when (val i = dataPoint.value["intensity"]) {
                is Number -> i.toFloat()
                else -> 0.5f
            }
            val intensityPercent = (intensity * 100).roundToInt()
            val intensityLabel = dataPoint.value["intensity_label"]?.toString() 
                ?: getIntensityLabel(intensity)
            fields.add(
                DataField(
                    label = "Intensity",
                    value = "$intensityPercent% ($intensityLabel)",
                    isImportant = true
                )
            )
            
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
            
            // Notes if present
            val notes = dataPoint.value["notes"]?.toString()
            if (!notes.isNullOrBlank()) {
                fields.add(
                    DataField(
                        label = "Notes",
                        value = notes,
                        isLongText = true
                    )
                )
            }
            
            return fields
        }
        
        override fun getHiddenFields(): List<String> = listOf(
            "metadata", 
            "timestamp", 
            "source", 
            "version", 
            "inputType",
            "has_notes",
            "intensity_label"  // Hide since we show it with the intensity value
        )
    }
}
