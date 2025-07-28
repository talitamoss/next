package com.domain.app.plugins.wellness.mood

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*

class MoodPlugin : Plugin {
    override val id = "mood"
    
    override val metadata = PluginMetadata(
        name = "Mood",
        description = "Track your emotional well-being",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "How are you feeling?",
        inputType = InputType.CHOICE,
        options = listOf(
            QuickOption("Great", 5, "😊"),
            QuickOption("Good", 4, "🙂"),
            QuickOption("Okay", 3, "😐"),
            QuickOption("Not Great", 2, "😕"),
            QuickOption("Bad", 1, "😢")
        )
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val moodValue = when (val value = data["value"] ?: data["amount"]) {
            is Number -> value.toInt()
            else -> 3 // Default to "Okay"
        }
        
        val note = data["note"] as? String
        
        return DataPoint(
            pluginId = id,
            type = "mood_entry",
            value = mapOf(
                "mood" to moodValue,
                "note" to (note ?: ""),
                "emoji" to getEmojiForMood(moodValue),
                "timestamp" to System.currentTimeMillis()
            ),
            metadata = mapOf(
                "quick_add" to "true"
            ),
            source = "manual"
        )
    }
    
    private fun getEmojiForMood(value: Int) = when(value) {
        5 -> "😊"
        4 -> "🙂"
        3 -> "😐"
        2 -> "😕"
        1 -> "😢"
        else -> "😐"
    }
}
