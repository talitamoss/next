// app/src/main/java/com/domain/app/ui/reflect/components/PluginFilterDropdown.kt
package com.domain.app.ui.reflect.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.Plugin
import com.domain.app.ui.theme.AppIcons
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun PluginFilterDropdown(
    availablePlugins: List<Plugin>,
    selectedPluginIds: Set<String>,
    showAllPlugins: Boolean,
    onTogglePlugin: (String) -> Unit,
    onSelectAll: () -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Determine display text based on selection
    val displayText = when {
        availablePlugins.isEmpty() -> "Loading..."
        showAllPlugins -> "All Activities"
        selectedPluginIds.isEmpty() -> "No Activities Selected"
        selectedPluginIds.size == 1 -> {
            availablePlugins.find { it.id == selectedPluginIds.first() }?.metadata?.name ?: "1 Activity"
        }
        else -> "${selectedPluginIds.size} Activities"
    }
    
    Column(modifier = modifier) {
        // Dropdown trigger
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Filter Activities",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    // Debug text to show plugin count
                    if (availablePlugins.isNotEmpty()) {
                        Text(
                            text = "${availablePlugins.size} plugins available",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    imageVector = if (expanded) AppIcons.Control.collapse else AppIcons.Control.expand,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Dropdown content
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                if (availablePlugins.isEmpty()) {
                    // Show loading or empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No plugins available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // "All Activities" option at the top
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    if (!showAllPlugins) {
                                        onSelectAll()
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = showAllPlugins,
                                onCheckedChange = { 
                                    if (it) {
                                        onSelectAll()
                                    } else {
                                        onClearAll()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "All Activities",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (showAllPlugins) FontWeight.Medium else FontWeight.Normal
                            )
                        }
                        
                        // Divider
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        // Scrollable plugin list
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp) // Set max height for scrolling
                                .verticalScroll(rememberScrollState())
                        ) {
                            // Individual plugin options
                            availablePlugins.forEach { plugin ->
                                val isSelected = if (showAllPlugins) {
                                    true
                                } else {
                                    plugin.id in selectedPluginIds
                                }
                                
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            onTogglePlugin(plugin.id)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { 
                                            onTogglePlugin(plugin.id)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = plugin.metadata.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = if (isSelected && !showAllPlugins) {
                                                FontWeight.Medium
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                        if (plugin.metadata.description.isNotBlank()) {
                                            Text(
                                                text = plugin.metadata.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Action buttons
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = onClearAll,
                                enabled = showAllPlugins || selectedPluginIds.isNotEmpty()
                            ) {
                                Text("Clear All")
                            }
                            TextButton(
                                onClick = onSelectAll,
                                enabled = !showAllPlugins
                            ) {
                                Text("Select All")
                            }
                        }
                    }
                }
            }
        }
    }
}
