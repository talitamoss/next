package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*

/**
 * Mood tracking plugin with security manifest
 */
class MoodPlugin : Plugin {
    override val id = "mood"
    
    override val metadata = PluginMetadata(
        name = "Mood",
        description = "Track your emotional well-being",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.MENTAL_WELLNESS,
        tags = listOf("mood", "emotion", "mental-health", "wellbeing", "qualitative"),
        dataPattern = DataPattern.RATING,
        inputType = InputType.CHOICE,
        supportsMultiStage = false,
        relatedPlugins = listOf("sleep", "exercise", "meditation"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.SENSITIVE,
        naturalLanguageAliases = listOf(
            "mood", "feeling", "emotion", "how I feel",
            "I'm feeling", "I feel", "my mood is",
            "happy", "sad", "anxious", "stressed", "calm",
            "excited", "depressed", "angry", "peaceful"
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
        dataSensitivity = DataSensitivity.SENSITIVE,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Mood data is sensitive personal information. It is encrypted and stored locally. We never share this data without your explicit consent.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your mood and emotional state",
        PluginCapability.READ_OWN_DATA to "View your mood history and patterns",
        PluginCapability.LOCAL_STORAGE to "Securely save your mood data on your device",
        PluginCapability.EXPORT_DATA to "Export your mood data for personal analysis or sharing with healthcare providers"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "How are you feeling?",
        inputType = InputType.CHOICE,
        options = listOf(
            QuickOption("Excellent", 5, "😄"),
            QuickOption("Good", 4, "😊"),
            QuickOption("Okay", 3, "😐"),
            QuickOption("Not Great", 2, "😔"),
            QuickOption("Bad", 1, "😢")
        )
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val rating = when (val value = data["value"]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull()
            else -> null
        } ?: return null
        
        val validationResult = validateDataPoint(mapOf("rating" to rating))
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        val emoji = when (rating) {
            5 -> "😄"
            4 -> "😊"
            3 -> "😐"
            2 -> "😔"
            1 -> "😢"
            else -> "😐"
        }
        
        val moodLabel = when (rating) {
            5 -> "Excellent"
            4 -> "Good"
            3 -> "Okay"
            2 -> "Not Great"
            1 -> "Bad"
            else -> "Unknown"
        }
        
        return DataPoint(
            pluginId = id,
            type = "mood_rating",
            value = mapOf(
                "rating" to rating,
                "label" to moodLabel,
                "emoji" to emoji
            ),
            metadata = mapOf(
                "quick_add" to "true",
                "time_of_day" to getTimeOfDay()
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val rating = (data["rating"] as? Number)?.toInt() 
            ?: return ValidationResult.Error("Rating is required")
        
        return when {
            rating !in 1..5 -> ValidationResult.Error("Rating must be between 1 and 5")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Mood Rating", "Mood Label", "Time of Day"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Mood Rating" to (dataPoint.value["rating"]?.toString() ?: ""),
            "Mood Label" to (dataPoint.value["label"]?.toString() ?: ""),
            "Time of Day" to (dataPoint.metadata?.get("time_of_day") ?: "")
        )
    }
    
    private fun getTimeOfDay(): String {
        val hour = java.time.LocalTime.now().hour
        return when (hour) {
            in 5..11 -> "morning"
            in 12..16 -> "afternoon"
            in 17..21 -> "evening"
            else -> "night"
        }
    }
}

