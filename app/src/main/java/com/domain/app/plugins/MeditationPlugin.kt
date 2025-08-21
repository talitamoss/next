// app/src/main/java/com/domain/app/plugins/MeditationPlugin.kt
package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.time.LocalDate

class MeditationPlugin : Plugin {
    override val id = "meditation"
    
    override val metadata = PluginMetadata(
        name = "Meditation",
        description = "Simple meditation tracker",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.MENTAL_WELLNESS,
        tags = listOf("meditation", "mindfulness", "wellness"),
        dataPattern = DataPattern.OCCURRENCE,
        inputType = InputType.BUTTON,
        supportsMultiStage = false
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
        privacyPolicy = "Meditation data stored locally only.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override suspend fun initialize(context: Context) {
        // Nothing needed
    }
    
    override fun supportsManualEntry() = true
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "meditation",
        title = "Meditation",
        inputType = InputType.BUTTON,
        buttonText = "I Meditated",
        primaryColor = "#7C3AED"
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint {
        // Just record that it happened
        return DataPoint(
            id = "meditation_${System.currentTimeMillis()}",
            pluginId = id,
            timestamp = Instant.now(),
            type = "meditation_session",
            value = mapOf("completed" to true),
            metadata = mapOf("date" to LocalDate.now().toString()),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        return ValidationResult.Success  // Button press is always valid
    }
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record meditation sessions",
        PluginCapability.READ_OWN_DATA to "View meditation history",
        PluginCapability.LOCAL_STORAGE to "Save meditation data locally",
        PluginCapability.EXPORT_DATA to "Export meditation history"
    )
    
    override fun exportHeaders() = listOf("Date", "Time", "Meditated")
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        // FIX: Use safe call operators for nullable metadata
        val date = dataPoint.metadata?.get("date") ?: ""
        val timeParts = dataPoint.timestamp.toString().split("T")
        val time = if (timeParts.size > 1) {
            timeParts[1].split(".").firstOrNull() ?: ""
        } else {
            ""
        }
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Meditated" to "Yes"
        )
    }
}
