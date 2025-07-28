package com.domain.app.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.domain.app.core.plugin.QuickOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterQuickAddDialog(
    options: List<QuickOption>,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var selectedOption by remember { mutableStateOf<QuickOption?>(options.firstOrNull()) }
    var customAmount by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Water") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("How much water did you drink?")
                
                // Quick options grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(options) { option ->
                        QuickOptionCard(
                            option = option,
                            isSelected = selectedOption == option,
                            onClick = {
                                selectedOption = option
                                showCustomInput = (option.value as? Number)?.toInt() == -1
                            }
                        )
                    }
                }
                
                // Custom input field
                if (showCustomInput) {
                    OutlinedTextField(
                        value = customAmount,
                        onValueChange = { customAmount = it.filter { char -> char.isDigit() } },
                        label = { Text("Amount (ml)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                // Recent amounts hint
                Text(
                    text = "Tip: Stay hydrated! Aim for 2L daily.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = when {
                        showCustomInput -> customAmount.toDoubleOrNull() ?: 0.0
                        else -> {
                            val value = selectedOption?.value as? Number
                            if (value?.toInt() == -1) 0.0 else value?.toDouble() ?: 0.0
                        }
                    }
                    if (amount > 0) {
                        onConfirm(amount)
                    }
                },
                enabled = !showCustomInput || customAmount.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickOptionCard(
    option: QuickOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (option.icon != null) {
                Text(
                    text = option.icon,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
            Text(
                text = option.label,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}
