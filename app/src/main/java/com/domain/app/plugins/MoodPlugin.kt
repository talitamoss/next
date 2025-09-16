package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant

/**
 * Mood Tracking Plugin
 * Tracks emotional state with specific emotions and intensity levels
 * Uses composite input approach for single-screen data entry
 */
class MoodPlugin : Plugin {
    override val id = "mood"
    
    override val metadata = PluginMetadata(
        name = "Mood",
        description = "Track your emotional state and mood patterns",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.MENTAL_WELLNESS,
        tags = listOf("mood", "emotion", "feeling", "mental health", "wellness"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.CHOICE,  // Primary input type
        supportsMultiStage = false,    // Using composite instead of multi-stage
        relatedPlugins = listOf("journal", "sleep", "meditation", "social"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.SENSITIVE,
        naturalLanguageAliases = listOf(
            "feeling", "emotion", "mood", "how am i feeling",
            "emotional state", "mental state", "vibe", "spirits"
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
        privacyPolicy = "Mood data is stored locally and encrypted. Your emotional data is never shared without explicit permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override suspend fun initialize(context: Context) {
        // Plugin initialization if needed
    }
    
    override fun supportsManualEntry(): Boolean = true
    
    override fun supportsAutomaticCollection(): Boolean = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Track Your Mood",
        inputType = InputType.CHOICE,  // This determines the dialog type
        // Using composite inputs list for single-screen display
        inputs = listOf(
            // Input 1: Emotion selection
            QuickAddInput(
                id = "emotion",
                label = "How are you feeling?",
                type = InputType.CHOICE,
                options = listOf(
                    // Positive High Energy
                    QuickOption("Excited", "excited"),
                    QuickOption("Joyful", "joyful"),
                    QuickOption("Energetic", "energetic"),
                    QuickOption("Enthusiastic", "enthusiastic"),
                    QuickOption("Playful", "playful"),
                    
                    // Positive Low Energy
                    QuickOption("Peaceful", "peaceful"),
                    QuickOption("Content", "content"),
                    QuickOption("Relaxed", "relaxed"),
                    QuickOption("Grateful", "grateful"),
                    QuickOption("Calm", "calm"),
                    
                    // Negative High Energy
                    QuickOption("Angry", "angry"),
                    QuickOption("Frustrated", "frustrated"),
                    QuickOption("Anxious", "anxious"),
                    QuickOption("Stressed", "stressed"),
                    QuickOption("Overwhelmed", "overwhelmed"),
                    
                    // Negative Low Energy
                    QuickOption("Sad", "sad"),
                    QuickOption("Lonely", "lonely"),
                    QuickOption("Tired", "tired"),
                    QuickOption("Bored", "bored"),
                    QuickOption("Depressed", "depressed"),
                    
                    // Neutral/Mixed
                    QuickOption("Confused", "confused"),
                    QuickOption("Curious", "curious"),
                    QuickOption("Surprised", "surprised"),
                    QuickOption("Thoughtful", "thoughtful"),
                    QuickOption("Indifferent", "indifferent")
                ),
                required = true
            ),
            // Input 2: Intensity selection using CHOICE instead of HORIZONTAL_SLIDER
            QuickAddInput(
                id = "intensity",
                label = "How intense is this feeling?",
                type = InputType.CHOICE,
                options = listOf(
                    QuickOption("1 - Very Mild", 1),
                    QuickOption("2 - Mild", 2),
                    QuickOption("3", 3),
                    QuickOption("4", 4),
                    QuickOption("5 - Moderate", 5),
                    QuickOption("6", 6),
                    QuickOption("7", 7),
                    QuickOption("8 - Strong", 8),
                    QuickOption("9", 9),
                    QuickOption("10 - Very Strong", 10)
                ),
                defaultValue = 5,
                required = true
            ),
            // Input 3: Optional note
            QuickAddInput(
                id = "note",
                label = "Notes (optional)",
                type = InputType.TEXT,
                placeholder = "What's on your mind?",
                required = false
            )
        )
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val emotion = data["emotion"] as? String ?: return null
        val intensity = (data["intensity"] as? Number)?.toInt() ?: 5
        val note = data["note"] as? String
        
        // Determine emotion category for analysis
        val category = getEmotionCategory(emotion)
        
        // Calculate mood score (-10 to +10 based on emotion type and intensity)
        val moodScore = calculateMoodScore(emotion, intensity)
        
        return DataPoint(
            pluginId = id,
            timestamp = Instant.now(),
            type = "mood_entry",
            value = mapOf(
                "emotion" to emotion,
                "intensity" to intensity,
                "category" to category,
                "mood_score" to moodScore,
                "note" to (note ?: "")
            ),
            metadata = mapOf(
                "collection_method" to "manual",
                "version" to metadata.version
            )
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val emotion = data["emotion"] as? String
        val intensity = (data["intensity"] as? Number)?.toInt()
        
        val validEmotions = listOf(
            "excited", "joyful", "energetic", "enthusiastic", "playful",
            "peaceful", "content", "relaxed", "grateful", "calm",
            "angry", "frustrated", "anxious", "stressed", "overwhelmed",
            "sad", "lonely", "tired", "bored", "depressed",
            "confused", "curious", "surprised", "thoughtful", "indifferent"
        )
        
        return when {
            emotion == null -> ValidationResult.Error("Emotion is required")
            emotion !in validEmotions -> ValidationResult.Error("Invalid emotion selected")
            intensity == null -> ValidationResult.Error("Intensity is required")
            intensity < 1 || intensity > 10 -> ValidationResult.Error("Intensity must be between 1 and 10")
            else -> ValidationResult.Success
        }
    }
    
    override fun getPermissionRationale(): Map<PluginCapability, String> {
        return mapOf(
            PluginCapability.COLLECT_DATA to "To record your mood entries",
            PluginCapability.READ_OWN_DATA to "To show your mood history and patterns",
            PluginCapability.LOCAL_STORAGE to "To save your mood data locally",
            PluginCapability.EXPORT_DATA to "To export your mood data for personal analysis"
        )
    }
    
    override fun exportHeaders(): List<String> {
        return listOf(
            "Date",
            "Time",
            "Emotion",
            "Category",
            "Intensity (1-10)",
            "Mood Score",
            "Note",
            "Day of Week",
            "Hour of Day"
        )
    }
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val timestamp = dataPoint.timestamp
        val date = timestamp.toString().split("T")[0]
        val time = timestamp.toString().split("T").getOrNull(1)?.split(".")?.get(0) ?: ""
        
        // Extract hour for pattern analysis
        val hour = time.split(":").firstOrNull()?.toIntOrNull() ?: 0
        val dayOfWeek = java.time.LocalDateTime.ofInstant(timestamp, java.time.ZoneOffset.UTC)
            .dayOfWeek.toString()
        
        // Format emotion name
        val emotionFormatted = (dataPoint.value["emotion"]?.toString() ?: "")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Emotion" to emotionFormatted,
            "Category" to (dataPoint.value["category"]?.toString() ?: ""),
            "Intensity (1-10)" to (dataPoint.value["intensity"]?.toString() ?: ""),
            "Mood Score" to (dataPoint.value["mood_score"]?.toString() ?: ""),
            "Note" to (dataPoint.value["note"]?.toString() ?: ""),
            "Day of Week" to dayOfWeek,
            "Hour of Day" to hour.toString()
        )
    }
    
    /**
     * Categorize emotions for analysis
     */
    private fun getEmotionCategory(emotion: String): String {
        return when (emotion) {
            in listOf("excited", "joyful", "energetic", "enthusiastic", "playful") -> 
                "positive_high"
            in listOf("peaceful", "content", "relaxed", "grateful", "calm") -> 
                "positive_low"
            in listOf("angry", "frustrated", "anxious", "stressed", "overwhelmed") -> 
                "negative_high"
            in listOf("sad", "lonely", "tired", "bored", "depressed") -> 
                "negative_low"
            else -> "neutral"
        }
    }
    
    /**
     * Calculate mood score based on emotion and intensity
     * Returns value between -10 and +10
     */
    private fun calculateMoodScore(emotion: String, intensity: Int): Float {
        val baseScore = when (getEmotionCategory(emotion)) {
            "positive_high" -> 1.0f
            "positive_low" -> 0.7f
            "negative_high" -> -0.7f
            "negative_low" -> -1.0f
            else -> 0.0f
        }
        
        // Scale by intensity (1-10)
        return baseScore * intensity
    }
    
    /**
     * Custom formatter for mood data display in Reflect page
     */
    override fun getDataFormatter(): PluginDataFormatter = MoodDataFormatter()
    
    /**
     * Inner class for custom mood data formatting
     */
    private inner class MoodDataFormatter : PluginDataFormatter {
        
        override fun formatSummary(dataPoint: DataPoint): String {
            val emotion = (dataPoint.value["emotion"]?.toString() ?: "unknown")
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            
            val intensity = dataPoint.value["intensity"] as? Int ?: 5
            
            // Build summary string
            return "$emotion (${intensity}/10)"
        }
        
        override fun formatDetails(dataPoint: DataPoint): List<DataField> {
            val fields = mutableListOf<DataField>()
            
            // Emotion field
            val emotion = (dataPoint.value["emotion"]?.toString() ?: "unknown")
                .replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            fields.add(
                DataField(
                    label = "Emotion",
                    value = emotion,
                    isImportant = true
                )
            )
            
            // Intensity field
            val intensity = dataPoint.value["intensity"] as? Int ?: 5
            val intensityDescription = when {
                intensity <= 3 -> "Mild"
                intensity <= 6 -> "Moderate"
                intensity <= 8 -> "Strong"
                else -> "Very Strong"
            }
            fields.add(
                DataField(
                    label = "Intensity",
                    value = "$intensity/10 ($intensityDescription)"
                )
            )
            
            // Category field
            val category = when (dataPoint.value["category"]?.toString()) {
                "positive_high" -> "Positive (High Energy)"
                "positive_low" -> "Positive (Low Energy)"
                "negative_high" -> "Negative (High Energy)"
                "negative_low" -> "Negative (Low Energy)"
                else -> "Neutral"
            }
            fields.add(
                DataField(
                    label = "Category",
                    value = category
                )
            )
            
            // Mood score field
            val moodScore = dataPoint.value["mood_score"] as? Float ?: 0f
            val moodDescription = when {
                moodScore > 5 -> "Very Positive"
                moodScore > 0 -> "Positive"
                moodScore == 0f -> "Neutral"
                moodScore > -5 -> "Negative"
                else -> "Very Negative"
            }
            fields.add(
                DataField(
                    label = "Mood Score",
                    value = "${moodScore.toInt()}/10 ($moodDescription)"
                )
            )
            
            // Note field (if present)
            val note = dataPoint.value["note"]?.toString()
            if (!note.isNullOrEmpty()) {
                fields.add(
                    DataField(
                        label = "Note",
                        value = note,
                        isLongText = true
                    )
                )
            }
            
            // Time field
            val timeParts = dataPoint.timestamp.toString().split("T")
            if (timeParts.size > 1) {
                val timeSubParts = timeParts[1].split(".")
                if (timeSubParts.isNotEmpty()) {
                    fields.add(
                        DataField(
                            label = "Time",
                            value = timeSubParts[0]
                        )
                    )
                }
            }
            
            return fields
        }
    }
}
