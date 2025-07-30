package com.domain.app.social.contracts

import java.time.Instant
import java.time.temporal.ChronoUnit

object MockData {
    val mockFeedItems = listOf(
        FeedItem(
            id = "1",
            authorId = "1",
            authorName = "Cashka",
            contentType = ContentType.DATA_INSIGHT,
            content = FeedContent.DataInsight(
                title = "Hydration Level",
                value = "2.4",
                unit = "L",
                emoji = "💧"
            ),
            timestamp = Instant.now().minus(3, ChronoUnit.HOURS),
            isRead = false,
            canReshare = false,
            expiresAt = Instant.now().plus(2, ChronoUnit.DAYS)
        ),
        FeedItem(
            id = "2",
            authorId = "2",
            authorName = "Jordan",
            contentType = ContentType.JOURNAL_ENTRY,
            content = FeedContent.JournalEntry(
                title = "A Calm Morning",
                excerpt = "I woke with the sun and listened to the wind...",
                wordCount = 83
            ),
            timestamp = Instant.now().minus(5, ChronoUnit.HOURS)
        )
    )
}
