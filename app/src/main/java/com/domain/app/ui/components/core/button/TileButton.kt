package com.domain.app.ui.components.core.button

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

/**
 * A reusable tile button component for plugin interfaces
 * Used for selecting options in a grid layout
 */
@Composable
fun TileButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedColor: Color = MaterialTheme.colorScheme.primaryContainer,
    unselectedColor: Color = MaterialTheme.colorScheme.surface,
    selectedTextColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val backgroundColor = if (selected) selectedColor else unselectedColor
    val textColor = if (selected) selectedTextColor else unselectedTextColor
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 4.dp else 1.dp
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * A grid of tile buttons with single selection
 */
@Composable
fun TileButtonGrid(
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 3,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.chunked(columns).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEach { option ->
                    TileButton(
                        text = option,
                        selected = option == selectedOption,
                        onClick = { onOptionSelected(option) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled
                    )
                }
                // Add empty space for incomplete rows
                repeat(columns - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
