// app/src/main/java/com/domain/app/plugins/ComponentTestPlugin.kt
package com.domain.app.plugins

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.domain.app.core.data.DataPoint
import com.domain.app.core.plugin.*
import com.domain.app.core.plugin.QuickOption  // Explicit import
import com.domain.app.core.plugin.QuickAddInput  // Explicit import
import com.domain.app.core.plugin.QuickAddStage  // Explicit import
import com.domain.app.core.plugin.security.*
import com.domain.app.core.validation.ValidationResult
import com.domain.app.ui.components.core.sliders.*
import com.domain.app.ui.components.core.carousel.*
import java.time.Instant

/**
 * A special plugin for testing and showcasing all UI components.
 * This plugin DOES NOT record any data - it's purely for visual testing.
 */
class ComponentTestPlugin : Plugin {
    override val id = "component_test"
    
    override val metadata = PluginMetadata(
        name = "🧪 Component Test Lab",
        description = "Test and showcase all UI components. No data is recorded.",
        version = "1.0.0",
        author = "System",
        category = PluginCategory.OTHER,
        tags = listOf("test", "debug", "ui", "components", "showcase"),
        dataPattern = DataPattern.COMPOSITE,
        inputType = InputType.CHOICE,
        supportsMultiStage = true,
        exportFormat = ExportFormat.CSV,
        dataSensitivity = DataSensitivity.PUBLIC,
        naturalLanguageAliases = listOf("test components", "ui test", "component showcase"),
        contextualTriggers = listOf(ContextTrigger.MANUAL_ONLY)
    )
    
    override val securityManifest = PluginSecurityManifest(
        requestedCapabilities = setOf(
            PluginCapability.READ_OWN_DATA  // Minimal capability just to load
        ),
        dataSensitivity = DataSensitivity.PUBLIC
    )
    
    override val trustLevel = PluginTrustLevel.OFFICIAL
    
    override suspend fun initialize(context: Context) {
        // No initialization needed for test plugin
    }
    
    override fun supportsManualEntry() = true
    
    override fun supportsAutomaticCollection() = false
    
    /**
     * The main component test configuration.
     * This will display ALL our components in different tabs/sections.
     */
    override fun getQuickAddConfig() = QuickAddConfig(
        title = "🧪 Component Test Lab",
        inputType = InputType.CHOICE,
        inputs = listOf(
            QuickAddInput(
                id = "test_section",
                label = "Select Test Category",
                type = InputType.CHOICE,
                options = listOf(
                    QuickOption("Sliders", "sliders", "🎚️"),
                    QuickOption("Inputs", "inputs", "⌨️"),
                    QuickOption("Selections", "selections", "☑️"),
                    QuickOption("All Components", "all", "🎨")
                ),
                defaultValue = "all"
            )
        ),
        // For multi-stage testing
        stages = listOf(
            QuickAddStage(
                id = "horizontal_slider_test",
                title = "Horizontal Slider Test",
                inputType = InputType.HORIZONTAL_SLIDER,
                defaultValue = 50f,
                min = 0f,
                max = 100f,
                step = 10f,
                unit = "units"
            ),
            QuickAddStage(
                id = "vertical_slider_test",
                title = "Vertical Slider Test",
                inputType = InputType.VERTICAL_SLIDER,
                defaultValue = 3f,
                min = 1f,
                max = 5f,
                step = 1f
            ),
            QuickAddStage(
                id = "carousel_test",
                title = "Carousel Test",
                inputType = InputType.CAROUSEL,
                options = listOf(
                    QuickOption("Option A", "a", "🅰️"),
                    QuickOption("Option B", "b", "🅱️"),
                    QuickOption("Option C", "c", "🌟"),
                    QuickOption("Option D", "d", "🎯"),
                    QuickOption("Option E", "e", "🚀")
                )
            ),
            QuickAddStage(
                id = "text_test",
                title = "Text Input Test",
                inputType = InputType.TEXT,
                placeholder = "Enter test text here..."
            ),
            QuickAddStage(
                id = "number_test",
                title = "Number Input Test",
                inputType = InputType.NUMBER,
                defaultValue = 42,
                min = 0,
                max = 100,
                unit = "items"
            ),
            QuickAddStage(
                id = "choice_test",
                title = "Choice Selection Test",
                inputType = InputType.CHOICE,
                options = listOf(
                    QuickOption("Choice 1", 1, "1️⃣"),
                    QuickOption("Choice 2", 2, "2️⃣"),
                    QuickOption("Choice 3", 3, "3️⃣")
                )
            ),
            QuickAddStage(
                id = "boolean_test",
                title = "Boolean Toggle Test",
                inputType = InputType.BOOLEAN,
                defaultValue = false
            )
        )
    )
    
