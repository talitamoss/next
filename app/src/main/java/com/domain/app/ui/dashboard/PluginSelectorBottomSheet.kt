package com.domain.app.ui.dashboard
import com.domain.app.ui.utils.getPluginIcon

import androidx.compose.foundation.clickable
import com.domain.app.ui.utils.getPluginIcon
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCategory
import com.domain.app.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginSelectorBottomSheet(
    availablePlugins: List<Plugin>,
    onDismiss: () -> Unit,
    viewModel: PluginSelectorViewModel = hiltViewModel()
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Add to Dashboard",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
            )
            
            if (availablePlugins.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "All plugins are already on your dashboard",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn {
                    // Group by category
                    val groupedPlugins = availablePlugins.groupBy { it.metadata.category }
                    
                    groupedPlugins.forEach { (category, plugins) ->
                        item {
                            Text(
                                text = category.name.replace("_", " "),
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(
                                    horizontal = 16.dp,
                                    vertical = 8.dp
                                )
                            )
                        }
                        
                        items(plugins) { plugin ->
                            PluginSelectorItem(
                                plugin = plugin,
                                onSelect = {
                                    viewModel.addPluginToDashboard(plugin.id)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PluginSelectorItem(
    plugin: Plugin,
    onSelect: () -> Unit
) {
    ListItem(
        headlineContent = { Text(plugin.metadata.name) },
        supportingContent = { Text(plugin.metadata.description) },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.getPluginIcon(plugin.id),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier.clickable { onSelect() }
    )
}
