package com.domain.app.social.contracts

import java.time.Instant

data class FeedItem(
    val id: String,
    val authorId: String,
    val authorName: String,
    val contentType: ContentType,
    val content: FeedContent,
    val timestamp: Instant,
    val isRead: Boolean = false,
    val canReshare: Boolean = false,
    val expiresAt: Instant? = null,
    val tags: List<String> = emptyList()
)
