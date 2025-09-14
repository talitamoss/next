// app/src/main/java/com/domain/app/ui/settings/sections/DataManagementScreen.kt
package com.domain.app.ui.settings.sections

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
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
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import com.domain.app.core.plugin.ExportFormat
import com.domain.app.ui.data.ExportOptions
import com.domain.app.ui.data.TimeFrame

val ExportFormat.displayName: String
    get() = when (this) {
        ExportFormat.JSON -> "JSON"
        ExportFormat.CSV -> "CSV"
        ExportFormat.XML -> "XML"
        ExportFormat.CUSTOM -> "Custom"
    }

val ExportFormat.description: String
    get() = when (this) {
        ExportFormat.JSON -> "JavaScript Object Notation - Human readable"
        ExportFormat.CSV -> "Comma Separated Values - Excel compatible"
        ExportFormat.XML -> "Structured data format"
        ExportFormat.CUSTOM -> "Plugin-specific format"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    onNavigateBack: () -> Unit,
    viewModel: DataManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    Button(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export")
                    }
                    
                    OutlinedButton(
                        onClick = { /* TODO: Import */ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import")
                    }
                }
            }
            
            // Backup Settings
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Automatic Backup",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        
                        BackupSettingItem(
                            title = "Enable Auto Backup",
                            subtitle = "Automatically backup your data",
                            checked = uiState.autoBackupEnabled,
                            onCheckedChange = { viewModel.toggleAutoBackup(it) }
                        )
                        
                        AnimatedVisibility(visible = uiState.autoBackupEnabled) {
                            Column {
                                BackupActionItem(
                                    title = "Backup Frequency",
                                    subtitle = uiState.backupFrequency.capitalize(),
                                    onClick = { /* TODO: Show frequency picker */ }
                                )
                                BackupActionItem(
                                    title = "Backup Time",
                                    subtitle = uiState.backupTime,
                                    onClick = { /* TODO: Show time picker */ }
                                )
                                BackupSettingItem(
                                    title = "WiFi Only",
                                    subtitle = "Only backup when connected to WiFi",
                                    checked = uiState.backupWifiOnly,
                                    onCheckedChange = { viewModel.setBackupWifiOnly(it) }
                                )
                            }
                        }
                    }
                }
            }
            
            // Manual Backup Actions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Manual Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showBackupDialog = true },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Backup Now")
                            }
                            
                            OutlinedButton(
                                onClick = { /* TODO: Restore */ },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restore,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Restore")
                            }
                        }
                    }
                }
            }
            
            // Danger Zone
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⚠️ Danger Zone",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "These actions cannot be undone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        OutlinedButton(
                            onClick = { showDeleteDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteForever,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear All Data")
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Enhanced Export Dialog with all our new features
    if (showExportDialog) {
        EnhancedExportDialog(
            onDismiss = { showExportDialog = false },
            onExport = { options ->
                scope.launch {
                    viewModel.exportDataWithOptions(options)
                    showExportDialog = false
                }
            },
            availablePlugins = uiState.availablePlugins
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
    
    // Show messages
    uiState.message?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            viewModel.clearMessage()
        }
    }
    
    // Show errors
    uiState.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }
}  // DataManagementScreen ends here

// All helper functions come AFTER DataManagementScreen, not inside it

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
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(
                    label = "Data Points",
                    value = dataPoints.toString()
                )
                StatItem(
                    label = "Storage Used",
                    value = storageUsed
                )
                StatItem(
                    label = "Last Backup",
                    value = lastBackup
                )
            }
        }
    }
}

