// app/src/main/java/com/domain/app/ui/settings/sections/StubScreens.kt
package com.domain.app.ui.settings.sections

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

// Profile Settings Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        ComingSoonContent(
            modifier = Modifier.padding(paddingValues),
            title = "Profile Settings",
            icon = "👤",
            description = "Manage your personal information, avatar, and account details"
        )
    }
}

// Aesthetic Settings Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AestheticSettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aesthetic") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        ComingSoonContent(
            modifier = Modifier.padding(paddingValues),
            title = "Aesthetic Settings",
            icon = "🎨",
            description = "Customize themes, colors, fonts, and visual preferences"
        )
    }
}

// Privacy Settings Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        ComingSoonContent(
            modifier = Modifier.padding(paddingValues),
            title = "Privacy Settings",
            icon = "🔒",
            description = "Control data collection, analytics, and privacy preferences"
        )
    }
}

// Plugin Security Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginSecurityScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugin Security") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        ComingSoonContent(
            modifier = Modifier.padding(paddingValues),
            title = "Plugin Security",
            icon = "🔐",
            description = "Manage plugin permissions and security settings"
        )
    }
}

// Notification Settings Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingsScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        ComingSoonContent(
            modifier = Modifier.padding(paddingValues),
            title = "Notification Settings",
            icon = "🔔",
            description = "Configure notification types, frequency, and channels"
        )
    }
}

// About Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        ComingSoonContent(
            modifier = Modifier.padding(paddingValues),
            title = "About",
            icon = "ℹ️",
            description = "App information, version details, and credits"
        )
    }
}

// Plugin Detail Screen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginDetailScreen(
    pluginId: String,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Plugin Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        ComingSoonContent(
            modifier = Modifier.padding(paddingValues),
            title = "Plugin: $pluginId",
            icon = "🧩",
            description = "View and configure plugin settings"
        )
    }
}

// Shared Coming Soon Component
@Composable
private fun ComingSoonContent(
    modifier: Modifier = Modifier,
    title: String,
    icon: String,
    description: String
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.displayLarge
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "🚀 Coming Soon",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}
