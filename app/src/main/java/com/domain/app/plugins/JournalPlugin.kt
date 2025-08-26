// app/src/main/java/com/domain/app/plugins/JournalPlugin.kt
package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant

/**
 * Simple journal plugin for recording thoughts and reflections
 * Uses the universal QuickAddDialog with minimal changes
 */
class JournalPlugin : Plugin {
    override val id = "journal"
    
    override val metadata = PluginMetadata(
        name = "Journal",
        description = "Record your thoughts and daily reflections",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.JOURNAL,
        tags = listOf("journal", "diary", "writing", "reflection"),
        dataPattern = DataPattern.TEXT,
        inputType = InputType.TEXT,  // Using TEXT type which exists
        supportsMultiStage = false,
        relatedPlugins = listOf("mood"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.SENSITIVE,
        naturalLanguageAliases = listOf(
            "journal", "diary", "write", "thoughts", "reflection"
        ),
        contextualTriggers = listOf(
            ContextTrigger.TIME_OF_DAY,
            ContextTrigger.MANUAL_ONLY
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
        privacyPolicy = "Journal entries are encrypted and stored locally.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED,
        encryptionRequired = true
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Store your journal entries",
        PluginCapability.READ_OWN_DATA to "View your journal history",
        PluginCapability.LOCAL_STORAGE to "Save entries on your device",
        PluginCapability.EXPORT_DATA to "Export your journal"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "value",  // Use standard key
        title = "Journal Entry",
        inputType = InputType.TEXT,
        placeholder = "What's on your mind?",
        buttonText = "Save",
        metadata = mapOf(
            "multiline" to true,  // Signal to dialog to use multiline
            "minLines" to 5,     // Minimum visible lines
            "maxLines" to 15,    // Maximum visible lines
            "minLength" to 10,   // Minimum character count
            "maxLength" to 2000  // Maximum character count
        )
    )
    
    override suspend fun collectData(): DataPoint? = null
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Extract text using standard key
        val entry = (data["value"] as? String)?.trim() ?: return null
        
        // Validate length
        if (entry.length < 10) return null
        
        // Calculate metrics
        val words = entry.split("\\s+".toRegex()).filter { it.isNotEmpty() }
        
        return DataPoint(
            pluginId = id,
            type = "journal_entry",
            value = mapOf(
                "entry" to entry,
                "wordCount" to words.size,
                "charCount" to entry.length
            ),
            metadata = mapOf(
                "input_method" to "manual"
            ),
            timestamp = Instant.now(),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val entry = data["value"] as? String ?: data["entry"] as? String
        
        return when {
            entry == null -> ValidationResult.Error("Entry is required")
            entry.trim().isEmpty() -> ValidationResult.Error("Entry cannot be empty")
            entry.length < 10 -> ValidationResult.Error("Entry too short (min 10 characters)")
            entry.length > 2000 -> ValidationResult.Error("Entry too long (max 2000 characters)")
            else -> ValidationResult.Success
        }
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Entry", "Words"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val date = dataPoint.timestamp.toString().split("T")[0]
        val time = dataPoint.timestamp.toString().split("T")[1].split(".")[0]
        val entry = dataPoint.value["entry"]?.toString() ?: ""
        val wordCount = dataPoint.value["wordCount"]?.toString() ?: "0"
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Entry" to entry,
            "Words" to wordCount
        )
    }
    
    override suspend fun cleanup() {
        // No special cleanup needed
    }
}
