package com.domain.app.social.data

import com.domain.app.social.contracts.*
import kotlinx.coroutines.delay
import java.time.Instant
import javax.inject.Inject

class MockSocialFeedRepository @Inject constructor() : SocialFeedRepository {

    override suspend fun getFeedItems(): List<FeedItem> {
        delay(1200) // gentle loading

        return listOf(
            FeedItem(
                id = "1",
                authorId = "phoenix_001",
                authorName = "Star Phoenix",
                content = FeedContent.Text("I just finished a fire ritual under the full moon. Feeling alive!"),
                contentType = ContentType.TEXT,
                timestamp = Instant.now(),
                isRead = false,
                canReshare = true,
                expiresAt = Instant.now().plusSeconds(86400), // +1 day
                tags = listOf("Fire", "Ritual")
            ),
            FeedItem(
                id = "2",
                authorId = "ocean_002",
                authorName = "Ocean Whisperer",
                content = FeedContent.Text("Tears flowed today. Water has taught me to feel without shame."),
                contentType = ContentType.TEXT,
                timestamp = Instant.now(),
                isRead = false,
                canReshare = false,
                expiresAt = Instant.now().plusSeconds(172800), // +2 days
                tags = listOf("Water", "Emotions")
            )
        )
    }
}
