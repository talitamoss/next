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
import androidx.compose.ui.unit.sp
import com.domain.app.core.plugin.QuickOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodQuickAddDialog(
    options: List<QuickOption>,
    onDismiss: () -> Unit,
    onConfirm: (Int, String?) -> Unit
) {
    var selectedMood by remember { mutableStateOf<QuickOption?>(null) }
    var note by remember { mutableStateOf("") }
    var showNoteField by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How are you feeling?") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Mood selection grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(80.dp)
                ) {
                    items(options) { option ->
                        MoodOptionCard(
                            option = option,
                            isSelected = selectedMood == option,
                            onClick = {
                                selectedMood = option
                                showNoteField = true
                            }
                        )
                    }
                }
                
                // Optional note field
                if (showNoteField) {
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Add a note (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    selectedMood?.let { mood ->
                        val moodValue = (mood.value as? Number)?.toInt() ?: 3
                        onConfirm(moodValue, note.takeIf { it.isNotBlank() })
                    }
                },
                enabled = selectedMood != null
            ) {
                Text("Save")
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
fun MoodOptionCard(
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
            .aspectRatio(1f)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = option.icon ?: "",
                fontSize = 28.sp
            )
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}
