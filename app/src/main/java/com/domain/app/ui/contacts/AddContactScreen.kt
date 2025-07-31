package com.domain.app.ui.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

/**
 * Screen for adding new contacts
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactScreen(
    navController: NavController,
    viewModel: AddContactViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Contact") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Generate Link Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Share Your Contact Link",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    if (uiState.contactLink != null) {
                        OutlinedTextField(
                            value = uiState.contactLink,
                            onValueChange = { },
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                    
                    Button(
                        onClick = { viewModel.generateContactLink() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isGenerating
                    ) {
                        if (uiState.isGenerating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text("Generate Link")
                        }
                    }
                }
            }
            
            // Add Contact Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Add Contact from Link",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    var contactLink by remember { mutableStateOf("") }
                    
                    OutlinedTextField(
                        value = contactLink,
                        onValueChange = { contactLink = it },
                        label = { Text("Contact Link") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    Button(
                        onClick = { 
                            viewModel.addContactFromLink(contactLink)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isAdding && contactLink.isNotBlank()
                    ) {
                        if (uiState.isAdding) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text("Add Contact")
                        }
                    }
                }
            }
            
            // Error handling
            uiState.error?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Success handling
            if (uiState.success) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            }
        }
    }
}
