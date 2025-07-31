package com.domain.app.network.protocol

import kotlinx.serialization.Serializable

/**
 * P2P Protocol data models and message definitions
 * Contains all data structures used in P2P communication
 * 
 * File location: app/src/main/java/com/domain/app/network/protocol/P2PProtocolModels.kt
 */

/**
 * Base message structure for all P2P communications
 */
@Serializable
data class P2PMessage(
    val id: String = generateMessageId(),
    val type: MessageType,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis(),
    val version: Int = PROTOCOL_VERSION
) {
    companion object {
        const val PROTOCOL_VERSION = 1
        
        fun generateMessageId(): String {
            return "msg_${System.currentTimeMillis()}_${(0..9999).random()}"
        }
    }
}

/**
 * Types of messages that can be sent
 */
@Serializable
enum class MessageType {
    // Basic connectivity
    PING,
    PONG,
    
    // Feed/content discovery
    FEED_REQUEST,
    FEED_RESPONSE,
    
    // Content transfer
    CONTENT_REQUEST,
    CONTENT_RESPONSE,
    
    // Data sharing
    DATA_SHARE,
    DATA_ACKNOWLEDGMENT,
    
    // Contact management
    PROFILE_UPDATE,
    STATUS_UPDATE,
    
    // Error handling
    ERROR
}

/**
 * Feed request - asking a peer for their available content
 */
@Serializable
data class FeedRequest(
    val requesterId: String,
    val since: Long? = null,  // Timestamp to get updates since
    val limit: Int = 50,      // Maximum items to return
    val contentTypes: List<String>? = null  // Filter by content type
)

/**
 * Feed response - list of available content
 */
@Serializable
data class FeedResponse(
    val items: List<FeedItem>,
    val hasMore: Boolean = false,
    val nextCursor: String? = null
)

/**
 * Individual feed item metadata
 */
@Serializable
data class FeedItem(
    val id: String,
    val contentId: String,
    val contentType: String,
    val title: String? = null,
    val preview: String? = null,
    val timestamp: Long,
    val size: Long? = null,
    val encrypted: Boolean = true
)

/**
 * Content metadata (not serializable, for internal use)
 */
data class ContentMetadata(
    val id: String,
    val contentId: String,
    val contentType: String,
    val timestamp: Long,
    val size: Long,
    val mimeType: String,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Request specific content
 */
@Serializable
data class ContentRequest(
    val contentId: String,
    val requestType: RequestType = RequestType.FULL
) {
    @Serializable
    enum class RequestType {
        METADATA_ONLY,  // Just the metadata
        PREVIEW,        // Limited preview
        FULL           // Complete content
    }
}

/**
 * Content response
 */
@Serializable
data class ContentResponse(
    val contentId: String,
    val requestType: ContentRequest.RequestType,
    val data: String? = null,  // Base64 encoded if binary
    val error: ErrorInfo? = null
)

/**
 * Share data with a peer
 */
@Serializable
data class DataShare(
    val dataType: String,
    val data: String,  // JSON encoded data
    val metadata: Map<String, String> = emptyMap(),
    val ephemeral: Boolean = false,  // Should peer delete after viewing?
    val expiresAt: Long? = null
)

/**
 * Error information
 */
@Serializable
data class ErrorInfo(
    val code: String,
    val message: String,
    val details: Map<String, String> = emptyMap()
)
