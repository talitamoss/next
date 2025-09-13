package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Social interaction tracking plugin
 * Tracks three key factors of social interactions:
 * 1. Group size (number of other people)
 * 2. Location setting (where the interaction occurred)
 * 3. Social battery (energy level after interaction) - INTEGER SCALE -5 to 5
 */
class SocialPlugin : Plugin {
    
    override val id = "social"
    
    override val metadata = PluginMetadata(
        name = "Social",
        description = "Track social interactions and their impact on your energy",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.LIFESTYLE,
        tags = listOf("social", "interaction", "people", "energy", "battery"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.CAROUSEL,
        supportsMultiStage = false,
        relatedPlugins = listOf("mood", "movement", "location", "productivity"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf(
            "social", "met friends", "family dinner", "team meeting",
            "party", "date", "hangout", "coffee with", "lunch meeting",
            "video call", "discord chat", "gathering", "event",
            "conference", "concert", "meeting", "talked to", "saw"
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
            PluginCapability.EXPORT_DATA,
            PluginCapability.ANALYTICS_BASIC
        ),
        dataSensitivity = DataSensitivity.NORMAL,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Social interaction data is stored locally and never shared without your permission. No personal identifiers or names are collected.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    // Group size options for the carousel
    private val groupSizeOptions = listOf(
        GroupSize("1", "One-on-one"),
        GroupSize("2", "Small pair"),
        GroupSize("3", "Small group"),
        GroupSize("5", "Medium group"),
        GroupSize(">5", "Group"),
        GroupSize(">10", "Large group"),
        GroupSize(">50", "Crowd"),
        GroupSize(">100", "Mass gathering")
    )
    
    // Location setting options
    private val locationOptions = listOf(
        LocationSetting("indoor_venue", "Indoor Venue"),
        LocationSetting("outdoors", "Outdoors"),
        LocationSetting("home", "Home"),
        LocationSetting("workplace", "Workplace"),
        LocationSetting("transit", "Transit"),
        LocationSetting("online", "Online"),
        LocationSetting("event_space", "Event Space")
    )
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your social interactions",
        PluginCapability.READ_OWN_DATA to "View your social interaction history",
        PluginCapability.LOCAL_STORAGE to "Save your social data on your device",
        PluginCapability.EXPORT_DATA to "Export your social data for personal use",
        PluginCapability.ANALYTICS_BASIC to "Analyze your social patterns and energy levels"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "social_entry",
        title = "Log Social Interaction",
        inputType = InputType.CAROUSEL,
        primaryColor = "#7B68EE",
        secondaryColor = "#E8E4FF",
        inputs = listOf(
            // Group size carousel
            QuickAddInput(
                id = "groupSize",
                label = "Group Size",
                type = InputType.CAROUSEL,
                defaultValue = "1",
                options = groupSizeOptions.map { size ->
                    QuickOption(label = size.label, value = size.value)
                }
            ),
            // Location setting carousel
            QuickAddInput(
                id = "setting",
                label = "Setting",
                type = InputType.CAROUSEL,
                defaultValue = "home",
                options = locationOptions.map { location ->
                    QuickOption(label = location.label, value = location.value)
                }
            ),
            // Social battery horizontal slider - INTEGER SCALE
            QuickAddInput(
                id = "socialBattery",
                label = "Social Battery",
                type = InputType.HORIZONTAL_SLIDER,
                defaultValue = 0f,
                min = -5f,
                max = 5f,
                topLabel = "Amped",
                bottomLabel = "Depleted",
                placeholder = "How did this interaction affect your energy?"
            )
        ),
        presets = listOf(
            QuickOption("Quick coffee", mapOf(
                "groupSize" to "1",
                "setting" to "indoor_venue",
                "socialBattery" to 2f
            )),
            QuickOption("Work meeting", mapOf(
                "groupSize" to "5",
                "setting" to "workplace",
                "socialBattery" to -2f
            )),
            QuickOption("Family dinner", mapOf(
                "groupSize" to "5",
                "setting" to "home",
                "socialBattery" to 1f
            )),
            QuickOption("Big event", mapOf(
                "groupSize" to ">50",
                "setting" to "event_space",
                "socialBattery" to -3f
            ))
        )
    )
    
    override suspend fun collectData(): DataPoint? {
        // Automatic collection not implemented
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val groupSize = data["groupSize"] as? String ?: "1"
        val setting = data["setting"] as? String ?: "home"
        // Convert social battery to integer
        val rawBattery = (data["socialBattery"] as? Number)?.toFloat() ?: 0f
        val socialBattery = rawBattery.roundToInt()  // Round to nearest integer
        
        val notes = data["notes"] as? String
        val timestamp = data["timestamp"] as? Instant ?: Instant.now()
        
        // Create the composite data structure
        val socialData = mutableMapOf<String, Any>(
            "group_size" to groupSize,
            "setting" to setting,
            "social_battery" to socialBattery,  // Store as Int
            "battery_label" to getBatteryLabel(socialBattery)
        )
        
        // Add optional notes if provided
        notes?.let { socialData["notes"] = it }
        
        // Calculate some derived metrics
        socialData["is_group"] = isGroupInteraction(groupSize)
        socialData["is_draining"] = (socialBattery < -1)
        socialData["is_energizing"] = (socialBattery > 1)
        socialData["setting_category"] = getSettingCategory(setting)
        
        return DataPoint(
            id = UUID.randomUUID().toString(),
            pluginId = id,
            timestamp = timestamp,
            type = "social_interaction",
            value = socialData,
            metadata = mapOf(
                "data_version" to "1.0",
                "entry_method" to "manual",
                "has_notes" to (notes != null).toString()
            ),
            source = "manual",
            version = 1
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate group size
        val groupSize = data["groupSize"] as? String
        if (groupSize == null) {
            errors.add("Group size is required")
        } else if (groupSize !in groupSizeOptions.map { it.value }) {
            errors.add("Invalid group size: $groupSize")
        }
        
        // Validate setting
        val setting = data["setting"] as? String
        if (setting == null) {
            errors.add("Setting is required")
        } else if (setting !in locationOptions.map { it.value }) {
            errors.add("Invalid setting: $setting")
        }
        
        // Validate social battery - now as integer
        val socialBattery = when (val battery = data["socialBattery"]) {
            is Number -> battery.toFloat().roundToInt()
            else -> null
        }
        if (socialBattery == null) {
            errors.add("Social battery is required")
        } else if (socialBattery < -5 || socialBattery > 5) {
            errors.add("Social battery must be between -5 and 5")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Error(errors.joinToString("; "))
        }
    }
    
    override fun exportHeaders(): List<String> {
        return listOf(
            "timestamp",
            "group_size",
            "setting",
            "social_battery",
            "battery_label",
            "is_group",
            "is_draining",
            "is_energizing",
            "setting_category",
            "notes"
        )
    }
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val data = dataPoint.value
        val formatter = DateTimeFormatter.ISO_INSTANT
        
        return mapOf(
            "timestamp" to formatter.format(dataPoint.timestamp),
            "group_size" to (data["group_size"]?.toString() ?: ""),
            "setting" to (data["setting"]?.toString() ?: ""),
            "social_battery" to (data["social_battery"]?.toString() ?: "0"),
            "battery_label" to (data["battery_label"]?.toString() ?: ""),
            "is_group" to (data["is_group"]?.toString() ?: "false"),
            "is_draining" to (data["is_draining"]?.toString() ?: "false"),
            "is_energizing" to (data["is_energizing"]?.toString() ?: "false"),
            "setting_category" to (data["setting_category"]?.toString() ?: ""),
            "notes" to (data["notes"]?.toString() ?: "")
        )
    }
    
    override suspend fun cleanup() {
        // No cleanup needed
    }
    
    /**
     * Custom formatter to ensure integer display for social battery
     */
    override fun getDataFormatter(): PluginDataFormatter = SocialDataFormatter()
    
    /**
     * Inner class for custom social data formatting
     * Ensures social battery displays as integer without decimals
     */
    private inner class SocialDataFormatter : PluginDataFormatter {
        
        override fun formatSummary(dataPoint: DataPoint): String {
            val groupSize = dataPoint.value["group_size"]?.toString() ?: "Unknown"
            val setting = dataPoint.value["setting"]?.toString() ?: "Unknown"
            val battery = when (val b = dataPoint.value["social_battery"]) {
                is Number -> b.toInt()
                else -> 0
            }
            
            // Format group size nicely
            val groupLabel = groupSizeOptions.find { it.value == groupSize }?.label ?: groupSize
            
            // Format setting nicely
            val settingLabel = locationOptions.find { it.value == setting }?.label ?: setting
            
            // Build summary with battery indicator
            val batteryIcon = when {
                battery <= -3 -> "🔴"  // Very drained
                battery < 0 -> "🟠"    // Somewhat drained
                battery == 0 -> "🟡"   // Neutral
                battery <= 2 -> "🟢"   // Somewhat energized
                else -> "⚡"           // Very energized
            }
            
            return "$groupLabel at $settingLabel $batteryIcon"
        }
        
        override fun formatDetails(dataPoint: DataPoint): List<DataField> {
            val fields = mutableListOf<DataField>()
            
            // Group size
            val groupSize = dataPoint.value["group_size"]?.toString() ?: "Unknown"
            val groupLabel = groupSizeOptions.find { it.value == groupSize }?.label ?: groupSize
            fields.add(
                DataField(
                    label = "Group Size",
                    value = groupLabel
                )
            )
            
            // Setting
            val setting = dataPoint.value["setting"]?.toString() ?: "Unknown"
            val settingLabel = locationOptions.find { it.value == setting }?.label ?: setting
            fields.add(
                DataField(
                    label = "Setting",
                    value = settingLabel
                )
            )
            
            // Social battery - formatted as integer
            val battery = when (val b = dataPoint.value["social_battery"]) {
                is Number -> b.toInt()
                else -> 0
            }
            val batteryLabel = dataPoint.value["battery_label"]?.toString() 
                ?: getBatteryLabel(battery)
            fields.add(
                DataField(
                    label = "Social Battery",
                    value = "$battery ($batteryLabel)",
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
            dataPoint.value["notes"]?.let { notes ->
                if (notes.toString().isNotBlank()) {
                    fields.add(
                        DataField(
                            label = "Notes",
                            value = notes.toString(),
                            isLongText = true
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
            "is_group",
            "is_draining",
            "is_energizing",
            "setting_category",
            "battery_label"  // Hide since we show it with the battery value
        )
    }
    
    // Helper functions
    
    private fun getBatteryLabel(value: Int): String {
        return when (value) {
            -5 -> "Completely Depleted"
            -4 -> "Very Drained"
            -3, -2 -> "Drained"
            -1 -> "Slightly Drained"
            0 -> "Neutral"
            1 -> "Slightly Energized"
            2, 3 -> "Energized"
            4 -> "Very Energized"
            5 -> "Completely Amped"
            else -> "Unknown"
        }
    }
    
    private fun isGroupInteraction(groupSize: String): Boolean {
        return when (groupSize) {
            "1", "2", "3" -> false
            else -> true
        }
    }
    
    private fun getSettingCategory(setting: String): String {
        return when (setting) {
            "home" -> "personal"
            "workplace" -> "professional"
            "indoor_venue", "event_space" -> "public"
            "outdoors", "transit" -> "mobile"
            "online" -> "virtual"
            else -> "other"
        }
    }
    
    // Natural language parsing helper
    fun parseNaturalLanguage(input: String): Map<String, Any>? {
        // Implementation for parsing natural language inputs
        // This is a simplified version - could be expanded with NLP
        val lowercaseInput = input.lowercase()
        
        // Try to detect group size
        val groupSize = when {
            lowercaseInput.contains("one-on-one") || lowercaseInput.contains("1-on-1") -> "1"
            lowercaseInput.contains("couple") || lowercaseInput.contains("pair") -> "2"
            lowercaseInput.contains("small group") -> "3"
            lowercaseInput.contains("team") || lowercaseInput.contains("meeting") -> "5"
            lowercaseInput.contains("party") || lowercaseInput.contains("event") -> ">10"
            lowercaseInput.contains("crowd") || lowercaseInput.contains("concert") -> ">50"
            else -> null
        }
        
        // Try to detect setting
        val setting = when {
            lowercaseInput.contains("home") || lowercaseInput.contains("house") -> "home"
            lowercaseInput.contains("work") || lowercaseInput.contains("office") -> "workplace"
            lowercaseInput.contains("outside") || lowercaseInput.contains("park") -> "outdoors"
            lowercaseInput.contains("online") || lowercaseInput.contains("video") || lowercaseInput.contains("zoom") -> "online"
            lowercaseInput.contains("restaurant") || lowercaseInput.contains("cafe") || lowercaseInput.contains("bar") -> "indoor_venue"
            else -> null
        }
        
        // Try to detect energy impact
        val socialBattery = when {
            lowercaseInput.contains("exhausted") || lowercaseInput.contains("drained") -> -4
            lowercaseInput.contains("tired") -> -2
            lowercaseInput.contains("energized") || lowercaseInput.contains("great") -> 3
            lowercaseInput.contains("good") -> 1
            else -> null
        }
        
        return if (groupSize != null || setting != null || socialBattery != null) {
            mapOf(
                "groupSize" to (groupSize ?: "1"),
                "setting" to (setting ?: "home"),
                "socialBattery" to (socialBattery ?: 0)
            )
        } else {
            null
        }
    }
    
    // Data classes for options
    data class GroupSize(val value: String, val label: String)
    data class LocationSetting(val value: String, val label: String)
}
