package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.util.UUID

/**
 * Poo Plugin - Track bowel movements using the Bristol Stool Chart
 * and subjective feeling about the experience
 * 
 * Bristol Stool Chart Types:
 * Type 1: Pebbles - Separate hard lumps
 * Type 2: Lumpy - Sausage-shaped but lumpy
 * Type 3: Dry - Like a sausage with cracks on surface
 * Type 4: Smooth - Like a sausage or snake, smooth and soft (ideal)
 * Type 5: Blobs - Soft blobs with clear-cut edges
 * Type 6: Mushy - Fluffy pieces with ragged edges
 * Type 7: Bumwee - Watery, no solid pieces, entirely liquid
 */
class PooPlugin : Plugin {
    override val id = "poo"
    
    override val metadata = PluginMetadata(
        name = "Poo",
        description = "Track bowel movements using the Bristol Stool Chart and how you feel",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.HEALTH,
        tags = listOf("health", "digestive", "bowel", "bristol", "stool", "poo", "bathroom"),
        dataPattern = DataPattern.COMPOSITE,  // Multiple values per entry
        inputType = InputType.CAROUSEL,    // Primary type for composite inputs
        supportsMultiStage = false,
        relatedPlugins = listOf("food", "water", "medical"),  // Related to diet and health
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.PRIVATE,  // Medical data is private
        naturalLanguageAliases = listOf(
            "poo", "poop", "bowel movement", "bathroom", "toilet",
            "number 2", "bm", "stool", "went to the bathroom"
        ),
        contextualTriggers = listOf(
            ContextTrigger.AFTER_EVENT,
            ContextTrigger.TIME_OF_DAY
        )
    )
    
