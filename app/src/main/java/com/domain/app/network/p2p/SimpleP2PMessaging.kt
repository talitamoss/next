package com.domain.app.network.p2p

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal P2P messaging setup based on BitChat
 * Start with basic messaging, then add behavioral data later
 * 
 * File location: app/src/main/java/com/domain/app/network/p2p/SimpleP2PMessaging.kt
 */
@Singleton
class SimpleP2PMessaging @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    
    // Store messages locally for pull-based feed
    private val _localMessages = MutableStateFlow<List<P2PMessage>>(emptyList())
    val localMessages: StateFlow<List<P2PMessage>> = _localMessages.asStateFlow()
    
    // Connected peers
    private val _connectedPeers = MutableStateFlow<List<P2PPeer>>(emptyList())
    val connectedPeers: StateFlow<List<P2PPeer>> = _connectedPeers.asStateFlow()
    
    // My identity
    private val myPeerId = generatePeerId()
    private val myNickname = "User${(1000..9999).random()}"
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        prettyPrint = false
    }
    
    /**
     * Initialize P2P messaging
     */
    fun initialize() {
        // For now, we'll simulate - in real implementation, use BitChat's BLE
        simulateP2PConnection()
    }
    
    /**
     * Post a message (make it available for others to pull)
     */
    suspend fun postMessage(content: String, messageType: MessageType = MessageType.PUBLIC) {
        val message = P2PMessage(
            id = UUID.randomUUID().toString(),
            senderId = myPeerId,
            senderNickname = myNickname,
            content = content,
            type = messageType,
            timestamp = Instant.now().toEpochMilli(),
            ttl = 7 // BitChat-style TTL
        )
        
        // Add to our local store (available for others to pull)
        _localMessages.update { messages ->
            (messages + message).sortedByDescending { it.timestamp }
        }
        
        // In real implementation, this would be available via BLE
        // when peers request updates
    }
    
    /**
     * Pull feed from all connected peers
     * This is the key to your pull-based architecture!
     */
    suspend fun pullFeed(): Flow<FeedUpdate> = flow {
        emit(FeedUpdate.RefreshStarted)
        
        // Request feed from each connected peer
        _connectedPeers.value.forEach { peer ->
            try {
                // In real implementation, send BLE request to peer
                // For now, simulate with delay
                val peerMessages = requestFeedFromPeer(peer)
                
                emit(FeedUpdate.PeerUpdated(
                    peerId = peer.id,
                    messages = peerMessages
                ))
            } catch (e: Exception) {
                emit(FeedUpdate.PeerError(peer.id, e))
            }
        }
        
        emit(FeedUpdate.RefreshComplete)
    }
    
    /**
     * Get aggregated feed from all peers
     */
    fun getAggregatedFeed(): Flow<List<FeedItem>> = flow {
        // Combine local messages with pulled messages
        val allMessages = mutableListOf<FeedItem>()
        
        // Add our own messages
        _localMessages.value.forEach { message ->
            allMessages.add(
                FeedItem(
                    message = message,
                    isLocal = true,
                    fromPeer = P2PPeer(myPeerId, myNickname, true)
                )
            )
        }
        
        // Add messages from peers (in real app, these would be cached from pulls)
        // For demo, generate some fake peer messages
        _connectedPeers.value.forEach { peer ->
            repeat(3) { i ->
                allMessages.add(
                    FeedItem(
                        message = P2PMessage(
                            id = UUID.randomUUID().toString(),
                            senderId = peer.id,
                            senderNickname = peer.nickname,
                            content = "Message $i from ${peer.nickname}",
                            type = MessageType.PUBLIC,
                            timestamp = Instant.now().minusSeconds(i * 3600L).toEpochMilli(),
                            ttl = 7
                        ),
                        isLocal = false,
                        fromPeer = peer
                    )
                )
            }
        }
        
        // Sort by timestamp (newest first)
        emit(allMessages.sortedByDescending { it.message.timestamp })
    }
    
    /**
     * Handle incoming feed requests (when peers pull from us)
     */
    fun handleFeedRequest(request: FeedRequest): FeedResponse {
        // Filter messages based on request criteria
        val messages = _localMessages.value.filter { message ->
            // Only return messages newer than requested timestamp
            request.since?.let { message.timestamp > it } ?: true
        }.take(request.limit)
        
        return FeedResponse(
            messages = messages,
            peerId = myPeerId,
            hasMore = messages.size == request.limit
        )
    }
    
    /**
     * Add a peer (in real app, discovered via BLE)
     */
    fun addPeer(peerId: String, nickname: String) {
        _connectedPeers.update { peers ->
            peers + P2PPeer(peerId, nickname, true)
        }
    }
    
    /**
     * Simulate P2P connection for testing
     */
    private fun simulateP2PConnection() {
        // Add some fake peers for testing
        addPeer("peer_1", "Alice")
        addPeer("peer_2", "Bob")
        addPeer("peer_3", "Charlie")
    }
    
    /**
     * Simulate requesting feed from a peer
     */
    private suspend fun requestFeedFromPeer(peer: P2PPeer): List<P2PMessage> {
        // In real implementation, this would:
        // 1. Send BLE packet with FeedRequest
        // 2. Wait for FeedResponse
        // 3. Parse and return messages
        
        // For now, return empty list (peer has no new messages)
        return emptyList()
    }
    
    private fun generatePeerId(): String {
        return "peer_${UUID.randomUUID().toString().take(8)}"
    }
}

/**
 * Data models
 */
@Serializable
data class P2PMessage(
    val id: String,
    val senderId: String,
    val senderNickname: String,
    val content: String,
    val type: MessageType,
    val timestamp: Long,
    val ttl: Int
)

@Serializable
enum class MessageType {
    PUBLIC,      // Visible to all
    PRIVATE,     // Direct message
    BEHAVIORAL   // Your future behavioral data
}

data class P2PPeer(
    val id: String,
    val nickname: String,
    val isOnline: Boolean
)

data class FeedItem(
    val message: P2PMessage,
    val isLocal: Boolean,
    val fromPeer: P2PPeer
)

@Serializable
data class FeedRequest(
    val requesterId: String,
    val since: Long? = null,  // Timestamp to get messages after
    val limit: Int = 50
)

@Serializable
data class FeedResponse(
    val messages: List<P2PMessage>,
    val peerId: String,
    val hasMore: Boolean
)

sealed class FeedUpdate {
    object RefreshStarted : FeedUpdate()
    data class PeerUpdated(val peerId: String, val messages: List<P2PMessage>) : FeedUpdate()
    data class PeerError(val peerId: String, val error: Throwable) : FeedUpdate()
    object RefreshComplete : FeedUpdate()
}
