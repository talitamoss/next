// app/src/main/java/com/domain/app/plugins/MoodPlugin.kt
package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.LocalTime

/**
 * Mood tracking plugin with vertical slider support
 * Built using ONLY existing, verified patterns from other working plugins
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
        inputType = InputType.VERTICAL_SLIDER,  // Using the new vertical slider type
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
        id = "mood",
        title = "How are you feeling?",
        defaultValue = 3,  // Middle of 1-5 scale
        inputType = InputType.VERTICAL_SLIDER,  // Changed from SLIDER to VERTICAL_SLIDER
        min = 1,  // Minimum mood value
        max = 5,  // Maximum mood value
        step = 1, // Integer steps
        unit = "",
        options = listOf(
            QuickOption("😔 Very Bad", 1, "😔"),
            QuickOption("😕 Bad", 2, "😕"),
            QuickOption("😐 Neutral", 3, "😐"),
            QuickOption("🙂 Good", 4, "🙂"),
            QuickOption("😊 Very Good", 5, "😊")
        )
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Pattern verified from WaterPlugin, CoffeePlugin
        val moodValue = when (val value = data["value"] ?: data["mood"]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: return null
            else -> return null
        }
        
        val validationResult = validateDataPoint(mapOf("value" to moodValue))
        if (validationResult is ValidationResult.Error) {
            return null
        }
        
        val note = data["note"] as? String
        
        return DataPoint(
            pluginId = id,
            type = "mood_rating",
            value = mapOf(
                "mood" to moodValue,
                "mood_label" to getMoodLabel(moodValue),
                "note" to (note ?: "")
            ),
            metadata = mapOf(
                "quick_add" to "true",
                "time_of_day" to getTimeOfDay()
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        // Pattern verified from MedicationPlugin, CoffeePlugin
        val value = (data["value"] ?: data["mood"]) as? Number
        
        return when {
            value == null -> ValidationResult.Error("Mood value is required")
            value.toInt() !in 1..5 -> ValidationResult.Error("Mood must be between 1 and 5")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Mood", "MoodLabel", "Note"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        // EXACT pattern from ALL other plugins - verified working
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Mood" to (dataPoint.value["mood"]?.toString() ?: ""),
            "MoodLabel" to (dataPoint.value["mood_label"]?.toString() ?: ""),
            "Note" to (dataPoint.value["note"]?.toString() ?: "")
        )
    }
    
    /**
     * Helper function to get mood label from numeric value
     */
    private fun getMoodLabel(value: Int): String {
        return when (value) {
            1 -> "Very Bad"
            2 -> "Bad"
            3 -> "Neutral"
            4 -> "Good"
            5 -> "Very Good"
            else -> "Unknown"
        }
    }
    
    /**
     * Helper function to get time of day category
     */
    private fun getTimeOfDay(): String {
        val hour = LocalTime.now().hour
        return when {
            hour in 5..11 -> "morning"
            hour in 12..16 -> "afternoon"
            hour in 17..20 -> "evening"
            else -> "night"
        }
    }
}
