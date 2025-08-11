package com.domain.app.social.contracts

sealed class FeedContent {
    data class DataInsight(
        val title: String,
        val value: String,
        val unit: String? = null,
        val emoji: String? = null
    ) : FeedContent()

    data class JournalEntry(
        val title: String,
        val excerpt: String,
        val wordCount: Int
    ) : FeedContent()

    data class MediaContent(
        val mediaUrl: String,
        val caption: String,
        val mediaType: MediaType
    ) : FeedContent()

    data class AggregatedData(
        val title: String,
        val summary: String,
        val chartData: String? = null,
        val timeRange: String
    ) : FeedContent()

    // 🌸 ADD THIS:
    data class Text(
        val value: String
    ) : FeedContent()
}
