package com.domain.app.social.ui.feed

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.domain.app.social.contracts.FeedItem
import com.domain.app.social.contracts.FeedContent
import java.time.format.DateTimeFormatter
import java.time.ZoneId

@Composable
fun FeedItemCard(feedItem: FeedItem) {
    val dateFormatter = DateTimeFormatter.ofPattern("hh:mm a")
        .withZone(ZoneId.systemDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = feedItem.authorName, style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(4.dp))

            when (val content = feedItem.content) {
                is FeedContent.DataInsight -> {
                    Text("${content.emoji ?: ""} ${content.title}", style = MaterialTheme.typography.titleMedium)
                    Text("${content.value} ${content.unit ?: ""}", style = MaterialTheme.typography.bodyMedium)
                }
                is FeedContent.JournalEntry -> {
                    Text(content.title, style = MaterialTheme.typography.titleMedium)
                    Text(content.excerpt, style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    Text("Unsupported content", style = MaterialTheme.typography.labelSmall)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Shared at: ${dateFormatter.format(feedItem.timestamp)}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
