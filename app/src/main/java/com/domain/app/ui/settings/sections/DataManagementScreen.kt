// app/src/main/java/com/domain/app/ui/settings/sections/DataManagementScreen.kt
package com.domain.app.ui.settings.sections

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.app.ui.settings.sections.DataManagementViewModel
import com.domain.app.ui.settings.sections.ExportFormat
import kotlinx.coroutines.launch

val ExportFormat.displayName: String
    get() = when (this) {
        ExportFormat.JSON -> "JSON"
        ExportFormat.CSV -> "CSV"
        ExportFormat.ZIP -> "ZIP Archive"
    }

val ExportFormat.description: String
    get() = when (this) {
        ExportFormat.JSON -> "JavaScript Object Notation - Human readable"
        ExportFormat.CSV -> "Comma Separated Values - Excel compatible"
        ExportFormat.ZIP -> "Compressed archive with all data"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    
    var showExportDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Data Statistics Card
            item {
                DataStatisticsCard(
                    dataPoints = uiState.totalDataPoints,
                    storageUsed = uiState.storageUsedMB,
                    lastBackup = uiState.lastBackupTime
                )
            }
            
            // Export/Import Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = "📤",
                        title = "Export Data",
                        subtitle = "Download backup",
                        onClick = { showExportDialog = true }
                    )
                    
                    ActionCard(
                        modifier = Modifier.weight(1f),
                        icon = "📥",
                        title = "Import Data",
                        subtitle = "Coming soon",
                        onClick = { /* Not implemented */ },
                        enabled = false
                    )
                }
            }
            
            // Backup Settings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        Text(
                            text = "BACKUP SETTINGS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(16.dp)
                        )
                        
			 BackupSettingItem(
			    title = "Auto Backup",
			    subtitle = "Backup data automatically",
			    checked = uiState.autoBackupEnabled,
			    onCheckedChange = { enabled -> viewModel.toggleAutoBackup(enabled) }
			)

                        
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        
                        BackupActionItem(
                            title = "Backup Now",
                            subtitle = "Create manual backup",
                            onClick = { showBackupDialog = true }
                        )
                        
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                        
                        BackupActionItem(
                            title = "Backup Location",
                            subtitle = uiState.backupLocation,
                            onClick = { /* TODO: Choose backup location */ }
                        )
                    }
                }
            }
            
            // Danger Zone
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column {
                        Text(
                            text = "DANGER ZONE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp)
                        )
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showDeleteDialog = true }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Clear All Data",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "This cannot be undone",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                                )
                            }
                            
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Export Dialog
    if (showExportDialog) {
        ExportDataDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format, encrypt ->
                scope.launch {
                    viewModel.exportData(format, encrypt)
                    showExportDialog = false
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeleteDataDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                scope.launch {
                    viewModel.clearAllData()
                    showDeleteDialog = false
                }
            }
        )
    }
    
    // Backup Progress Dialog
    if (showBackupDialog) {
        BackupProgressDialog(
            onDismiss = { showBackupDialog = false },
            onComplete = {
                scope.launch {
                    viewModel.backupNow()
                    showBackupDialog = false
                }
            }
        )
    }
}

@Composable
private fun DataStatisticsCard(
    dataPoints: Int,
    storageUsed: String,
    lastBackup: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = "📊",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Your Data Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatisticItem(
                    label = "Data Points",
                    value = dataPoints.toString()
                )
                StatisticItem(
                    label = "Storage Used",
                    value = storageUsed
                )
                StatisticItem(
                    label = "Last Backup",
                    value = lastBackup
                )
            }
        }
    }
}

@Composable
private fun StatisticItem(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ActionCard(
    modifier: Modifier = Modifier,
    icon: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = if (enabled) onClick else { {} },
        modifier = modifier
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BackupSettingItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,  // Use the parameter, not uiState
            onCheckedChange = onCheckedChange  // Use the parameter, not viewModel
        )
    }
}

@Composable
private fun BackupActionItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ExportDataDialog(
    onDismiss: () -> Unit,
    onExport: (format: ExportFormat, encrypt: Boolean) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.JSON) }
    var encryptExport by remember { mutableStateOf(true) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column {
                Text("Choose export format:")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ExportFormat.values().forEach { format ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(vertical = 8.dp)
                    ) {
                        RadioButton(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(format.displayName)
                            Text(
                                format.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { encryptExport = !encryptExport }
                ) {
                    Checkbox(
                        checked = encryptExport,
                        onCheckedChange = { encryptExport = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Encrypt export with password")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onExport(selectedFormat, encryptExport) }
            ) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DeleteDataDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Clear All Data",
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text(
                    "This action cannot be undone!",
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("All your data including:")
                
                val items = listOf(
                    "• Personal information",
                    "• Plugin data",
                    "• Settings and preferences",
                    "• Backup history"
                )
                
                items.forEach { item ->
                    Text(
                        item,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Will be permanently deleted.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete Everything")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun BackupProgressDialog(
    onDismiss: () -> Unit,
    onComplete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Creating Backup") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Creating encrypted backup...")
            }
        },
        confirmButton = {
            TextButton(onClick = onComplete) {
                Text("OK")
            }
        }
    )
}
