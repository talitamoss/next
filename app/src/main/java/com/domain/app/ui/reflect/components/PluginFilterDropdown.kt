// app/src/main/java/com/domain/app/ui/reflect/components/PluginFilterDropdown.kt
package com.domain.app.ui.reflect.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.domain.app.core.plugin.Plugin
import com.domain.app.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
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
    
    // Determine display text
    val displayText = when {
        showAllPlugins -> "All Activities"
        selectedPluginIds.isEmpty() -> "Select Activities"
        selectedPluginIds.size == 1 -> {
            availablePlugins.find { it.id in selectedPluginIds }?.metadata?.name ?: "1 Activity"
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
                tonalElevation = 4.dp,
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    // All option
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
                                if (it) onSelectAll() else onClearAll()
                            }
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "All Activities",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (showAllPlugins) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                    
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    // Plugin list
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState())
                    ) {
                        availablePlugins.forEach { plugin ->
                            val isSelected = plugin.id in selectedPluginIds
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onTogglePlugin(plugin.id) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected || showAllPlugins,
                                    onCheckedChange = { onTogglePlugin(plugin.id) },
                                    enabled = !showAllPlugins
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plugin.metadata.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                                    )
                                    plugin.metadata.description.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                                
                                // Show count if available
                                Badge(
                                    containerColor = if (isSelected || showAllPlugins) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    }
                                ) {
                                    Text(
                                        text = "0", // This will be connected to actual counts
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }
                    
                    // Action buttons
                    if (!showAllPlugins && availablePlugins.isNotEmpty()) {
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = onClearAll) {
                                Text("Clear All")
                            }
                            TextButton(onClick = onSelectAll) {
                                Text("Select All")
                            }
                        }
                    }
                }
            }
        }
    }
}
