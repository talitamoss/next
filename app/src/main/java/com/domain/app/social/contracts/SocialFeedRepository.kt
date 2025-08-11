package com.domain.app.social.contracts

import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant

interface SocialFeedRepository {
    suspend fun getFeedItems(): List<FeedItem>
}

@Singleton
class SocialFeedRepositoryImpl @Inject constructor() : SocialFeedRepository {
    override suspend fun getFeedItems(): List<FeedItem> {
        delay(1500) // simulate network delay

        return listOf(
            FeedItem(
                id = "1",
                authorId = "phoenix_001",
                authorName = "Star Phoenix",
                contentType = ContentType.TEXT,
                content = FeedContent.Text(
                    value = "I just finished a fire ritual..."
                ),
                tags = listOf("Fire", "Ritual"),
                timestamp = Instant.now()
            ),
            FeedItem(
                id = "2",
                authorId = "ocean_002",
                authorName = "Ocean Whisperer",
                content = FeedContent.Text(
                    value = "Tears flowed today. Water has taught me to feel without shame."
                ),
                contentType = ContentType.TEXT,
                tags = listOf("Water", "Emotions"),
                timestamp = Instant.now()
            )
        )
    }
}
