package com.domain.app.core.plugin

/**
 * Utility for generating plugin boilerplate code
 * Makes creating new data collection plugins extremely fast
 */
object PluginGenerator {
    
    /**
     * Generate a complete plugin class from a specification
     */
    fun generatePlugin(spec: PluginSpec): String {
        val className = "${spec.name.toCamelCase()}Plugin"
        
        return buildString {
            appendLine("package com.domain.app.plugins")
            appendLine()
            appendLine("import android.content.Context")
            appendLine("import com.domain.app.core.data.DataPoint")
            appendLine("import com.domain.app.core.plugin.*")
            appendLine("import com.domain.app.core.plugin.security.*")
            if (spec.dataPattern == DataPattern.DURATION || spec.inputType == InputType.TIME_PICKER) {
                appendLine("import java.time.LocalTime")
            }
            appendLine()
            appendLine("/**")
            appendLine(" * ${spec.description}")
            appendLine(" */")
            appendLine("class $className : Plugin {")
            appendLine("    override val id = \"${spec.id}\"")
            appendLine()
            
            // Generate metadata
            appendLine("    override val metadata = PluginMetadata(")
            appendLine("        name = \"${spec.name}\",")
            appendLine("        description = \"${spec.description}\",")
            appendLine("        version = \"1.0.0\",")
            appendLine("        author = \"${spec.author}\",")
            appendLine("        category = PluginCategory.${spec.category},")
            appendLine("        tags = listOf(${spec.tags.joinToString { "\"$it\"" }}),")
            appendLine("        dataPattern = DataPattern.${spec.dataPattern},")
            appendLine("        inputType = InputType.${spec.inputType},")
            appendLine("        supportsMultiStage = ${spec.multiStage},")
            appendLine("        relatedPlugins = listOf(${spec.relatedPlugins.joinToString { "\"$it\"" }}),")
            appendLine("        exportFormat = ExportFormat.CSV,")
            appendLine("        dataSensitivity = DataSensitivity.${spec.dataSensitivity},")
            appendLine("        naturalLanguageAliases = listOf(")
            spec.aliases.forEach { alias ->
                appendLine("            \"$alias\",")
            }
            appendLine("        ),")
            appendLine("        contextualTriggers = listOf(${spec.triggers.joinToString { "ContextTrigger.$it" }})")
            appendLine("    )")
            appendLine()
            
            // Generate security manifest
            appendLine("    override val securityManifest = PluginSecurityManifest(")
            appendLine("        requestedCapabilities = setOf(")
            appendLine("            PluginCapability.COLLECT_DATA,")
            appendLine("            PluginCapability.READ_OWN_DATA,")
            appendLine("            PluginCapability.LOCAL_STORAGE,")
            if (spec.needsExport) appendLine("            PluginCapability.EXPORT_DATA,")
            if (spec.needsAnalytics) appendLine("            PluginCapability.ANALYTICS_BASIC,")
            appendLine("        ),")
            appendLine("        dataSensitivity = DataSensitivity.${spec.dataSensitivity},")
            appendLine("        dataAccess = setOf(DataAccessScope.OWN_DATA_ONLY),")
            appendLine("        privacyPolicy = \"${spec.privacyPolicy}\",")
            appendLine("        dataRetention = DataRetentionPolicy.USER_CONTROLLED")
            appendLine("    )")
            appendLine()
            
            appendLine("    override val trustLevel = PluginTrustLevel.OFFICIAL")
            appendLine()
            
            // Generate permission rationale
            appendLine("    override fun getPermissionRationale() = mapOf(")
            appendLine("        PluginCapability.COLLECT_DATA to \"${spec.collectRationale}\",")
            appendLine("        PluginCapability.READ_OWN_DATA to \"${spec.readRationale}\",")
            appendLine("        PluginCapability.LOCAL_STORAGE to \"${spec.storageRationale}\"")
            if (spec.needsExport) appendLine("        ,PluginCapability.EXPORT_DATA to \"${spec.exportRationale}\"")
            appendLine("    )")
            appendLine()
            
            // Generate initialization
            appendLine("    override suspend fun initialize(context: Context) {")
            appendLine("        // No special initialization needed")
            appendLine("    )")
            appendLine()
            
            appendLine("    override fun supportsManualEntry() = true")
            appendLine()
            
            // Generate quick add config
            when (spec.inputType) {
                InputType.CHOICE -> generateChoiceQuickAdd(spec)
                InputType.NUMBER -> generateNumberQuickAdd(spec)
                InputType.SCALE -> generateScaleQuickAdd(spec)
                InputType.DURATION -> generateDurationQuickAdd(spec)
                InputType.TEXT -> generateTextQuickAdd(spec)
                else -> generateGenericQuickAdd(spec)
            }.let { appendLine(it) }
            
            // Generate manual entry method
            appendLine()
            generateManualEntryMethod(spec).let { appendLine(it) }
            
            // Generate validation
            if (spec.validation.isNotEmpty()) {
                appendLine()
                generateValidationMethod(spec).let { appendLine(it) }
            }
            
            // Generate export methods
            appendLine()
            generateExportMethods(spec).let { appendLine(it) }
            
            appendLine("}")
        }
    }
    
    private fun generateChoiceQuickAdd(spec: PluginSpec): String {
        return buildString {
            appendLine("    override fun getQuickAddConfig() = QuickAddConfig(")
            appendLine("        title = \"${spec.quickAddTitle}\",")
            appendLine("        inputType = InputType.CHOICE,")
            appendLine("        options = listOf(")
            spec.quickAddOptions.forEach { option ->
                appendLine("            QuickOption(\"${option.label}\", ${option.value}, \"${option.icon}\"),")
            }
            appendLine("        ),")
            if (spec.unit.isNotEmpty()) appendLine("        unit = \"${spec.unit}\"")
            appendLine("    )")
        }
    }
    
    private fun generateNumberQuickAdd(spec: PluginSpec): String {
        return buildString {
            appendLine("    override fun getQuickAddConfig() = QuickAddConfig(")
            appendLine("        title = \"${spec.quickAddTitle}\",")
            appendLine("        defaultValue = ${spec.defaultValue ?: 1},")
            appendLine("        inputType = InputType.NUMBER,")
            if (spec.unit.isNotEmpty()) appendLine("        unit = \"${spec.unit}\"")
            appendLine("    )")
        }
    }
    
    private fun generateScaleQuickAdd(spec: PluginSpec): String {
        return buildString {
            appendLine("    override fun getQuickAddConfig() = QuickAddConfig(")
            appendLine("        title = \"${spec.quickAddTitle}\",")
            appendLine("        inputType = InputType.SCALE,")
            appendLine("        options = listOf(")
            for (i in 1..spec.scaleMax) {
                val emoji = when {
                    i <= 2 -> "😔"
                    i <= 4 -> "😐"
                    else -> "😊"
                }
                appendLine("            QuickOption(\"$i\", $i, \"$emoji\"),")
            }
            appendLine("        )")
            appendLine("    )")
        }
    }
    
    private fun generateDurationQuickAdd(spec: PluginSpec): String {
        return buildString {
            appendLine("    override fun getQuickAddConfig() = QuickAddConfig(")
            appendLine("        title = \"${spec.quickAddTitle}\",")
            appendLine("        inputType = InputType.DURATION,")
            appendLine("        options = listOf(")
            appendLine("            QuickOption(\"15 min\", 15, \"⏱️\"),")
            appendLine("            QuickOption(\"30 min\", 30, \"⏱️\"),")
            appendLine("            QuickOption(\"1 hour\", 60, \"⏱️\"),")
            appendLine("            QuickOption(\"Custom\", -1, \"⏱️\")")
            appendLine("        ),")
            appendLine("        unit = \"minutes\"")
            appendLine("    )")
        }
    }
    
    private fun generateTextQuickAdd(spec: PluginSpec): String {
        return buildString {
            appendLine("    override fun getQuickAddConfig() = QuickAddConfig(")
            appendLine("        title = \"${spec.quickAddTitle}\",")
            appendLine("        inputType = InputType.TEXT")
            appendLine("    )")
        }
    }
    
    private fun generateGenericQuickAdd(spec: PluginSpec): String {
        return buildString {
            appendLine("    override fun getQuickAddConfig() = QuickAddConfig(")
            appendLine("        title = \"${spec.quickAddTitle}\",")
            appendLine("        inputType = InputType.${spec.inputType}")
            appendLine("    )")
        }
    }
    
    private fun generateManualEntryMethod(spec: PluginSpec): String {
        return buildString {
            appendLine("    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {")
            
            when (spec.dataPattern) {
                DataPattern.SINGLE_VALUE -> {
                    appendLine("        val value = data[\"value\"] ?: data[\"amount\"] ?: return null")
                    appendLine("        val note = data[\"note\"] as? String")
                    appendLine()
                    appendLine("        return DataPoint(")
                    appendLine("            pluginId = id,")
                    appendLine("            value = mapOf(")
                    appendLine("                \"value\" to value,")
                    appendLine("                \"note\" to (note ?: \"\")")
                    appendLine("            )")
                    appendLine("        )")
                }
                DataPattern.COMPOSITE -> {
                    appendLine("        val mainValue = data[\"value\"] ?: return null")
                    appendLine("        val note = data[\"note\"] as? String")
                    appendLine("        val category = data[\"category\"] as? String")
                    appendLine()
                    appendLine("        return DataPoint(")
                    appendLine("            pluginId = id,")
                    appendLine("            value = mapOf(")
                    appendLine("                \"value\" to mainValue,")
                    appendLine("                \"note\" to (note ?: \"\"),")
                    appendLine("                \"category\" to (category ?: \"general\")")
                    appendLine("            )")
                    appendLine("        )")
                }
                DataPattern.CUMULATIVE -> {
                    appendLine("        val amount = data[\"amount\"] ?: data[\"value\"] ?: return null")
                    appendLine("        val note = data[\"note\"] as? String")
                    appendLine()
                    appendLine("        return DataPoint(")
                    appendLine("            pluginId = id,")
                    appendLine("            value = mapOf(")
                    appendLine("                \"amount\" to amount,")
                    appendLine("                \"note\" to (note ?: \"\"),")
                    if (spec.unit.isNotEmpty()) appendLine("                \"unit\" to \"${spec.unit}\"")
                    appendLine("            )")
                    appendLine("        )")
                }
                else -> {
                    appendLine("        return DataPoint(")
                    appendLine("            pluginId = id,")
                    appendLine("            value = data")
                    appendLine("        )")
                }
            }
            
            appendLine("    }")
        }
    }
    
    private fun generateValidationMethod(spec: PluginSpec): String {
        return buildString {
            appendLine("    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {")
            spec.validation.forEach { rule ->
                when (rule.type) {
                    "required" -> {
                        appendLine("        if (data[\"${rule.field}\"] == null) {")
                        appendLine("            return ValidationResult.Error(\"${rule.message}\")")
                        appendLine("        }")
                    }
                    "range" -> {
                        appendLine("        val ${rule.field} = (data[\"${rule.field}\"] as? Number)?.toInt()")
                        appendLine("        if (${rule.field} != null && (${rule.field} < ${rule.min} || ${rule.field} > ${rule.max})) {")
                        appendLine("            return ValidationResult.Error(\"${rule.message}\")")
                        appendLine("        }")
                    }
                    "positive" -> {
                        appendLine("        val ${rule.field} = (data[\"${rule.field}\"] as? Number)?.toDouble()")
                        appendLine("        if (${rule.field} != null && ${rule.field} <= 0) {")
                        appendLine("            return ValidationResult.Error(\"${rule.message}\")")
                        appendLine("        }")
                    }
                }
            }
            appendLine("        return ValidationResult.Success")
            appendLine("    }")
        }
    }
    
    private fun generateExportMethods(spec: PluginSpec): String {
        return buildString {
            appendLine("    override fun exportHeaders() = listOf(")
            spec.exportHeaders.forEach { header ->
                appendLine("        \"$header\",")
            }
            appendLine("    )")
            appendLine()
            appendLine("    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {")
            appendLine("        val date = dataPoint.timestamp.toString().split(\"T\")[0]")
            appendLine("        val time = dataPoint.timestamp.toString().split(\"T\")[1].split(\".\")[0]")
            appendLine()
            appendLine("        return mapOf(")
            appendLine("            \"Date\" to date,")
            appendLine("            \"Time\" to time,")
            spec.exportHeaders.drop(2).forEach { header ->
                val field = header.lowercase().replace(" ", "_").replace("(", "").replace(")", "")
                appendLine("            \"$header\" to (dataPoint.value[\"$field\"]?.toString() ?: \"\"),")
            }
            appendLine("        )")
            appendLine("    }")
        }
    }
    
    private fun String.toCamelCase(): String {
        return split(" ").joinToString("") { word ->
            word.lowercase().replaceFirstChar { it.uppercase() }
        }
    }
}

