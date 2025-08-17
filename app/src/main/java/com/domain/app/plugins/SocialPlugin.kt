package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Social interaction tracking plugin
 * Tracks three key factors of social interactions:
 * 1. Group size (number of other people)
 * 2. Location setting (where the interaction occurred)
 * 3. Social battery (energy level after interaction)
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
            // Social battery horizontal slider
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
        val socialBattery = (data["socialBattery"] as? Number)?.toFloat() ?: 0f
        val notes = data["notes"] as? String
        val timestamp = data["timestamp"] as? Instant ?: Instant.now()
        
        // Create the composite data structure
        val socialData = mutableMapOf<String, Any>(
            "group_size" to groupSize,
            "setting" to setting,
            "social_battery" to socialBattery,
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
        
        // Validate social battery
        val socialBattery = data["socialBattery"] as? Number
        if (socialBattery == null) {
            errors.add("Social battery is required")
        } else {
            val batteryValue = socialBattery.toFloat()
            if (batteryValue < -5f || batteryValue > 5f) {
                errors.add("Social battery must be between -5 and 5")
            }
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
    
    // Helper functions
    
    private fun getBatteryLabel(value: Float): String {
        return when {
            value <= -4 -> "Completely Depleted"
            value <= -2 -> "Drained"
            value <= -0.5 -> "Slightly Drained"
            value < 0.5 -> "Neutral"
            value <= 2 -> "Slightly Energized"
            value <= 4 -> "Energized"
            else -> "Completely Amped"
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
        val lowerInput = input.lowercase()
        
        return when {
            lowerInput.contains("coffee") && lowerInput.contains("friend") -> mapOf(
                "groupSize" to "1",
                "setting" to "indoor_venue"
            )
            lowerInput.contains("team meeting") || lowerInput.contains("work meeting") -> mapOf(
                "groupSize" to "5",
                "setting" to "workplace"
            )
            lowerInput.contains("party") -> mapOf(
                "groupSize" to ">10",
                "setting" to "indoor_venue"
            )
            lowerInput.contains("concert") -> mapOf(
                "groupSize" to ">50",
                "setting" to "event_space"
            )
            lowerInput.contains("conference") -> mapOf(
                "groupSize" to ">100",
                "setting" to "event_space"
            )
            lowerInput.contains("family") && lowerInput.contains("dinner") -> mapOf(
                "groupSize" to "5",
                "setting" to "home"
            )
            lowerInput.contains("walk") && lowerInput.contains("park") -> mapOf(
                "groupSize" to "1",
                "setting" to "outdoors"
            )
            lowerInput.contains("video call") || lowerInput.contains("zoom") -> mapOf(
                "groupSize" to "5",
                "setting" to "online"
            )
            else -> null
        }
    }
    
    // Data classes for type safety
    
    private data class GroupSize(
        val value: String,
        val label: String
    )
    
    private data class LocationSetting(
        val value: String,
        val label: String
    )
}
