package com.domain.app.plugins

import android.content.Context
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Food tracking plugin with meal type and food selection
 * Uses carousel for meal type and dynamic checkboxes for food items
 */
class FoodPlugin : Plugin {
    override val id = "food"
    
    override val metadata = PluginMetadata(
        name = "Food",
        description = "Track your meals and snacks",
        version = "1.0.0",
        author = "App Team",
        category = PluginCategory.HEALTH,
        tags = listOf("nutrition", "diet", "meals", "food", "eating"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.CAROUSEL,
        supportsMultiStage = false,
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.NORMAL,
        naturalLanguageAliases = listOf("eat", "meal", "snack", "food", "lunch", "dinner", "breakfast"),
        relatedPlugins = listOf("water", "movement", "mood")
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
        privacyPolicy = "Food data is stored locally and never shared without your permission.",
        dataRetention = DataRetentionPolicy.USER_CONTROLLED
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    // Food options for each meal type
    private val snackOptions = listOf(
        QuickOption("Nuts", "nuts"),
        QuickOption("Fruit", "fruit"),
        QuickOption("Baked Goods", "baked_goods"),
        QuickOption("Protein Bar", "protein_bar"),
        QuickOption("Chips", "chips"),
        QuickOption("Candy", "candy"),
        QuickOption("Yogurt", "yogurt"),
        QuickOption("Vegetables", "vegetables"),
        QuickOption("Cheese", "cheese")
    )
    
    private val lightMealOptions = listOf(
        QuickOption("Carb Heavy", "carb_heavy"),
        QuickOption("Protein Focused", "protein_focused"),
        QuickOption("Salad", "salad"),
        QuickOption("Soup", "soup"),
        QuickOption("Sandwich", "sandwich"),
        QuickOption("Smoothie", "smoothie"),
        QuickOption("Leftovers", "leftovers"),
        QuickOption("Fast Food", "fast_food")
    )
    
    private val fullMealOptions = listOf(
        QuickOption("Balanced", "balanced"),
        QuickOption("Protein Heavy", "protein_heavy"),
        QuickOption("Vegetarian", "vegetarian"),
        QuickOption("Vegan", "vegan"),
        QuickOption("Pasta/Rice", "pasta_rice"),
        QuickOption("Pizza", "pizza"),
        QuickOption("Restaurant", "restaurant"),
        QuickOption("Home Cooked", "home_cooked"),
        QuickOption("Takeout", "takeout")
    )
    
    override fun getPermissionRationale() = mapOf(
        PluginCapability.COLLECT_DATA to "Record your food intake",
        PluginCapability.READ_OWN_DATA to "View your meal history",
        PluginCapability.LOCAL_STORAGE to "Save your food data on your device",
        PluginCapability.EXPORT_DATA to "Export your food log for personal use"
    )
    
    override suspend fun initialize(context: Context) {
        // No special initialization needed
    }
    
    override fun supportsManualEntry() = true
    override fun supportsAutomaticCollection() = false
    
    override fun getQuickAddConfig() = QuickAddConfig(
        id = "food_entry",
        title = "Log Food",
        inputType = InputType.CAROUSEL, // Primary type
        inputs = listOf(
            // Meal type carousel
            QuickAddInput(
                id = "mealType",
                label = "What did you have?",
                type = InputType.CAROUSEL,
                defaultValue = "snack",
                options = listOf(
                    QuickOption("Snack", "snack"),
                    QuickOption("Light Meal", "light_meal"),
                    QuickOption("Full Meal", "full_meal")
                )
            ),
            // Food category selection - changes based on meal type
            // Using CHOICE for now since MULTI_CHOICE isn't implemented
            QuickAddInput(
                id = "foodCategory",
                label = "Type of food",
                type = InputType.CHOICE,
                defaultValue = "balanced",
                options = snackOptions // Default to snacks, will be dynamic in dialog
            ),
            // Portion size slider
            QuickAddInput(
                id = "portion",
                label = "Portion Size",
                type = InputType.HORIZONTAL_SLIDER,
                defaultValue = 1.0f,
                min = 0.5f,
                max = 3.0f,
                topLabel = "Large",
                bottomLabel = "Small"
            ),
            // Satisfaction slider
            QuickAddInput(
                id = "satisfaction",
                label = "Satisfaction",
                type = InputType.HORIZONTAL_SLIDER,
                defaultValue = 0.5f,
                min = 0f,
                max = 1f,
                topLabel = "Very Satisfied",
                bottomLabel = "Still Hungry"
            )
        ),
        primaryColor = "#4CAF50",
        secondaryColor = "#8BC34A"
    )
    
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        val mealType = data["mealType"] as? String ?: "snack"
        val foodCategory = data["foodCategory"] as? String ?: ""
        val portion = (data["portion"] as? Number)?.toFloat() ?: 1.0f
        val satisfaction = (data["satisfaction"] as? Number)?.toFloat() ?: 0.5f
        val notes = data["notes"] as? String ?: ""
        
        return DataPoint(
            id = generateDataPointId(),
            pluginId = id,
            timestamp = Instant.now(),
            type = "food_entry",
            value = mapOf(
                "mealType" to mealType,
                "foodCategory" to foodCategory,
                "portion" to portion,
                "satisfaction" to satisfaction,
                "notes" to notes
            ),
            metadata = mapOf(
                "version" to metadata.version,
                "inputType" to "manual"
            ),
            source = "manual"
        )
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        val mealType = data["mealType"] as? String
        val portion = (data["portion"] as? Number)?.toFloat()
        
        return when {
            mealType.isNullOrBlank() -> ValidationResult.Error("Meal type is required")
            portion == null -> ValidationResult.Error("Portion is required")
            portion < 0 -> ValidationResult.Error("Portion cannot be negative")
            portion > 5 -> ValidationResult.Warning("That's a very large portion!")
            else -> ValidationResult.Success
        }
    }
    
    private fun generateDataPointId(): String {
        return "${id}_${System.currentTimeMillis()}_${(0..9999).random()}"
    }
    
    override fun exportHeaders() = listOf(
        "Date", "Time", "Meal Type", "Food Category", "Portion", "Satisfaction"
    )
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        val dateTime = dataPoint.timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
        val date = dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val time = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
        
        // Convert meal type value to readable format
        val mealTypeDisplay = when(dataPoint.value["mealType"]?.toString()) {
            "snack" -> "Snack"
            "light_meal" -> "Light Meal"
            "full_meal" -> "Full Meal"  
            else -> dataPoint.value["mealType"]?.toString() ?: ""
        }
        
        // Convert food category to readable format
        val foodCategoryDisplay = dataPoint.value["foodCategory"]?.toString()
            ?.replace("_", " ")
            ?.split(" ")
            ?.joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
            ?: ""
        
        return mapOf(
            "Date" to date,
            "Time" to time,
            "Meal Type" to mealTypeDisplay,
            "Food Category" to foodCategoryDisplay,
            "Portion" to (dataPoint.value["portion"]?.toString() ?: ""),
            "Satisfaction" to (dataPoint.value["satisfaction"]?.toString() ?: "")
        )
    }
}
