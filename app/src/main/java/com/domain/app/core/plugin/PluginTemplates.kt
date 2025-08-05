package com.domain.app.core.plugin

/**
 * Predefined templates for common plugin types
 * Makes plugin creation extremely fast with sensible defaults
 */
object PluginTemplates {
    
    /**
     * Template for simple counter/tally plugins
     */
    fun counterTemplate(
        name: String,
        thingToBeCounted: String,
        icon: String = "📊"
    ) = PluginSpec(
        name = name,
        description = "Track and count $thingToBeCounted",
        category = PluginCategory.PRODUCTIVITY,
        tags = listOf("counter", "tally", "tracking", thingToBeCounted.lowercase()),
        dataPattern = DataPattern.CUMULATIVE,
        inputType = InputType.CHOICE,
        aliases = listOf(
            "count $thingToBeCounted",
            "tally $thingToBeCounted",
            "track $thingToBeCounted",
            "add $thingToBeCounted",
            "+1 $thingToBeCounted"
        ),
        quickAddOptions = listOf(
            QuickOption("+1", 1, icon),
            QuickOption("+5", 5, icon),
            QuickOption("+10", 10, icon),
            QuickOption("Custom", -1, icon)
        ),
        exportHeaders = listOf("Date", "Time", "Count", "Note"),
        collectRationale = "Count $thingToBeCounted occurrences",
        readRationale = "View your $thingToBeCounted counting history"
    )
    
    /**
     * Template for rating/scale plugins (1-5 or 1-10)
     */
    fun ratingTemplate(
        name: String,
        whatBeingRated: String,
        scaleMax: Int = 5,
        sensitivity: DataSensitivity = DataSensitivity.NORMAL
    ) = PluginSpec(
        name = name,
        description = "Rate your $whatBeingRated on a scale of 1-$scaleMax",
        category = if (sensitivity == DataSensitivity.SENSITIVE) PluginCategory.MENTAL_WELLNESS else PluginCategory.LIFESTYLE,
        tags = listOf("rating", "scale", "subjective", whatBeingRated.lowercase()),
        dataPattern = DataPattern.RATING,
        inputType = InputType.SCALE,
        dataSensitivity = sensitivity,
        scaleMax = scaleMax,
        aliases = listOf(
            "rate $whatBeingRated",
            "score $whatBeingRated",
            "how was $whatBeingRated",
            "$whatBeingRated rating",
            "my $whatBeingRated"
        ),
        quickAddTitle = "How would you rate your $whatBeingRated?",
        exportHeaders = listOf("Date", "Time", "Rating (1-$scaleMax)", "Note"),
        validation = listOf(
            ValidationRule("required", "value", "Rating is required"),
            ValidationRule("range", "value", "Rating must be between 1 and $scaleMax", 1, scaleMax)
        ),
        collectRationale = "Rate your $whatBeingRated experiences",
        readRationale = "View your $whatBeingRated rating patterns"
    )
    
    /**
     * Template for duration/time tracking plugins
     */
    fun durationTemplate(
        name: String,
        activity: String,
        category: PluginCategory = PluginCategory.PRODUCTIVITY
    ) = PluginSpec(
        name = name,
        description = "Track time spent on $activity",
        category = category,
        tags = listOf("duration", "time", "tracking", activity.lowercase()),
        dataPattern = DataPattern.DURATION,
        inputType = InputType.DURATION,
        unit = "minutes",
        aliases = listOf(
            "track $activity time",
            "log $activity",
            "time spent on $activity",
            "$activity duration",
            "record $activity"
        ),
        quickAddTitle = "How long did you spend on $activity?",
        quickAddOptions = listOf(
            QuickOption("15 min", 15, "⏱️"),
            QuickOption("30 min", 30, "⏱️"),
            QuickOption("1 hour", 60, "⏱️"),
            QuickOption("2 hours", 120, "⏱️"),
            QuickOption("Custom", -1, "⏱️")
        ),
        exportHeaders = listOf("Date", "Time", "Duration (minutes)", "Category", "Note"),
        validation = listOf(
            ValidationRule("required", "duration", "Duration is required"),
            ValidationRule("positive", "duration", "Duration must be positive")
        ),
        collectRationale = "Track time spent on $activity",
        readRationale = "View your $activity time patterns"
    )
    
    /**
     * Generate code for multiple plugins at once
     */
    fun generateMultiplePlugins(specs: List<PluginSpec>): String {
        return buildString {
            specs.forEach { spec ->
                appendLine(PluginGenerator.generatePlugin(spec))
                appendLine()
                appendLine("// " + "=".repeat(50))
                appendLine()
            }
        }
    }
    
    /**
     * Quick helper to generate a simple plugin
     */
    fun quickPlugin(name: String, description: String, type: String): String {
        val spec = when (type.lowercase()) {
            "counter" -> counterTemplate(name, description)
            "rating" -> ratingTemplate(name, description)
            "duration" -> durationTemplate(name, description)
            else -> PluginSpec(name = name, description = description)
        }
        return PluginGenerator.generatePlugin(spec)
    }
}
