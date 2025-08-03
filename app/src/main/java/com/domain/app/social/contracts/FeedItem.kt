package com.domain.app.social.contracts

import java.time.Instant

/**
 * Feed item domain model
 * Represents a single item in the P2P social feed
 * 
 * File location: app/src/main/java/com/domain/app/social/contracts/FeedItem.kt
 */
data class FeedItem(
    val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val timestamp: Instant,
    val type: FeedItemType,
    val attachments: List<FeedAttachment> = emptyList(),
    val tags: List<String> = emptyList(),
    val isEncrypted: Boolean = false,
    val signature: String? = null
)

/**
 * Types of feed items
 */
enum class FeedItemType {
    TEXT,
    DATA_SHARE,
    MILESTONE,
    INSIGHT,
    QUESTION,
    RESPONSE
}

/**
 * Feed attachment
 */
data class FeedAttachment(
    val id: String,
    val type: AttachmentType,
    val mimeType: String,
    val size: Long,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Attachment types
 */
enum class AttachmentType {
    IMAGE,
    DATA_VISUALIZATION,
    PLUGIN_DATA,
    DOCUMENT,
    LINK
}

/**
 * Extension functions for FeedItem
 */

/**
 * Convert domain FeedItem to network protocol format
 */
fun FeedItem.toNetworkProtocol(): com.domain.app.network.protocol.FeedItem {
    return com.domain.app.network.protocol.FeedItem(
        id = this.id,
        authorId = this.authorId,
        content = this.content,
        timestamp = this.timestamp.toEpochMilli(),
        type = this.type.name,
        isEncrypted = this.isEncrypted,
        signature = this.signature
    )
}

/**
 * Convert network protocol FeedItem to domain format
 */
fun com.domain.app.network.protocol.FeedItem.toDomainModel(authorName: String): FeedItem {
    return FeedItem(
        id = this.id,
        authorId = this.authorId,
        authorName = authorName,
        content = this.content,
        timestamp = Instant.ofEpochMilli(this.timestamp),
        type = FeedItemType.valueOf(this.type),
        attachments = emptyList(), // Attachments would be loaded separately
        tags = emptyList(), // Tags would be parsed from content or metadata
        isEncrypted = this.isEncrypted,
        signature = this.signature
    )
}
