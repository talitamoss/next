package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import java.time.LocalTime

/**
 * Medication tracking plugin with enhanced security for sensitive health data
 */
class MedicationPlugin : Plugin {
    override val id = "medication"
    
    override val metadata = PluginMetadata(
        name = "Medication",
        description = "Track medication adherence",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("medication", "medicine", "pills", "adherence", "health"),
        dataPattern = DataPattern.OCCURRENCE,
        inputType = InputType.CHOICE,
        supportsMultiStage = true,
        relatedPlugins = listOf("symptoms", "mood", "sleep"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.SENSITIVE,
        naturalLanguageAliases = listOf(
            "medication", "medicine", "pill", "pills", "meds",
            "took medication", "took medicine", "took pills",
            "prescription", "dose", "tablet", "capsule"
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
            PluginCapability.EXPORT_DATA,
            PluginCapability.SHOW_NOTIFICATIONS
        ),
        dataSensitivity = DataSensitivity.SENSITIVE,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Medication data is highly sensitive health information. It is encrypted with additional security layers and never leaves your device without explicit consent. Only share with trusted healthcare providers.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record when you take your medications",
        PluginCapability.READ_OWN_DATA to "View your medication history and adherence",
        PluginCapability.LOCAL_STORAGE to "Securely store your medication data with encryption",
        PluginCapability.EXPORT_DATA to "Export medication logs for healthcare providers",
        PluginCapability.SHOW_NOTIFICATIONS to "Remind you to take your medications on time"
    )
    
    override suspend fun initialize(context: Context) {
        // Future: Load saved medication schedules
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddStages() = listOf(
        QuickAddStage(
            id = "medication",
            title = "Which medication?",
            inputType = InputType.TEXT,
            required = true,
            hint = "Medication name"
        ),
        QuickAddStage(
            id = "dosage",
            title = "Dosage",
            inputType = InputType.TEXT,
            required = true,
            hint = "e.g., 500mg, 2 tablets"
        ),
        QuickAddStage(
            id = "timing",
            title = "When taken?",
            inputType = InputType.CHOICE,
            required = true,
            options = listOf(
                QuickOption("Morning", "morning", "☀️"),
                QuickOption("Afternoon", "afternoon", "🌤️"),
                QuickOption("Evening", "evening", "🌙"),
                QuickOption("Night", "night", "🌃"),
                QuickOption("As Needed", "as_needed", "💊")
            )
        ),
        QuickAddStage(
            id = "notes",
            title = "Any notes?",
            inputType = InputType.TEXT,
            required = false,
            hint = "Side effects, etc."
        )
    )
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "Log Medication",
        inputType = InputType.CHOICE,
        options = listOf(
            QuickOption("Morning Dose", "morning", "☀️"),
            QuickOption("Afternoon Dose", "afternoon", "🌤️"),
            QuickOption("Evening Dose", "evening", "🌙"),
            QuickOption("As Needed", "as_needed", "💊"),
            QuickOption("Custom", "custom", "📝")
        )
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val timing = data["timing"] as? String ?: data["value"] as? String ?: "custom"
        val medicationName = data["medication"] as? String ?: ""
        val dosage = data["dosage"] as? String ?: ""
        val taken = data["taken"] as? Boolean ?: true
        val notes = data["notes"] as? String
        
        return DataPoint(
            pluginId = id,
            type = "medication_log",
            value = mapOf(
                "medication" to medicationName,
                "dosage" to dosage,
                "timing" to timing,
                "timing_label" to getTimingLabel(timing),
                "taken" to taken,
                "time" to LocalTime.now().toString(),
                "notes" to (notes ?: ""),
                "emoji" to getEmojiForTiming(timing)
            ),
            metadata = mapOf(
                "quick_add" to "true",
                "adherence" to if (taken) "taken" else "missed",
                "has_notes" to (notes != null).toString()
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val medication = data["medication"] as? String
        val dosage = data["dosage"] as? String
        
        return when {
            medication.isNullOrBlank() -> ValidationResult.Error("Medication name is required")
            dosage.isNullOrBlank() -> ValidationResult.Error("Dosage is required")
            medication.length < 2 -> ValidationResult.Error("Please enter a valid medication name")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Medication", "Dosage", "When Taken", "Taken", "Notes"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Medication" to (dataPoint.value["medication"]?.toString() ?: ""),
            "Dosage" to (dataPoint.value["dosage"]?.toString() ?: ""),
            "When Taken" to (dataPoint.value["timing_label"]?.toString() ?: ""),
            "Taken" to (dataPoint.value["taken"]?.toString() ?: "true"),
            "Notes" to (dataPoint.value["notes"]?.toString() ?: "")
        )
    }
    
    private fun getTimingLabel(timing: String) = when(timing) {
        "morning" -> "Morning"
        "afternoon" -> "Afternoon"
        "evening" -> "Evening"
        "night" -> "Night"
        "as_needed" -> "As Needed"
        else -> "Other"
    }
    
    private fun getEmojiForTiming(timing: String) = when(timing) {
        "morning" -> "☀️"
        "afternoon" -> "🌤️"
        "evening" -> "🌙"
        "night" -> "🌃"
        "as_needed" -> "💊"
        else -> "📝"
    }
}