/**
 * Specification for generating a plugin
 */
data class PluginSpec(
    val name: String,
    val id: String = name.lowercase().replace(" ", "_"),
    val description: String,
    val author: String = "System",
    val category: PluginCategory = PluginCategory.OTHER,
    val tags: List<String> = emptyList(),
    val dataPattern: DataPattern = DataPattern.SINGLE_VALUE,
    val inputType: InputType = InputType.NUMBER,
    val dataSensitivity: DataSensitivity = DataSensitivity.NORMAL,
    val multiStage: Boolean = false,
    val relatedPlugins: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
    val triggers: List<String> = listOf("MANUAL_ONLY"),
    val unit: String = "",
    val defaultValue: Any? = null,
    val scaleMax: Int = 5,
    val quickAddTitle: String = "Add $name",
    val quickAddOptions: List<QuickOption> = emptyList(),
    val validation: List<ValidationRule> = emptyList(),
    val exportHeaders: List<String> = listOf("Date", "Time", "Value", "Note"),
    val needsExport: Boolean = true,
    val needsAnalytics: Boolean = false,
    val collectRationale: String = "Record your $name data",
    val readRationale: String = "View your $name history",
    val storageRationale: String = "Save your $name data on device",
    val exportRationale: String = "Export your $name data",
    val privacyPolicy: String = "$name data is stored locally and encrypted."
)

data class ValidationRule(
    val type: String, // "required", "range", "positive"
    val field: String,
    val message: String,
    val min: Int = 0,
    val max: Int = 100
)