@Composable
private fun StatItem(
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
            checked = checked,
            onCheckedChange = onCheckedChange
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

// THE NEW ENHANCED EXPORT DIALOG WITH ALL OUR FEATURES
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnhancedExportDialog(
    onDismiss: () -> Unit,
    onExport: (ExportOptions) -> Unit,
    availablePlugins: List<com.domain.app.core.plugin.Plugin>
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
    var selectedTimeFrame by remember { mutableStateOf(TimeFrame.ALL) }
    var selectedPlugins by remember { mutableStateOf(availablePlugins.map { it.id }.toSet()) }
    var customStartDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var customEndDate by remember { mutableStateOf<LocalDateTime?>(null) }
    var encryptExport by remember { mutableStateOf(false) }
    var showFormatDropdown by remember { mutableStateOf(false) }
    var showTimeFrameDropdown by remember { mutableStateOf(false) }
    var showPluginSelector by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Data") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Format Dropdown
                Column {
                    Text(
                        "Export Format",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFormatDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(selectedFormat.displayName)
                                    Text(
                                        text = selectedFormat.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showFormatDropdown,
                            onDismissRequest = { showFormatDropdown = false }
                        ) {
                            ExportFormat.values().forEach { format ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(format.displayName)
                                            Text(
                                                format.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedFormat = format
                                        showFormatDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Time Frame Selection
                Column {
                    Text(
                        "Time Period",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTimeFrameDropdown = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedTimeFrame.displayName)
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = null
                                )
                            }
                        }
                        
                        DropdownMenu(
                            expanded = showTimeFrameDropdown,
                            onDismissRequest = { showTimeFrameDropdown = false }
                        ) {
                            TimeFrame.values().forEach { timeFrame ->
                                DropdownMenuItem(
                                    text = { Text(timeFrame.displayName) },
                                    onClick = {
                                        selectedTimeFrame = timeFrame
                                        showTimeFrameDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
                
                // Plugin Selection
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Plugins to Export",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                selectedPlugins = if (selectedPlugins.size == availablePlugins.size) {
                                    emptySet()
                                } else {
                                    availablePlugins.map { it.id }.toSet()
                                }
                            }
                        ) {
                            Text(
                                if (selectedPlugins.size == availablePlugins.size) "Deselect All" else "Select All"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Column {
                        availablePlugins.forEach { plugin ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPlugins = if (plugin.id in selectedPlugins) {
                                            selectedPlugins - plugin.id
                                        } else {
                                            selectedPlugins + plugin.id
                                        }
                                    }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = plugin.id in selectedPlugins,
                                    onCheckedChange = { checked ->
                                        selectedPlugins = if (checked) {
                                            selectedPlugins + plugin.id
                                        } else {
                                            selectedPlugins - plugin.id
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(plugin.metadata.name)
                            }
                        }
                    }
                }
                
                // Encryption Option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = encryptExport,
                        onCheckedChange = { encryptExport = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Encrypt Export")
                        Text(
                            "Protect your data with encryption",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
		 onClick = {
                    val options = ExportOptions(
                        format = selectedFormat,
                        timeFrame = selectedTimeFrame,
                        selectedPlugins = selectedPlugins,
                        customDateRange = if (selectedTimeFrame == TimeFrame.CUSTOM && customStartDate != null && customEndDate != null) {
                            customStartDate.atZone(ZoneId.systemDefault()).toInstant() to
                            customEndDate.atZone(ZoneId.systemDefault()).toInstant()
                        } else null,
                        encrypt = encryptExport
                    )
                    onExport(options)
                },
                enabled = selectedPlugins.isNotEmpty()
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

val TimeFrame.displayName: String
    get() = when (this) {
        TimeFrame.DAY -> "Last 24 Hours"
        TimeFrame.WEEK -> "Last Week"
        TimeFrame.MONTH -> "Last Month"
        TimeFrame.THREE_MONTHS -> "Last 3 Months"
        TimeFrame.SIX_MONTHS -> "Last 6 Months"
        TimeFrame.YEAR -> "Last Year"
        TimeFrame.ALL -> "All Time"
        TimeFrame.CUSTOM -> "Custom Range"
    }

@Composable
private fun DeleteDataDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete All Data?") },
        text = {
            Column {
                Text(
                    "This will permanently delete all your data.",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("All your data will be permanently deleted.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete All")
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
        title = { Text("Backup in Progress") },
        text = {
            Column {
                Text("Creating backup of your data...")
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = onComplete) {
                Text("Complete")
            }
        }
    )
}
