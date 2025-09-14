// app/src/main/java/com/domain/app/ui/reflect/ReflectScreen.kt
package com.domain.app.ui.reflect

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.domain.app.ui.theme.AppIcons
import com.domain.app.ui.reflect.components.PluginFilterDropdown
import com.domain.app.ui.reflect.components.TimeFrameSelector
import com.domain.app.ui.reflect.components.WeekView
import com.domain.app.ui.reflect.components.MonthView
import com.domain.app.ui.reflect.components.ExpandableEntryCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReflectScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ReflectViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showDeleteDialog by remember { mutableStateOf<DataEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Reflect",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = uiState.currentMonthYear,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(
                            imageVector = AppIcons.Navigation.back,
                            contentDescription = "Previous month"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(
                            imageVector = AppIcons.Navigation.forward,
                            contentDescription = "Next month"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = AppIcons.Navigation.settings,
                            contentDescription = "Settings"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Plugin Filter Dropdown (replacing "Most Active" stats)
            PluginFilterDropdown(
                availablePlugins = uiState.availablePlugins,
                selectedPluginIds = uiState.selectedPluginIds,
                showAllPlugins = uiState.showAllPlugins,
                onTogglePlugin = { pluginId ->
                    viewModel.togglePluginFilter(pluginId)
                },
                onSelectAll = {
                    viewModel.selectAllPlugins()
                },
                onClearAll = {
                    viewModel.clearAllPlugins()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Divider()
            
            // Calendar Grid (respects filter)
            CalendarView(
                yearMonth = uiState.currentYearMonth,
                selectedDate = uiState.selectedDate,
                dayActivityMap = if (uiState.showAllPlugins || uiState.selectedPluginIds.isEmpty()) {
                    uiState.dayActivityMap
                } else {
                    uiState.filteredDayActivityMap
                },
                onDateSelected = { date ->
                    viewModel.selectDate(date)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            HorizontalDivider()
            
            // Time Frame Selector
            TimeFrameSelector(
                selectedTimeFrame = uiState.selectedTimeFrame,
                onTimeFrameSelected = { timeFrame ->
                    viewModel.selectTimeFrame(timeFrame)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            // Content based on selected time frame
            AnimatedContent(
                targetState = uiState.selectedTimeFrame,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "TimeFrameContent"
            ) { timeFrame ->
                when (timeFrame) {
                    TimeFrame.DAY -> {
                        DayView(
                            selectedDate = uiState.selectedDate,
                            dayData = uiState.selectedDayData,
                            expandedEntryIds = uiState.expandedEntryIds,
                            onToggleExpand = { entryId ->
                                viewModel.toggleEntryExpanded(entryId)
                            },
                            onEdit = { entry ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Edit feature coming soon")
                                }
                            },
                            onDelete = { entry ->
                                showDeleteDialog = entry
                            },
                            onShare = { entry ->
                                scope.launch {
                                    snackbarHostState.showSnackbar("Shared: ${entry.displayValue}")
                                }
                            }
                        )
                    }
                    TimeFrame.WEEK -> {
                        WeekView(
                            weekData = uiState.weekData,
                            selectedPluginIds = uiState.selectedPluginIds,
                            availablePlugins = uiState.availablePlugins.associate { 
                                it.id to it.metadata.name 
                            }
                        )
                    }
                    TimeFrame.MONTH -> {
                        MonthView(
                            monthData = uiState.monthData,
                            selectedPluginIds = uiState.selectedPluginIds,
                            availablePlugins = uiState.availablePlugins.associate { 
                                it.id to it.metadata.name 
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { entry ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Entry") },
            text = { 
                Text("Are you sure you want to delete this ${entry.pluginName} entry?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteEntry(entry.id)
                        showDeleteDialog = null
                        scope.launch {
                            snackbarHostState.showSnackbar("Entry deleted")
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Calendar View Component
@Composable
private fun CalendarView(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    dayActivityMap: Map<LocalDate, DayActivity>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Day headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Calendar grid
        val firstDayOfMonth = yearMonth.atDay(1)
        val lastDayOfMonth = yearMonth.atEndOfMonth()
        val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value
        val daysInMonth = yearMonth.lengthOfMonth()
        
        val totalCells = ((daysInMonth + firstDayOfWeek - 2) / 7 + 1) * 7
        
        Column {
            for (week in 0 until (totalCells / 7)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (dayOfWeek in 0..6) {
                        val dayIndex = week * 7 + dayOfWeek
                        val dayOfMonth = dayIndex - firstDayOfWeek + 2
                        
                        if (dayOfMonth in 1..daysInMonth) {
                            val date = yearMonth.atDay(dayOfMonth)
                            val activity = dayActivityMap[date]
                            val isSelected = date == selectedDate
                            val isToday = date == LocalDate.now()
                            
                            DayCell(
                                date = date,
                                activity = activity,
                                isSelected = isSelected,
                                isToday = isToday,
                                onClick = { onDateSelected(date) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    activity: DayActivity?,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .then(
                if (isSelected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                } else if (isToday) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                } else {
                    Modifier
                }
            )
            .background(
                color = when (activity?.intensity) {
                    ActivityIntensity.VERY_HIGH -> MaterialTheme.colorScheme.primary
                    ActivityIntensity.HIGH -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    ActivityIntensity.MEDIUM -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    ActivityIntensity.LOW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    else -> Color.Transparent
                },
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            color = when (activity?.intensity) {
                ActivityIntensity.VERY_HIGH, ActivityIntensity.HIGH -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}

// Day View Component (existing day details with expandable cards)
@Composable
private fun DayView(
    selectedDate: LocalDate?,
    dayData: DayData,
    expandedEntryIds: Set<String>,
    onToggleExpand: (String) -> Unit,
    onEdit: (DataEntry) -> Unit,
    onDelete: (DataEntry) -> Unit,
    onShare: (DataEntry) -> Unit
) {
    if (selectedDate != null) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Day summary header
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("EEEE")),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = selectedDate.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = dayData.totalCount.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (dayData.totalCount == 1) "entry" else "entries",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            
            // Data entries
            if (dayData.entries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.Data.calendar,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "No data recorded",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(dayData.entries, key = { it.id }) { entry ->
                    ExpandableEntryCard(
                        entry = entry,
                        isExpanded = entry.id in expandedEntryIds,
                        onToggleExpand = { onToggleExpand(entry.id) },
                        onEdit = { onEdit(entry) },
                        onDelete = { onDelete(entry) },
                        onShare = { onShare(entry) }
                    )
                }
            }
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Select a day to view details",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