    /**
     * Don't actually collect any data
     */
    override suspend fun collectData(): DataPoint? = null
    
    /**
     * Override createManualEntry to NOT save anything - just log for testing
     */
    override suspend fun createManualEntry(data: Map<String, Any>): DataPoint? {
        // Log the data but don't actually save it
        println("🧪 Component Test - Received data: $data")
        
        // Return null so nothing gets saved
        return null
    }
    
    override fun validateDataPoint(data: Map<String, Any>): ValidationResult {
        // Always valid for testing
        return ValidationResult.Success
    }
    
    override fun exportHeaders(): List<String> = listOf("test_data")
    
    override fun formatForExport(dataPoint: DataPoint): Map<String, String> {
        return mapOf("test_data" to "test_value")
    }
    
    override fun getPermissionRationale(): Map<PluginCapability, String> = 
        mapOf(
            PluginCapability.READ_OWN_DATA to "Display test components only - no data is saved"
        )
    
    /**
     * Custom composable for comprehensive component testing
     * This would be called from a test screen, not from the plugin directly
     */
    @Composable
    fun ComponentTestScreen() {
        var selectedTab by remember { mutableStateOf(0) }
        val tabs = listOf("Sliders", "Inputs", "Selections", "Special")
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Tab selector
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }
            
            // Content based on selected tab
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> SliderTestSection()
                    1 -> InputTestSection()
                    2 -> SelectionTestSection()
                    3 -> SpecialComponentsSection()
                }
            }
        }
    }
    
    @Composable
    private fun SliderTestSection() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Slider Components Test",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Horizontal Slider Tests
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Horizontal Slider - Basic", style = MaterialTheme.typography.titleMedium)
                    
                    var value1 by remember { mutableStateOf(50f) }
                    HorizontalSlider(
                        value = value1,
                        onValueChange = { value1 = it },
                        valueRange = 0f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Value: ${value1.toInt()}")
                    
                    HorizontalDivider()
                    
                    Text("Horizontal Slider - With Steps", style = MaterialTheme.typography.titleMedium)
                    var value2 by remember { mutableStateOf(5f) }
                    HorizontalSlider(
                        value = value2,
                        onValueChange = { value2 = it },
                        valueRange = 0f..10f,
                        steps = 9,
                        showTicks = true,
                        showValueMarkers = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    HorizontalDivider()
                    
                    Text("Horizontal Slider - Custom Colors", style = MaterialTheme.typography.titleMedium)
                    var value3 by remember { mutableStateOf(30f) }
                    HorizontalSlider(
                        value = value3,
                        onValueChange = { value3 = it },
                        valueRange = 0f..100f,
                        colors = HorizontalSliderDefaults.colors(
                            thumbColor = Color(0xFF6B46C1),
                            activeTrackColor = androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFF6B46C1), Color(0xFF9333EA))
                            )
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            // Vertical Slider Tests
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Vertical Sliders", style = MaterialTheme.typography.titleMedium)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Classic style
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Classic", style = MaterialTheme.typography.labelMedium)
                            var value4 by remember { mutableStateOf(3f) }
                            VerticalSlider(
                                value = value4,
                                onValueChange = { value4 = it },
                                valueRange = 1f..5f,
                                steps = 3,
                                height = 200.dp,
                                style = SliderStyle.Classic,
                                showLabel = true,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        
                        // Modern style
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Modern", style = MaterialTheme.typography.labelMedium)
                            var value5 by remember { mutableStateOf(50f) }
                            VerticalSlider(
                                value = value5,
                                onValueChange = { value5 = it },
                                valueRange = 0f..100f,
                                height = 200.dp,
                                style = SliderStyle.Modern,
                                showLabel = true,
                                colors = VerticalSliderDefaults.colors(
                                    gradientColors = listOf(
                                        Color(0xFFFF6B6B),
                                        Color(0xFFFECA57),
                                        Color(0xFF48C9B0)
                                    )
                                ),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        
                        // With side labels
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("With Labels", style = MaterialTheme.typography.labelMedium)
                            var value6 by remember { mutableStateOf(7f) }
                            VerticalSlider(
                                value = value6,
                                onValueChange = { value6 = it },
                                valueRange = 0f..10f,
                                height = 200.dp,
                                sideLabels = Pair("High", "Low"),
                                showTicks = true,
                                steps = 9,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
            
            // Range Slider Test
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Range Slider", style = MaterialTheme.typography.titleMedium)
                    
                    var startValue by remember { mutableStateOf(20f) }
                    var endValue by remember { mutableStateOf(80f) }
                    
                    RangeSlider(
                        startValue = startValue,
                        endValue = endValue,
                        onRangeChange = { start, end ->
                            startValue = start
                            endValue = end
                        },
                        valueRange = 0f..100f,
                        minRange = 10f,
                        showLabels = true,
                        showTicks = true,
                        steps = 10,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Selected range: ${startValue.toInt()} - ${endValue.toInt()}")
                }
            }
        }
    }
    
    @Composable
    private fun InputTestSection() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Input Components Test",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Text inputs, number inputs, etc.
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    var text by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Text Input Test") },
                        placeholder = { Text("Enter text here...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    var number by remember { mutableStateOf("") }
                    OutlinedTextField(
                        value = number,
                        onValueChange = { if (it.all { char -> char.isDigit() }) number = it },
                        label = { Text("Number Input Test") },
                        placeholder = { Text("Enter numbers only...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    
    @Composable
    private fun SelectionTestSection() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Selection Components Test",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Carousel Test
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Carousel Component", style = MaterialTheme.typography.titleMedium)
                    
                    val options = listOf(
                        CarouselOption("Running", "running", "🏃"),
                        CarouselOption("Cycling", "cycling", "🚴"),
                        CarouselOption("Swimming", "swimming", "🏊"),
                        CarouselOption("Yoga", "yoga", "🧘"),
                        CarouselOption("Weights", "weights", "💪")
                    )
                    var selected by remember { mutableStateOf(options.first()) }
                    
                    Carousel(
                        options = options,
                        selectedOption = selected,
                        onOptionSelected = { selected = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Selected: ${selected.label}")
                }
            }
            
            // Choice chips
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Choice Chips", style = MaterialTheme.typography.titleMedium)
                    
                    var selectedChip by remember { mutableStateOf(0) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Small", "Medium", "Large", "XL").forEachIndexed { index, label ->
                            FilterChip(
                                selected = selectedChip == index,
                                onClick = { selectedChip = index },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
            
            // Boolean switches
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Toggle Switches", style = MaterialTheme.typography.titleMedium)
                    
                    var switch1 by remember { mutableStateOf(false) }
                    var switch2 by remember { mutableStateOf(true) }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Option A")
                        Switch(checked = switch1, onCheckedChange = { switch1 = it })
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Option B")
                        Switch(checked = switch2, onCheckedChange = { switch2 = it })
                    }
                }
            }
        }
    }
    
    @Composable
    private fun SpecialComponentsSection() {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Special Components Test",
                style = MaterialTheme.typography.headlineMedium
            )
            
            // Add any special custom components here
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Preset Slider with Quick Options", style = MaterialTheme.typography.titleMedium)
                    
                    var presetValue by remember { mutableStateOf(250f) }
                    val presets = listOf(
                        com.domain.app.ui.components.core.sliders.QuickOption("250ml", 250f, "🥤"),
                        com.domain.app.ui.components.core.sliders.QuickOption("500ml", 500f, "🧃"),
                        com.domain.app.ui.components.core.sliders.QuickOption("750ml", 750f, "💧"),
                        com.domain.app.ui.components.core.sliders.QuickOption("1L", 1000f, "💦")
                    )
                    
                    HorizontalSliderWithPresets(
                        value = presetValue,
                        onValueChange = { presetValue = it },
                        presets = presets,
                        valueRange = 0f..1500f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Value: ${presetValue.toInt()}ml")
                }
            }
        }
    }
}
