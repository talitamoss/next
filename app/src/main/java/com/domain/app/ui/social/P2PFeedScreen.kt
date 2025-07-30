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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * P2P Feed Screen - Pull-based social feed
 * 
 * File location: app/src/main/java/com/domain/app/ui/social/P2PFeedScreen.kt
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun P2PFeedScreen(
    onNavigateToContacts: () -> Unit,
    viewModel: P2PFeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val feedItems by viewModel.feedItems.collectAsStateWithLifecycle()
    val connectedPeers by viewModel.connectedPeers.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("P2P Feed")
                        Text(
                            text = "${connectedPeers.size} peers connected",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToContacts) {
                        Icon(Icons.Default.People, "Manage Contacts")
                    }
                    IconButton(onClick = { viewModel.pullFeed() }) {
                        Icon(Icons.Default.Refresh, "Pull Feed")
                    }
                }
            )
        },
        bottomBar = {
            // Message input bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.messageInput,
                        onValueChange = viewModel::updateMessageInput,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Share something...") },
                        singleLine = true
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    IconButton(
                        onClick = { viewModel.postMessage(uiState.messageInput) },
                        enabled = uiState.messageInput.isNotBlank() && !uiState.isPosting
                    ) {
                        if (uiState.isPosting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, "Post")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        SwipeRefresh(
            state = rememberSwipeRefreshState(uiState.isRefreshing),
            onRefresh = { viewModel.pullFeed() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (feedItems.isEmpty() && !uiState.isRefreshing) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.WifiTetheringOff,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No messages yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Pull to refresh or post something!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Feed list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(feedItems) { item ->
                        FeedItemCard(item)
                    }
                }
            }
        }
    }
    
    // Error handling
    uiState.error?.let { error ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = viewModel::clearError) {
                    Text("Dismiss")
                }
            }
        ) {
            Text(error)
        }
    }
}

@Composable
fun FeedItemCard(
    item: FeedItem
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (item.isLocal) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar placeholder
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = item.fromPeer.nickname.first().toString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            text = item.fromPeer.nickname,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatTimestamp(item.message.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Status indicators
                Row {
                    if (item.isLocal) {
                        Icon(
                            Icons.Default.CloudDone,
                            contentDescription = "Available for peers",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    if (item.message.ttl < 7) {
                        Text(
                            text = "TTL: ${item.message.ttl}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Message content
            Text(
                text = item.message.content,
                style = MaterialTheme.typography.bodyMedium
            )
            
            // Future: Add behavioral data visualization here
            when (item.message.type) {
                MessageType.BEHAVIORAL -> {
                    // Show data visualization
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            text = "📊 Behavioral data will appear here",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                else -> { /* Regular message */ }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long): String {
    val instant = Instant.ofEpochMilli(timestamp)
    val now = Instant.now()
    
    return when {
        instant.isAfter(now.minusSeconds(60)) -> "Just now"
        instant.isAfter(now.minusSeconds(3600)) -> {
            val minutes = (now.epochSecond - instant.epochSecond) / 60
            "$minutes min ago"
        }
        instant.isAfter(now.minusSeconds(86400)) -> {
            val hours = (now.epochSecond - instant.epochSecond) / 3600
            "$hours hours ago"
        }
        else -> {
            DateTimeFormatter.ofPattern("MMM d, HH:mm")
                .withZone(ZoneId.systemDefault())
                .format(instant)
        }
    }
}
