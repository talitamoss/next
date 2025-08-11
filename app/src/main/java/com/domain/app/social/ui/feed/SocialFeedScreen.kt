// com.domain.app.social.ui.feed.SocialFeedScreen.kt

package com.domain.app.social.ui.feed

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.domain.app.social.ui.feed.FeedItemCard

@Composable
fun SocialFeedScreen(viewModel: SocialFeedViewModel = hiltViewModel()) {
    val feedItems by viewModel.feedItems.collectAsState()

    LazyColumn {
        items(feedItems) { item ->
            FeedItemCard(item)
        }
    }
}
