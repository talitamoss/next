package com.domain.app.network

import com.domain.app.core.data.DataPoint
import com.domain.app.core.data.DataRepository
import com.domain.app.network.protocol.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Main P2P Network Manager - coordinates all BitChat P2P operations
 * Implements pull-based feed architecture for behavioral data sharing
 * 
 * File location: app/src/main/java/com/domain/app/network/P2PNetworkManager.kt
 */
@Singleton
class P2PNetworkManager @Inject constructor(
    private val bitChatService: BitChatService,
    private val protocol: P2PProtocolHandler,
    private val dataRepository: DataRepository,
    private val contentStore: ContentStore,
    private val coroutineScope: CoroutineScope
) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    // Pending requests waiting for responses
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<P2PMessage>>()
    
    // Feed cache
    private val _feedItems = MutableStateFlow<List<FeedItem>>(emptyList())
    val feedItems: StateFlow<List<FeedItem>> = _feedItems.asStateFlow()
    
    // Network events
    private val _networkEvents = MutableSharedFlow<NetworkEvent>()
    val networkEvents: SharedFlow<NetworkEvent> = _networkEvents.asSharedFlow()
    
    // Local peer info
    private var localPeerId: String = ""
    private var localNickname: String = ""
    
    init {
        // Set up message listener for BitChat
        bitChatService.addMessageHandler { peerId, rawMessage ->
            coroutineScope.launch {
                handleIncomingMessage(peerId, rawMessage)
            }
        }
    }
    
    /**
     * Initialize the network manager
     */
    suspend fun initialize(nickname: String): Result<Unit> {
        return try {
            localNickname = nickname
            localPeerId = generatePeerId()
            
            // Initialize BitChat service
            val result = bitChatService.initialize()
            
            result.onSuccess {
                _networkEvents.emit(NetworkEvent.Initialized(localPeerId))
                
                // Start periodic feed refresh
                startPeriodicFeedRefresh()
            }
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Start periodic feed refresh
     */
    private fun startPeriodicFeedRefresh() {
        coroutineScope.launch {
            while (isActive) {
                delay(30000) // 30 seconds
                pullFeedFromPeers()
            }
        }
    }
    
    /**
     * Pull feed from all connected peers
     */
    suspend fun pullFeedFromPeers() {
        val peers = bitChatService.connectedPeers.value
        
        peers.forEach { peer ->
            try {
                val since = getLastSyncTime(peer.id)
                val request = protocol.createFeedRequest(
                    requesterId = localPeerId,
                    since = since,
                    limit = 50,
                    contentTypes = null
                )
                
                sendMessage(peer.id, request)
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to request feed from ${peer.id}")
            }
        }
    }
    
    /**
     * Send a message to a peer
     */
    private suspend fun sendMessage(peerId: String, message: String): Result<Unit> {
        return try {
            bitChatService.sendMessage(peerId, message.toByteArray())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Send message and wait for response
     */
    private suspend fun sendMessageAndWaitForResponse(
        peerId: String,
        message: String,
        timeoutMs: Long = 5000
    ): Result<P2PMessage> {
        return try {
            val messageObj = protocol.parseMessage(message).getOrThrow()
            val deferred = CompletableDeferred<P2PMessage>()
            pendingRequests[messageObj.id] = deferred
            
            sendMessage(peerId, message)
            
            withTimeout(timeoutMs) {
                deferred.await()
            }.let { Result.success(it) }
            
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // Clean up pending request
            pendingRequests.remove(message)
        }
    }
    
    /**
     * Handle incoming message from BitChat
     */
    private suspend fun handleIncomingMessage(peerId: String, rawMessage: ByteArray) {
        try {
            val messageString = String(rawMessage, Charsets.UTF_8)
            val message = protocol.parseMessage(messageString).getOrThrow()
            
            Timber.d("Received ${message.type} from $peerId")
            
            when (message.type) {
                MessageType.PING -> handlePing(peerId, message)
                MessageType.PONG -> handlePong(peerId, message)
                MessageType.FEED_REQUEST -> handleFeedRequest(peerId, message)
                MessageType.FEED_RESPONSE -> handleFeedResponse(peerId, message)
                MessageType.CONTENT_REQUEST -> handleContentRequest(peerId, message)
                MessageType.CONTENT_RESPONSE -> handleContentResponse(peerId, message)
                MessageType.DATA_SHARE -> handleDataShare(peerId, message)
                MessageType.DATA_ACKNOWLEDGMENT -> handleDataAcknowledgment(peerId, message)
                MessageType.PROFILE_UPDATE -> handleProfileUpdate(peerId, message)
                MessageType.STATUS_UPDATE -> handleStatusUpdate(peerId, message)
                MessageType.ERROR -> handleError(peerId, message)
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle message from $peerId")
        }
    }
    
    private suspend fun handlePing(peerId: String, message: P2PMessage) {
        val pong = protocol.createPong(message.id)
        sendMessage(peerId, pong)
    }
    
    private suspend fun handlePong(peerId: String, message: P2PMessage) {
        _networkEvents.emit(NetworkEvent.PeerResponded(peerId))
    }
    
    private suspend fun handleFeedRequest(peerId: String, message: P2PMessage) {
        val request = protocol.extractPayload<FeedRequest>(message).getOrThrow()
        
        // Get available content for this peer
        val availableContent = contentStore.getAvailableContent(peerId)
            .filter { content ->
                // Filter by time if requested
                request.since?.let { since ->
                    content.timestamp > since
                } ?: true
            }
            .filter { content ->
                // Filter by content type if requested
                request.contentTypes?.contains(content.contentType) ?: true
            }
            .take(request.limit)
        
        // Send response
        val response = protocol.createFeedResponse(message.id, availableContent)
        sendMessage(peerId, response)
    }
    
    private suspend fun handleFeedResponse(peerId: String, message: P2PMessage) {
        // Feed response handling is done in the request/response flow
        pendingRequests[message.id]?.complete(message)
        
        val response = protocol.extractPayload<FeedResponse>(message).getOrThrow()
        
        // Convert to local feed items
        val feedItems = response.items.map { item ->
            FeedItem(
                id = item.id,
                contentId = item.contentId,
                contentType = item.contentType,
                title = item.title,
                preview = item.preview,
                timestamp = item.timestamp,
                size = item.size,
                encrypted = item.encrypted
            )
        }
        
        // Update feed
        _feedItems.update { currentItems ->
            (currentItems + feedItems).distinctBy { it.id }
                .sortedByDescending { it.timestamp }
        }
        
        _networkEvents.emit(NetworkEvent.FeedUpdated(peerId, feedItems.size))
    }
    
    private suspend fun handleContentRequest(peerId: String, message: P2PMessage) {
        val request = protocol.extractPayload<ContentRequest>(message).getOrThrow()
        
        // Check if content is available for this peer
        if (!contentStore.isContentAvailableFor(request.contentId, peerId)) {
            val error = protocol.createError(
                code = "CONTENT_NOT_AVAILABLE",
                message = "Content not available or access denied"
            )
            sendMessage(peerId, error)
            return
        }
        
        // Get content based on request type
        val content = when (request.requestType) {
            ContentRequest.RequestType.METADATA_ONLY -> {
                contentStore.getContentMetadata(request.contentId)
            }
            ContentRequest.RequestType.PREVIEW -> {
                contentStore.getContentPreview(request.contentId)
            }
            ContentRequest.RequestType.FULL -> {
                contentStore.getFullContent(request.contentId)
            }
        }
        
        content?.let {
            val response = protocol.createContentResponse(
                requestId = message.id,
                content = it
            )
            sendMessage(peerId, response)
        }
    }
    
    private suspend fun handleContentResponse(peerId: String, message: P2PMessage) {
        // Content response handling
        pendingRequests[message.id]?.complete(message)
    }
    
    private suspend fun handleDataShare(peerId: String, message: P2PMessage) {
        val share = protocol.extractPayload<DataShare>(message).getOrThrow()
        
        // Store shared data
        when (share.dataType) {
            "behavioral_data" -> {
                // Parse and store behavioral data
                try {
                    val dataPoint = json.decodeFromString<DataPoint>(share.data)
                    // Modify the data point to indicate it's from a peer
                    val peerDataPoint = dataPoint.copy(
                        metadata = dataPoint.metadata?.plus(mapOf("from_peer" to peerId)) ?: mapOf("from_peer" to peerId)
                    )
                    dataRepository.insertDataPoints(listOf(peerDataPoint))
                    _networkEvents.emit(NetworkEvent.DataReceived(peerId, peerDataPoint.id))
                } catch (e: Exception) {
                    Timber.e(e, "Failed to parse behavioral data from $peerId")
                }
            }
            else -> {
                Timber.w("Unknown data type received: ${share.dataType}")
            }
        }
    }
    
    private suspend fun handleDataAcknowledgment(peerId: String, message: P2PMessage) {
        // Handle acknowledgment
        _networkEvents.emit(NetworkEvent.DataShared(message.id))
    }
    
    private suspend fun handleProfileUpdate(peerId: String, message: P2PMessage) {
        // Handle profile updates
        _networkEvents.emit(NetworkEvent.PeerConnected(peerId))
    }
    
    private suspend fun handleStatusUpdate(peerId: String, message: P2PMessage) {
        // Handle status updates
    }
    
    private suspend fun handleError(peerId: String, message: P2PMessage) {
        val error = protocol.extractPayload<ErrorInfo>(message).getOrNull()
        Timber.e("Error from $peerId: ${error?.message}")
        _networkEvents.emit(NetworkEvent.PeerError(peerId, error?.message ?: "Unknown error"))
    }
    
    /**
     * Share behavioral data with peers
     */
    suspend fun shareBehavioralData(dataPoint: DataPoint): Result<Unit> {
        return try {
            // Convert data point to shareable format
            val shareData = DataShare(
                dataType = "behavioral_data",
                data = json.encodeToString(dataPoint),
                metadata = mapOf(
                    "plugin_id" to dataPoint.pluginId,
                    "data_type" to dataPoint.type
                ),
                ephemeral = false
            )
            
            // Add to our content store (available for peers to pull)
            contentStore.addContent(
                ContentMetadata(
                    id = dataPoint.id,
                    contentId = dataPoint.id,
                    contentType = "behavioral_data",
                    timestamp = dataPoint.timestamp.toEpochMilli(),
                    size = shareData.data.length.toLong(),
                    mimeType = "application/json",
                    metadata = shareData.metadata
                )
            )
            
            _networkEvents.emit(NetworkEvent.DataShared(dataPoint.id))
            Result.success(Unit)
            
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Helper functions
    
    private fun generatePeerId(): String {
        return "peer_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
    
    private suspend fun getLastSyncTime(peerId: String): Long? {
        // In a real implementation, this would retrieve from storage
        return null
    }
    
    private suspend fun saveLastSyncTime(peerId: String, timestamp: Long) {
        // In a real implementation, this would save to storage
    }
    
    /**
     * Stop the network manager
     */
    fun stop() {
        coroutineScope.cancel()
        bitChatService.stop()
    }
}

/**
 * Network events
 */
sealed class NetworkEvent {
    data class Initialized(val peerId: String) : NetworkEvent()
    data class PeerConnected(val peerId: String) : NetworkEvent()
    data class PeerDisconnected(val peerId: String) : NetworkEvent()
    data class PeerSynced(val peerId: String) : NetworkEvent()
    data class PeerSyncFailed(val peerId: String, val error: Throwable) : NetworkEvent()
    data class PeerResponded(val peerId: String) : NetworkEvent()
    data class PeerError(val peerId: String, val error: String) : NetworkEvent()
    data class DataShared(val dataId: String) : NetworkEvent()
    data class DataReceived(val peerId: String, val dataId: String) : NetworkEvent()
    data class FeedUpdated(val peerId: String, val itemCount: Int) : NetworkEvent()
}