    override val securityManifest = PluginSecurityManifest(
        requestedCapabilities = setOf(
            PluginCapability.COLLECT_DATA,
            PluginCapability.READ_OWN_DATA,
            PluginCapability.LOCAL_STORAGE,
            PluginCapability.EXPORT_DATA
        ),
        dataSensitivity = DataSensitivity.PRIVATE,
        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),
        privacyPolicy = "Bowel movement data is highly private health information. It is stored locally with encryption and never shared without your explicit permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your bowel movements for health tracking",
        PluginCapability.READ_OWN_DATA to "View your digestive health history and patterns",
        PluginCapability.LOCAL_STORAGE to "Save your bowel movement data securely on your device",
        PluginCapability.EXPORT_DATA to "Export your data for medical consultations or personal analysis"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "poo_entry",
        title = "Record Bowel Movement",
        inputType = InputType.CAROUSEL,  // Primary type, but composite handled by inputs field
        showValue = true,  // Show values for Bristol scale
        primaryColor = "#8D6E63",   // Brown color theme
        secondaryColor = "#D7CCC8",  // Light brown
        inputs = listOf(
            // Bristol Stool Chart carousel selector
            QuickAddInput(
                id = "bristol_type",
                label = "Bristol Type",
                type = InputType.CAROUSEL,
                defaultValue = 4,  // Type 4 (Smooth) is ideal
                options = listOf(
                    QuickOption(label = "Pebbles", value = 1),
                    QuickOption(label = "Lumpy", value = 2),
                    QuickOption(label = "Dry", value = 3),
                    QuickOption(label = "Smooth", value = 4),
                    QuickOption(label = "Blobs", value = 5),
                    QuickOption(label = "Mushy", value = 6),
                    QuickOption(label = "Bumwee", value = 7)
                ),
                required = true
            ),
            // Feeling slider (0-100, displayed as Nah to Yeah)
            QuickAddInput(
                id = "feeling",
                label = "How do you feel about your poo?",
                type = InputType.HORIZONTAL_SLIDER,
                min = 0,
                max = 100,
                defaultValue = 50,
                bottomLabel = "Nah",  // Left side - Bad
                topLabel = "Yeah",    // Right side - Good
                required = true
            )
        )
    )
    
    override suspend fun collectData(): DataPoint? {
        // This plugin only supports manual entry
        return null
    }
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Bristol type comes from carousel as a number (1-7)
        val bristolType = (data["bristol_type"] as? Number)?.toInt() ?: return null
        val feeling = (data["feeling"] as? Number)?.toInt() ?: return null
        
        // Get the single-word label for the Bristol type
        val bristolLabel = when (bristolType) {
            1 -> "Pebbles"
            2 -> "Lumpy"
            3 -> "Dry"
            4 -> "Smooth"
            5 -> "Blobs"
            6 -> "Mushy"
            7 -> "Bumwee"
            else -> "Unknown"
        }
        
        // Determine consistency description based on Bristol type
        val consistency = when (bristolType) {
            1, 2 -> "Hard"
            3, 4, 5 -> "Normal"
            6, 7 -> "Loose"
            else -> "Unknown"
        }
        
        // Determine health indicator
        val healthIndicator = when (bristolType) {
            1, 2 -> "May indicate constipation"
            3, 4 -> "Healthy"
            5 -> "Lacking fiber"
            6, 7 -> "May indicate diarrhea"
            else -> "Unknown"
        }
        
        return DataPoint(
            id = UUID.randomUUID().toString(),
            pluginId = id,
            timestamp = Instant.now(),
            type = "bowel_movement",
            value = mapOf(
                "bristol_type" to bristolType,
                "bristol_label" to bristolLabel,
                "feeling" to feeling,
                "consistency" to consistency,
                "health_indicator" to healthIndicator
            ),
            metadata = mapOf(
                "version" to metadata.version,
                "consistency" to consistency
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val bristolType = (data["bristol_type"] as? Number)?.toInt()
        if (bristolType == null) {
            return ValidationResult.Error("Bristol type is required")
        }
        if (bristolType < 1 || bristolType > 7) {
            return ValidationResult.Error("Bristol type must be between 1 and 7")
        }
        
        val feeling = (data["feeling"] as? Number)?.toInt()
        if (feeling == null) {
            return ValidationResult.Error("Feeling rating is required")
        }
        if (feeling < 0 || feeling > 100) {
            return ValidationResult.Error("Feeling must be between 0 and 100")
        }
        
        return ValidationResult.Success
    }
    
    override fun exportHeaders(): List<String> {
        return listOf(
            "Date",
            "Time", 
            "Bristol Type",
            "Type Description",
            "Consistency",
            "Feeling (0-100)",
            "Feeling Description",
            "Health Indicator"
        )
    }
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val instant = dataPoint.timestamp
        val bristolType = dataPoint.value["bristol_type"]?.toString() ?: "Unknown"
        val bristolLabel = dataPoint.value["bristol_label"]?.toString() ?: getBristolLabel(bristolType.toIntOrNull() ?: 0)
        val feeling = (dataPoint.value["feeling"] as? Number)?.toInt() ?: 50
        
        val feelingDescription = when {
            feeling >= 75 -> "Great"
            feeling >= 50 -> "Okay"
            feeling >= 25 -> "Not great"
            else -> "Poor"
        }
        
        return mapOf(
            "Date" to instant.toString().substring(0, 10),
            "Time" to instant.toString().substring(11, 19),
            "Bristol Type" to bristolType,
            "Type Description" to bristolLabel,
            "Consistency" to (dataPoint.value["consistency"]?.toString() ?: "Unknown"),
            "Feeling (0-100)" to feeling.toString(),
            "Feeling Description" to feelingDescription,
            "Health Indicator" to (dataPoint.value["health_indicator"]?.toString() ?: "Unknown")
        )
    }
    
    /**
     * Helper function to get Bristol label
     */
    private fun getBristolLabel(type: Int): String {
        return when (type) {
            1 -> "Pebbles"
            2 -> "Lumpy"
            3 -> "Dry"
            4 -> "Smooth"
            5 -> "Blobs"
            6 -> "Mushy"
            7 -> "Bumwee"
            else -> "Unknown"
        }
    }
    
    /**
     * Get Bristol type description for display
     */
    fun getBristolDescription(type: Int): String {
        return when (type) {
            1 -> "Type 1: Pebbles - Separate hard lumps"
            2 -> "Type 2: Lumpy - Sausage-shaped but lumpy"
            3 -> "Type 3: Dry - Like a sausage with cracks"
            4 -> "Type 4: Smooth - Smooth and soft (ideal)"
            5 -> "Type 5: Blobs - Soft blobs with clear edges"
            6 -> "Type 6: Mushy - Fluffy pieces, mushy stool"
            7 -> "Type 7: Bumwee - Watery, entirely liquid"
            else -> "Unknown type"
        }
    }
    
    /**
     * Analyze patterns for health insights
     * Note: This is a helper function that could be called by UI components
     */
    fun analyzePattern(recentData: List<DataPoint>): Map<String, Any> {
        if (recentData.isEmpty()) return emptyMap()
        
        val bristolTypes = recentData.mapNotNull { 
            (it.value["bristol_type"] as? Number)?.toInt() 
        }
        
        if (bristolTypes.isEmpty()) return emptyMap()
        
        val averageType = bristolTypes.average()
        val mostCommon = bristolTypes.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key
        val mostCommonLabel = mostCommon?.let { getBristolLabel(it) } ?: "Unknown"
        
        return mapOf(
            "average_bristol_type" to averageType,
            "most_common_type" to (mostCommon ?: 0),
            "most_common_label" to mostCommonLabel,
            "trend" to when {
                averageType < 3 -> "Trending toward constipation"
                averageType > 5 -> "Trending toward loose stools"
                else -> "Normal range"
            },
            "recommendation" to when {
                averageType < 3 -> "Consider increasing water and fiber intake"
                averageType > 5 -> "Monitor diet and consider consulting healthcare provider if persistent"
                else -> "Digestive health appears normal"
            }
        )
    }
}
