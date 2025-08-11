// File: com/domain/app/social/ui/feed/FeedItemCard.kt

package com.domain.app.social.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.domain.app.social.contracts.FeedContent
import com.domain.app.social.contracts.FeedItem
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FeedItemCard(
    feedItem: FeedItem,
    onClick: (() -> Unit)? = null
) {
    val formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy • h:mm a")
        .withZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = feedItem.authorName,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = formatter.format(feedItem.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            when (val content = feedItem.content) {
                is FeedContent.Text -> {
                    Text(content.value, style = MaterialTheme.typography.bodyLarge)
                }

                is FeedContent.DataInsight -> {
                    Text(content.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${content.value} ${content.unit.orEmpty()} ${content.emoji.orEmpty()}")
                }

                is FeedContent.JournalEntry -> {
                    Text(content.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(content.excerpt, style = MaterialTheme.typography.bodyMedium)
                }

                is FeedContent.MediaContent -> {
                    Text(content.caption, style = MaterialTheme.typography.bodyMedium)
                }

                is FeedContent.AggregatedData -> {
                    Text(content.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(content.summary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
