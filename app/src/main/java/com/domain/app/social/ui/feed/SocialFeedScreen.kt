package com.domain.app.social.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.domain.app.social.contracts.FeedItem
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.delay

@Composable
fun SocialFeedScreen(
    viewModel: SocialFeedViewModel = hiltViewModel()
) {
    val feedItems by viewModel.feedItems.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Social Feed", style = MaterialTheme.typography.titleLarge)
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshFeed() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SwipeRefresh(
                state = rememberSwipeRefreshState(isRefreshing),
                onRefresh = { viewModel.refreshFeed() },
                indicator = { state, refreshTriggerDistance ->
                    SwipeRefreshIndicator(
                        state = state,
                        refreshTriggerDistance = refreshTriggerDistance,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(feedItems) { item: FeedItem ->
                        FeedItemCard(
                            feedItem = item,
                            modifier = Modifier.fillMaxWidth(),
                            isUnread = !item.isRead,
                            isExpiringSoon = item.expiresAt?.let {
                                val now = java.time.Instant.now()
                                val remaining = java.time.Duration.between(now, it).toHours()
                                remaining in 0..12
                            } ?: false,
                            onMarkRead = { viewModel.markAsRead(item.id) }
                        )
                    }
                }
            }
        }
    }
}
