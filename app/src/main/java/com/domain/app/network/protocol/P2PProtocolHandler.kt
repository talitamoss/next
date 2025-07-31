package com.domain.app.network.protocol

/**
 * Interface for P2P protocol message handling
 * Defines the contract for encoding/decoding P2P messages
 * 
 * File location: app/src/main/java/com/domain/app/network/protocol/P2PProtocolHandler.kt
 */
interface P2PProtocolHandler {
    
    /**
     * Parse a raw message string into a P2P message
     */
    fun parseMessage(rawMessage: String): Result<P2PMessage>
    
    /**
     * Extract typed payload from a message
     */
    fun <T> extractPayload(message: P2PMessage, type: Class<T>): Result<T>
    
    /**
     * Create a ping message for connectivity checks
     */
    fun createPing(): String
    
    /**
     * Create a pong response to a ping
     */
    fun createPong(pingId: String): String
    
    /**
     * Create a feed request message
     */
    fun createFeedRequest(
        requesterId: String,
        since: Long?,
        limit: Int,
        contentTypes: List<String>?
    ): String
    
    /**
     * Create a feed response message
     */
    fun createFeedResponse(
        requestId: String,
        items: List<ContentMetadata>
    ): String
    
    /**
     * Create a content request message
     */
    fun createContentRequest(
        contentId: String,
        requestType: ContentRequest.RequestType
    ): String
    
    /**
     * Create a content response message
     */
    fun createContentResponse(
        requestId: String,
        content: Any
    ): String
    
    /**
     * Create a data share message
     */
    fun createDataShare(
        dataType: String,
        data: Any,
        ephemeral: Boolean = false,
        expiresIn: Long? = null
    ): String
    
    /**
     * Create an error message
     */
    fun createError(
        code: String,
        message: String,
        details: Map<String, String> = emptyMap()
    ): String
}

/**
 * Extension function for reified type extraction
 */
inline fun <reified T> P2PProtocolHandler.extractPayload(message: P2PMessage): Result<T> {
    return extractPayload(message, T::class.java)
}
