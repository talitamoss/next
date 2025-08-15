package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Movement tracking plugin
 * Tracks physical movement and activities with duration and intensity
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
        inputType = InputType.CAROUSEL,  // Primary input type (or could be HORIZONTAL_SLIDER)
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
            // Duration horizontal slider (0 to 120 minutes = 2 hours)
            QuickAddInput(
                id = "duration",
                label = "Duration",
                type = InputType.HORIZONTAL_SLIDER,
                defaultValue = 30f,
                min = 0f,
                max = 120f,
                unit = "min"
            ),
            // Intensity horizontal slider  
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
        val durationMinutes = (data["duration"] as? Number)?.toInt() ?: 30
        val intensity = data["intensity"] as? String ?: "moderate"
        val notes = data["notes"] as? String
        
        return DataPoint(
            id = generateDataPointId(),
            pluginId = id,
            timestamp = Instant.now(),
            type = "movement_entry",
            value = mapOf(
                "type" to movementType,
                "duration_minutes" to durationMinutes,
                "intensity" to intensity,
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
        
        return when {
            type.isNullOrBlank() -> ValidationResult.Error("Movement type is required")
            duration == null -> ValidationResult.Error("Duration is required")
            duration < 0 -> ValidationResult.Error("Duration cannot be negative")
            duration > 720 -> ValidationResult.Warning("That's over 12 hours! Are you sure?")
            else -> ValidationResult.Success
        }
    }
    
    private fun generateDataPointId(): String {
        return "${id}_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Type", "Duration (min)", "Intensity", "Notes"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val dateTime = dataPoint.timestamp.atZone(java.time.ZoneId.systemDefault()).toLocalDateTime()
        val date = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Type" to (dataPoint.value["type"]?.toString() ?: ""),
            "Duration (min)" to (dataPoint.value["duration_minutes"]?.toString() ?: ""),
            "Intensity" to (dataPoint.value["intensity"]?.toString() ?: ""),
            "Notes" to (dataPoint.value["notes"]?.toString() ?: "")
        )
    }
}
