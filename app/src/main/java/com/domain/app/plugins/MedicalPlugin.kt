package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Medical Tracker Plugin
 * Tracks medications with name, dosage, unit, and optional notes
 * Uses TEXT input type with composite inputs list for single-page display
 */
class MedicalPlugin : Plugin {
    override val id = "medical"
    
    override val metadata = PluginMetadata(
        name = "Medicine Tracker",
        description = "Track your medications and dosages",
        version = "1.0.0", 
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("medicine", "medication", "health", "prescription", "dosage", "pills", "tablets"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.TEXT,           // FIX: Use TEXT (no MULTI_STAGE or COMPOSITE exists)
        supportsMultiStage = false,           // Single page display
        relatedPlugins = listOf("health", "mood", "sleep"),
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.SENSITIVE,
        naturalLanguageAliases = listOf(
            "medicine", "medication", "meds", "pills",
            "took medicine", "took medication", "took pills",
            "prescription", "tablets", "capsules"
        ),
        contextualTriggers = listOf(
            ContextTrigger.TIME_OF_DAY,
            ContextTrigger.AFTER_EVENT        // FIX: Removed REMINDER_BASED (doesn't exist)
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
        privacyPolicy = "Medicine data is encrypted locally and never shared without your explicit permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your medication intake",
        PluginCapability.READ_OWN_DATA to "View your medication history",
        PluginCapability.LOCAL_STORAGE to "Save your medication records securely on your device",
        PluginCapability.EXPORT_DATA to "Export your medication data for sharing with healthcare providers"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    /**
     * Configure quick add with all fields on one screen using inputs list
     */
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "medicine_entry",
        title = "Add Medicine",
        inputType = InputType.TEXT,         // FIX: Use TEXT as base type
        primaryColor = "#4CAF50",           // Green for health/medical
        secondaryColor = "#E8F5E9",          // Light green
        // Use inputs list to show all fields on one screen
        inputs = listOf(
            // Medicine Name Input
            QuickAddInput(
                id = "medicine_name",
                label = "Medicine Name",
                type = InputType.TEXT,
                placeholder = "e.g., Aspirin, Vitamin D",
                required = true
            ),
            // Dosage Amount Input
            QuickAddInput(
                id = "dosage_amount", 
                label = "Dosage Amount",
                type = InputType.TEXT,
                placeholder = "Enter amount",
                required = true,
                min = 0.0
            ),
            // Dosage Unit Selection
            QuickAddInput(
                id = "dosage_unit",
                label = "Unit",
                type = InputType.CHOICE,
                defaultValue = "mg",
                required = true,
                options = listOf(
                    QuickOption("mg", "mg"),
                    QuickOption("g", "g"),
                    QuickOption("ml", "ml"),
                    QuickOption("tablets", "tablets"),
                    QuickOption("capsules", "capsules"),
                    QuickOption("drops", "drops"),
                    QuickOption("IU", "IU"),
                    QuickOption("mcg", "mcg")
                )
            ),
            // Notes Input (Optional)
            QuickAddInput(
                id = "notes",
                label = "Notes (Optional)",
                type = InputType.TEXT,
                placeholder = "e.g., After breakfast, for headache",
                required = false
            )
        ),
        metadata = mapOf(
            "confirmButtonText" to "Save Medicine",
            "cancelButtonText" to "Cancel"
        )
    )
    
    /**
     * Create manual entry from user input
     */
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Extract and validate data
        val medicineName = data["medicine_name"] as? String ?: return null
        val dosageAmount = when (val amount = data["dosage_amount"]) {
            is Number -> amount.toDouble()
            is String -> amount.toDoubleOrNull() ?: return null
            else -> return null
        }
        val dosageUnit = data["dosage_unit"] as? String ?: "mg"
        val notes = data["notes"] as? String ?: ""
        
        // Create DataPoint with composite data
        return DataPoint(
            pluginId = id,
            timestamp = Instant.now(),
            value = mapOf(
                "medicine_name" to medicineName,
                "dosage_amount" to dosageAmount,
                "dosage_unit" to dosageUnit,
                "notes" to notes,
                "time_taken" to Instant.now().toEpochMilli()
            ),
            type = "medicine_intake",
            source = "manual",
            metadata = mapOf(
                "version" to metadata.version,
                "sensitivity" to "sensitive"
            )
        )
    }
    
    /**
     * Validate data point
     */
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate medicine name
        val medicineName = data["medicine_name"] as? String
        when {
            medicineName == null -> errors.add("Medicine name is required")
            medicineName.length < 2 -> errors.add("Medicine name must be at least 2 characters")
            medicineName.length > 100 -> errors.add("Medicine name is too long")
        }
        
        // Validate dosage amount
        val dosageAmount = when (val amount = data["dosage_amount"]) {
            is Number -> amount.toDouble()
            is String -> amount.toDoubleOrNull()
            else -> null
        }
        when {
            dosageAmount == null -> errors.add("Dosage amount is required")
            dosageAmount <= 0 -> errors.add("Dosage amount must be positive")
            dosageAmount > 10000 -> errors.add("Dosage amount seems too high")
        }
        
        // Validate dosage unit
        val validUnits = listOf("mg", "g", "ml", "tablets", "capsules", "drops", "IU", "mcg")
        val dosageUnit = data["dosage_unit"] as? String
        if (dosageUnit != null && dosageUnit !in validUnits) {
            errors.add("Invalid dosage unit")
        }
        
        // Validate notes (optional but check length if provided)
        val notes = data["notes"] as? String
        if (notes != null && notes.length > 500) {
            errors.add("Notes are too long (max 500 characters)")
        }
        
        // FIX: Use actual ValidationResult sealed class structure
        return if (errors.isEmpty()) {
            ValidationResult.Success           // FIX: It's an object, not a function
        } else {
            ValidationResult.Error(             // FIX: Use Error constructor properly
                message = errors.joinToString(", "),
                field = null
            )
        }
    }
    
    /**
     * Define export headers for CSV
     */
    override fun exportHeaders(): List<String> {
        return listOf(
            "Date",
            "Time", 
            "Medicine Name",
            "Dosage Amount",
            "Dosage Unit",
            "Notes"
        )
    }
    
    /**
     * Format data point for export
     */
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val valueMap = dataPoint.value as? Map<*, *> ?: return emptyMap()
        
        // Extract timestamp
        val timestamp = when (val time = valueMap["time_taken"]) {
            is Long -> Instant.ofEpochMilli(time)
            else -> dataPoint.timestamp
        }
        
        val localDateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
        
        // Extract medicine data
        val medicineName = valueMap["medicine_name"]?.toString() ?: "Unknown"
        val dosageAmount = valueMap["dosage_amount"]?.toString() ?: "0"
        val dosageUnit = valueMap["dosage_unit"]?.toString() ?: ""
        val notes = valueMap["notes"]?.toString() ?: ""
        
        return mapOf(
            "Date" to localDateTime.format(DateTimeFormatter.ISO_LOCAL_DATE),
            "Time" to localDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            "Medicine Name" to medicineName,
            "Dosage Amount" to dosageAmount,
            "Dosage Unit" to dosageUnit,
            "Notes" to notes
        )
    }
    
    /**
     * Cleanup resources
     */
    override suspend fun cleanup() {
        // No cleanup needed
    }
}
