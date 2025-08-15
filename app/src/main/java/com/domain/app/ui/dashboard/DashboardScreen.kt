package com.domain.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.app.core.plugin.Plugin
import com.domain.app.ui.theme.AppIcons
import com.domain.app.ui.utils.getPluginIcon
import com.domain.app.ui.components.plugin.quickadd.QuickAddDialog

/**
 * Main dashboard screen with clean 3x4 plugin grid
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToPlugin: (Plugin) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPlugin by remember { mutableStateOf<Plugin?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = AppIcons.Navigation.settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3), // Changed to 3 columns
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), // Reduced spacing
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Generate exactly 12 tiles
            items(12) { index ->
                val plugin = uiState.dashboardPlugins.getOrNull(index)
                
                if (plugin != null) {
                    // Active plugin tile
                    PluginTile(
                        plugin = plugin,
                        onClick = {
                            selectedPlugin = plugin
                        }
                    )
                } else {
                    // Empty placeholder tile
                    EmptyPluginTile()
                }
            }
        }
    }
    
    // Quick add dialog
    selectedPlugin?.let { plugin ->
        QuickAddDialog(
            plugin = plugin,
            onDismiss = { 
                selectedPlugin = null 
            },
            onConfirm = { data ->
                viewModel.onQuickAdd(plugin, data)
                selectedPlugin = null
            }
        )
    }
}

/**
 * Simplified plugin tile - just icon and name, fully clickable
 */
@Composable
private fun PluginTile(
    plugin: Plugin,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Keep tiles square
            .clickable { onClick() }, // Entire tile is clickable
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Plugin icon
            Icon(
                imageVector = getPluginIcon(plugin),
                contentDescription = plugin.metadata.name,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Plugin name
            Text(
                text = plugin.metadata.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Empty placeholder tile for unused plugin slots
 */
@Composable
private fun EmptyPluginTile() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = AppIcons.Action.add,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .alpha(0.3f),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
