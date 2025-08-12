package com.domain.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.app.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPlugins: () -> Unit,
    onNavigateToSecurity: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = AppIcons.Navigation.back,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // User section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "User",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        SettingsItem(
                            icon = Icons.Default.AccountCircle,  // FIX: Using correct icon
                            title = "Profile",
                            subtitle = uiState.userName ?: "Not set",
                            onClick = { /* Navigate to profile */ }
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        SettingsItem(
                            icon = AppIcons.Security.shield,
                            title = "Privacy",
                            subtitle = "Manage your data privacy",
                            onClick = { /* Navigate to privacy */ }
                        )
                    }
                }
            }
            
            // Plugins section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Plugins",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        SettingsItem(
                            icon = AppIcons.Plugin.custom,
                            title = "Manage Plugins",
                            subtitle = "${uiState.enabledPluginCount} enabled",
                            onClick = onNavigateToPlugins
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        SettingsItem(
                            icon = AppIcons.Security.lock,
                            title = "Plugin Security",
                            subtitle = "Permissions and access control",
                            onClick = onNavigateToSecurity
                        )
                    }
                }
            }
            
            // Data section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        SettingsItem(
                            icon = AppIcons.Data.upload,
                            title = "Export Data",
                            subtitle = "Download your data",
                            onClick = { viewModel.exportData() }
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        SettingsItem(
                            icon = AppIcons.Data.download,
                            title = "Import Data",
                            subtitle = "Restore from backup",
                            onClick = { viewModel.importData() }
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        SettingsItem(
                            icon = AppIcons.Action.delete,
                            title = "Clear All Data",
                            subtitle = "Delete all stored data",
                            onClick = { viewModel.clearAllData() },
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            
            // App section
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "App",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SettingsItem(
                                icon = Icons.Default.Palette,  // FIX: Using correct icon for theme
                                title = "Dark Mode",
                                subtitle = if (uiState.isDarkMode) "Enabled" else "Disabled",
                                onClick = null,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = uiState.isDarkMode,
                                onCheckedChange = { viewModel.toggleDarkMode() }
                            )
                        }
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        SettingsItem(
                            icon = AppIcons.Communication.notifications,
                            title = "Notifications",
                            subtitle = if (uiState.notificationsEnabled) "Enabled" else "Disabled",
                            onClick = { /* Navigate to notifications */ }
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        SettingsItem(
                            icon = AppIcons.Status.info,
                            title = "About",
                            subtitle = "Version ${uiState.appVersion}",
                            onClick = onNavigateToAbout
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    contentColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                }
            )
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = contentColor
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = contentColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.7f)
            )
        }
    }
}
