package com.domain.app.plugins.health

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import java.time.LocalTime

class MedicationPlugin : Plugin {
    override val id = "medication"
    
    override val metadata = PluginMetadata(
        name = "Medication",
        description = "Track medication adherence",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH
    )
    
    override suspend fun initialize(context: Context) {
        // Future: Could load saved medications
    }
    
    override fun supportsManualEntry() = true
    
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
                "timing" to timing,
                "medication" to medicationName,
                "dosage" to dosage,
                "taken" to taken,
                "time" to LocalTime.now().toString(),
                "notes" to (notes ?: ""),
                "emoji" to getEmojiForTiming(timing)
            ),
            metadata = mapOf(
                "quick_add" to "true",
                "adherence" to if (taken) "taken" else "missed"
            ),
            source = "manual"
        )
    }
    
    private fun getEmojiForTiming(timing: String) = when(timing) {
        "morning" -> "☀️"
        "afternoon" -> "🌤️"
        "evening" -> "🌙"
        "as_needed" -> "💊"
        else -> "📝"
    }
    
    // Future: Add medication schedule support
    fun getMedicationSchedule(): List<MedicationSchedule> {
        // This would be loaded from storage
        return emptyList()
    }
    
    data class MedicationSchedule(
        val name: String,
        val dosage: String,
        val times: List<LocalTime>,
        val notes: String? = null
    )
}
