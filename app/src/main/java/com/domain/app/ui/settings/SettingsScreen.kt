// app/src/main/java/com/domain/app/ui/settings/SettingsScreen.kt
package com.domain.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.domain.app.ui.theme.AppIcons
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    paddingValues: PaddingValues = PaddingValues(),
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}, // Back navigation parameter
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = AppIcons.Navigation.back,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // ============================================
            // WORKING FEATURES SECTION
            // ============================================
            
            // Data Management Section (WORKING - Only Export)
            item {
                SettingsSection(title = "Data Management") {
                    SettingsItem(
                        icon = Icons.Default.Download,
                        title = "Export Data",
                        subtitle = "Export all data to CSV",
                        onClick = onNavigateToDataManagement,
                        enabled = true
                    )
                }
            }
            
            // About Section (WORKING - Simplified to single button)
            item {
                SettingsSection(title = "Information") {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "About",
                        subtitle = "Version ${BuildConfig.VERSION_NAME} • Licenses • Credits",
                        onClick = onNavigateToAbout,
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
            // DISABLED FEATURES (COMING SOON)
            // ============================================
            
            // Data & Backup Section (DISABLED) - Moved from working section
            item {
                SettingsSection(
                    title = "Backup & Sync",
                    enabled = false
                ) {
                    SettingsItemWithSwitch(
                        icon = Icons.Default.Backup,
                        title = "Auto Backup",
                        subtitle = "Automatically backup your data",
                        checked = false,
                        onCheckedChange = {},
                        enabled = false
                    )
                    
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsItem(
                        icon = Icons.Default.Schedule,
                        title = "Backup Frequency",
                        subtitle = "Daily, Weekly, Monthly",
                        onClick = {},
                        enabled = false
                    )
                    
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsItem(
                        icon = Icons.Default.CloudUpload,
                        title = "Cloud Sync",
                        subtitle = "Sync data across devices",
                        onClick = {},
                        enabled = false
                    )
                }
            }
            
            // Plugins Section (DISABLED) - Moved from working section
            item {
                SettingsSection(
                    title = "Plugins",
                    enabled = false
                ) {
                    SettingsItem(
                        icon = Icons.Default.Extension,
                        title = "Manage Plugins",
                        subtitle = "${uiState.enabledPluginCount} plugins enabled",
                        onClick = {},
                        enabled = false
                    )
                    
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsItem(
                        icon = Icons.Default.AdminPanelSettings,
                        title = "Plugin Permissions",
                        subtitle = "Control plugin access",
                        onClick = {},
                        enabled = false
                    )
                }
            }
            
            // Aesthetic Section (DISABLED)
            item {
                SettingsSection(
                    title = "Aesthetic",
                    enabled = false
                ) {
                    SettingsItem(
                        icon = Icons.Default.Palette,
                        title = "Theme",
                        subtitle = "Dark Mode",
                        value = uiState.themeMode,
                        onClick = {},
                        enabled = false
                    )
                    
                    Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    
                    SettingsItem(
                        icon = Icons.Default.FormatSize,
                        title = "Font & Display",
                        subtitle = "Text size, font style",
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
}

// Keep all the existing @Composable functions as they are...
@Composable
fun SettingsSection(
    title: String,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surface
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            content()
        }
    }
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
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}
