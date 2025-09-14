package com.domain.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.domain.app.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToPlugins: () -> Unit = {},
    onNavigateToDataManagement: () -> Unit = {},
    onNavigateToAbout: () -> Unit = {},
    // Keep these for future when implemented
    onNavigateToAesthetic: () -> Unit = {},
    onNavigateToPrivacy: () -> Unit = {},
    onNavigateToSecurity: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    onNavigateToPluginSecurity: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF3C3F41))
            .padding(paddingValues),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        // ============================================
        // WORKING FEATURES SECTION
        // ============================================
        
        // Plugins Section (WORKING)
        item {
            SettingsSection(title = "Plugins") {
                SettingsItem(
                    icon = Icons.Default.Extension,
                    title = "Manage Plugins",
                    subtitle = "${uiState.enabledPluginCount} plugins enabled",
                    onClick = onNavigateToPlugins,
                    enabled = true
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.Dashboard,
                    title = "Dashboard Configuration",
                    subtitle = "Choose which plugins appear",
                    onClick = { navController.navigate("dashboard_config") },
                    enabled = true
                )
            }
        }
        
        // Data Management Section (WORKING)
        item {
            SettingsSection(title = "Data Management") {
                SettingsItem(
                    icon = Icons.Default.Download,
                    title = "Export Data",
                    subtitle = "Export all data to CSV",
                    onClick = onNavigateToDataManagement,
                    enabled = true
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItemWithSwitch(
                    icon = Icons.Default.Backup,
                    title = "Auto Backup",
                    subtitle = if (uiState.autoBackupEnabled) "Enabled" else "Disabled",
                    checked = uiState.autoBackupEnabled,
                    onCheckedChange = { viewModel.toggleAutoBackup() },
                    enabled = true
                )
                
                if (uiState.autoBackupEnabled) {
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsItem(
                        icon = Icons.Default.Schedule,
                        title = "Backup Frequency",
                        subtitle = uiState.backupFrequency,
                        value = uiState.backupFrequency,
                        onClick = { viewModel.showBackupFrequencyDialog() },
                        enabled = true
                    )
                }
            }
        }
        
        // About Section (WORKING)
        item {
            SettingsSection(title = "About") {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    subtitle = "Version ${BuildConfig.VERSION_NAME}",
                    onClick = onNavigateToAbout,
                    enabled = true
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.PrivacyTip,
                    title = "Privacy Policy",
                    subtitle = "Your data never leaves your device",
                    onClick = { /* Show privacy dialog */ },
                    enabled = true
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.Code,
                    title = "Open Source Licenses",
                    subtitle = "View third-party licenses",
                    onClick = { /* Show licenses */ },
                    enabled = true
                )
            }
        }
        
        // ============================================
        // UNDER CONSTRUCTION BANNER
        // ============================================
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Construction,
                        contentDescription = "Under Construction",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Features Under Development",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "These features are coming soon",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
        
        // ============================================
        // DISABLED/UNDER CONSTRUCTION FEATURES
        // ============================================
        
        // Aesthetic Section (DISABLED)
        item {
            SettingsSection(
                title = "Appearance",
                enabled = false
            ) {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Theme & Style",
                    subtitle = "Colors, dark mode, fonts",
                    onClick = {},
                    enabled = false
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItemWithSwitch(
                    icon = Icons.Default.DarkMode,
                    title = "Dark Mode",
                    subtitle = "Easier on the eyes",
                    checked = false,
                    onCheckedChange = {},
                    enabled = false
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
                    subtitle = "English",
                    value = "English",
                    onClick = {},
                    enabled = false
                )
            }
        }
        
        // Privacy & Security Section (DISABLED)
        item {
            SettingsSection(
                title = "Privacy & Security",
                enabled = false
            ) {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy Settings",
                    subtitle = "Data collection, analytics",
                    onClick = {},
                    enabled = false
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Advanced Security",
                    subtitle = "Biometric auth, encryption settings",
                    onClick = {},
                    enabled = false
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Plugin Security",
                    subtitle = "Permissions & sandboxing",
                    onClick = {},
                    enabled = false
                )
            }
        }
        
        // Notifications Section (DISABLED)
        item {
            SettingsSection(
                title = "Notifications",
                enabled = false
            ) {
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notification Settings",
                    subtitle = "Types, frequency, channels",
                    onClick = {},
                    enabled = false
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItemWithSwitch(
                    icon = Icons.Default.NotificationsActive,
                    title = "Daily Reminders",
                    subtitle = "Get reminded to log data",
                    checked = false,
                    onCheckedChange = {},
                    enabled = false
                )
            }
        }
        
        // Account Section (DISABLED)
        item {
            SettingsSection(
                title = "Account",
                enabled = false
            ) {
                SettingsItem(
                    icon = Icons.Default.AccountCircle,
                    title = "Profile",
                    subtitle = "Name, email, avatar",
                    onClick = {},
                    enabled = false
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.Sync,
                    title = "Sync & Backup",
                    subtitle = "Cloud sync settings",
                    onClick = {},
                    enabled = false
                )
            }
        }
        
        // P2P/Sharing Section (DISABLED)
        item {
            SettingsSection(
                title = "Sharing",
                enabled = false
            ) {
                SettingsItem(
                    icon = Icons.Default.Share,
                    title = "P2P Sharing",
                    subtitle = "Share data with trusted contacts",
                    onClick = {},
                    enabled = false
                )
                
                Divider(modifier = Modifier.padding(horizontal = 16.dp))
                
                SettingsItem(
                    icon = Icons.Default.Group,
                    title = "Trusted Contacts",
                    subtitle = "Manage sharing permissions",
                    onClick = {},
                    enabled = false
                )
            }
        }
        
        // Add some bottom padding
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun SettingsSection(
    title: String,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (enabled) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                }
            )
        ) {
            Column {
                content()
            }
        }
    }
}

// Helper function to format export time
@Composable
private fun formatExportTime(timestamp: Long): String {
    val formatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return formatter.format(Date(timestamp))
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    value: String? = null,
    badge: String? = null,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        onClick = if (enabled) onClick else { {} },
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    }
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            }
            
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
            
            if (badge != null) {
                Badge(
                    containerColor = if (enabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        text = badge,
                        color = if (enabled) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        }
                    )
                }
            }
            
            if (!enabled) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Coming Soon",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SettingsItemWithSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                }
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    }
                )
            }
        }
        
        Switch(
            checked = checked,
            onCheckedChange = if (enabled) onCheckedChange else { {} },
            enabled = enabled
        )
    }
}
