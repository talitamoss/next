package com.domain.app.ui.reflect

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.domain.app.ui.theme.AppIcons
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
    
    Scaffold(
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
                            style = MaterialTheme.typography.bodySmall,
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
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Quick Stats Bar
            QuickStatsBar(
                streak = uiState.currentStreak,
                monthTotal = uiState.monthlyTotal,
                mostActiveDay = uiState.mostActiveDay
            )
            
            Divider()
            
            // Calendar Grid
            CalendarView(
                yearMonth = uiState.currentYearMonth,
                selectedDate = uiState.selectedDate,
                dayActivityMap = uiState.dayActivityMap,
                onDateSelected = { date ->
                    viewModel.selectDate(date)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            Divider()
            
            // Selected Day Details
            if (uiState.selectedDate != null) {
                SelectedDayDetails(
                    date = uiState.selectedDate,
                    dayData = uiState.selectedDayData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                // Empty state when no date selected
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
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
    }
}

@Composable
private fun QuickStatsBar(
    streak: Int,
    monthTotal: Int,
    mostActiveDay: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            label = "Streak",
            value = "$streak days",
            modifier = Modifier.weight(1f)
        )
        StatItem(
            label = "This Month",
            value = "$monthTotal entries",
            modifier = Modifier.weight(1f)
        )
        StatItem(
            label = "Most Active",
            value = mostActiveDay,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CalendarView(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    dayActivityMap: Map<LocalDate, DayActivity>,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Days of week header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach { day ->
                Text(
                    text = day,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
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
        
        // Create list of dates including padding for alignment
        val calendarDays = mutableListOf<LocalDate?>()
        
        // Add empty padding for days before month starts (Monday = 1)
        repeat(firstDayOfWeek - 1) {
            calendarDays.add(null)
        }
        
        // Add all days of the month
        for (day in 1..daysInMonth) {
            calendarDays.add(yearMonth.atDay(day))
        }
        
        // Fill remaining slots to complete the grid
        while (calendarDays.size % 7 != 0) {
            calendarDays.add(null)
        }
        
        // Display in rows of 7
        calendarDays.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                week.forEach { date ->
                    if (date != null) {
                        DayCell(
                            date = date,
                            isSelected = date == selectedDate,
                            isToday = date == LocalDate.now(),
                            activity = dayActivityMap[date],
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

@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    activity: DayActivity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primaryContainer
                    isToday -> MaterialTheme.colorScheme.secondaryContainer
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick)
            .then(
                if (isToday && !isSelected) {
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                    isToday -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            
            // Activity indicator dots
            if (activity != null && activity.entryCount > 0) {
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    val dotCount = minOf(activity.entryCount, 5)
                    repeat(dotCount) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(
                                    when (activity.intensity) {
                                        ActivityIntensity.LOW -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                        ActivityIntensity.MEDIUM -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                        ActivityIntensity.HIGH -> MaterialTheme.colorScheme.primary
                                    }
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayDetails(
    date: LocalDate,
    dayData: DayData,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp)
    ) {
        // Date header
        Text(
            text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        if (dayData.entries.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No data recorded for this day",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dayData.entries) { entry ->
                    DataEntryCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun DataEntryCard(entry: DataEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.pluginName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = entry.displayValue,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (entry.note != null) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = entry.time,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
