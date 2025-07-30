package com.domain.app.ui.social

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * P2P Feed Screen - Shows messages and data from peers
 * 
 * File location: app/src/main/java/com/domain/app/ui/social/P2PFeedScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P2PFeedScreen(
    onNavigateToContacts: () -> Unit,
    onNavigateToAddContact: () -> Unit,
    onNavigateToContactDetail: (String) -> Unit,
    viewModel: P2PFeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("P2P Feed") },
                actions = {
                    IconButton(onClick = { viewModel.refreshFeed() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onNavigateToContacts) {
                        Icon(Icons.Default.People, contentDescription = "Contacts")
                    }
                    IconButton(onClick = onNavigateToAddContact) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add Contact")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { /* TODO: Post message */ },
                text = { Text("Post") },
                icon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
        }
    ) { paddingValues ->
        when {
            uiState.isLoading && uiState.feedItems.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            uiState.feedItems.isEmpty() -> {
                EmptyFeedState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    onAddContact = onNavigateToAddContact
                )
            }
            
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.feedItems) { item ->
                        FeedItemCard(
                            item = item,
                            onContactClick = { onNavigateToContactDetail(item.authorId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyFeedState(
    modifier: Modifier = Modifier,
    onAddContact: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.RssFeed,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "No feed items yet",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Add contacts to see their shared data",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        FilledTonalButton(onClick = onAddContact) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Contact")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedItemCard(
    item: com.domain.app.network.FeedItem,
    onContactClick: () -> Unit
) {
    Card(
        onClick = { /* TODO: View details */ },
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onContactClick) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = item.authorName,
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                
                Text(
                    text = formatTimestamp(item.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = item.preview ?: "Behavioral data",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row {
                AssistChip(
                    onClick = { },
                    label = { Text(item.contentType) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.DataUsage,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
    return instant.atZone(ZoneId.systemDefault()).format(formatter)
}
