package com.domain.app.network.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.and

/**
 * P2P Protocol implementation for secure communication
 * 
 * This protocol defines how peers communicate, establish trust,
 * and exchange data in the network.
 */
object P2PProtocol {
    const val PROTOCOL_VERSION = "1.0"
    const val DEFAULT_PORT = 8888
    
    // Message types
    object MessageType {
        const val HANDSHAKE = "handshake"
        const val DATA_SHARE = "data_share"
        const val DATA_REQUEST = "data_request"
        const val DATA_RESPONSE = "data_response"
        const val PING = "ping"
        const val PONG = "pong"
        const val DISCONNECT = "disconnect"
    }
    
    // JSON serializer with lenient parsing
    internal val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
}

/**
 * Base message structure for all P2P communications
 */
@Serializable
data class P2PMessage(
    val type: String,
    val version: String = P2PProtocol.PROTOCOL_VERSION,
    val timestamp: Long = System.currentTimeMillis(),
    val senderId: String,
    val payload: String? = null,
    val signature: String? = null
)

/**
 * Handshake message for establishing connection
 */
@Serializable
data class HandshakePayload(
    val publicKey: String,
    val deviceName: String,
    val supportedCapabilities: List<String>
)

/**
 * Data sharing message
 */
@Serializable
data class DataSharePayload(
    val dataType: String,
    val encryptedData: String,
    val checksum: String,
    val sharePermissions: SharePermissions
)

/**
 * Share permissions structure
 */
@Serializable
data class SharePermissions(
    val canReshare: Boolean = false,
    val expiryTime: Long? = null,
    val viewOnly: Boolean = true
)

/**
 * Contact link structure for sharing
 */
@Serializable
data class ContactLink(
    val peerId: String,
    val publicKey: String,
    val endpoint: String,
    val timestamp: Long,
    val signature: String
)

/**
 * Protocol handler interface
 */
interface P2PProtocolHandler {
    suspend fun handleMessage(message: P2PMessage): P2PMessage?
    suspend fun onConnectionEstablished(peerId: String)
    suspend fun onConnectionLost(peerId: String)
}

/**
 * Message builder utilities
 */
object MessageBuilder {
    fun createHandshake(
        senderId: String,
        publicKey: String,
        deviceName: String,
        capabilities: List<String>
    ): P2PMessage {
        val payload = HandshakePayload(
            publicKey = publicKey,
            deviceName = deviceName,
            supportedCapabilities = capabilities
        )
        
        return P2PMessage(
            type = P2PProtocol.MessageType.HANDSHAKE,
            senderId = senderId,
            payload = P2PProtocol.json.encodeToString(payload)
        )
    }
    
    fun createDataShare(
        senderId: String,
        dataType: String,
        encryptedData: String,
        checksum: String,
        permissions: SharePermissions
    ): P2PMessage {
        val payload = DataSharePayload(
            dataType = dataType,
            encryptedData = encryptedData,
            checksum = checksum,
            sharePermissions = permissions
        )
        
        return P2PMessage(
            type = P2PProtocol.MessageType.DATA_SHARE,
            senderId = senderId,
            payload = P2PProtocol.json.encodeToString(payload)
        )
    }
    
    fun createPing(senderId: String): P2PMessage {
        return P2PMessage(
            type = P2PProtocol.MessageType.PING,
            senderId = senderId
        )
    }
    
    fun createPong(senderId: String): P2PMessage {
        return P2PMessage(
            type = P2PProtocol.MessageType.PONG,
            senderId = senderId
        )
    }
}

/**
 * Utility functions for protocol operations
 */
object ProtocolUtils {
    fun generateChecksum(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return hash.joinToString("") { "%02x".format(it) }
    }
    
    fun validateMessage(message: P2PMessage): Boolean {
        // Version check
        if (message.version != P2PProtocol.PROTOCOL_VERSION) {
            return false
        }
        
        // Timestamp validation (within 5 minutes)
        val timeDiff = System.currentTimeMillis() - message.timestamp
        if (timeDiff > 5 * 60 * 1000 || timeDiff < -5 * 60 * 1000) {
            return false
        }
        
        // Type validation
        val validTypes = listOf(
            P2PProtocol.MessageType.HANDSHAKE,
            P2PProtocol.MessageType.DATA_SHARE,
            P2PProtocol.MessageType.DATA_REQUEST,
            P2PProtocol.MessageType.DATA_RESPONSE,
            P2PProtocol.MessageType.PING,
            P2PProtocol.MessageType.PONG,
            P2PProtocol.MessageType.DISCONNECT
        )
        
        return message.type in validTypes
    }
    
    fun encryptData(data: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(key.take(16).toByteArray(), "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(data)
    }
    
    fun decryptData(encryptedData: ByteArray, key: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        val secretKey = SecretKeySpec(key.take(16).toByteArray(), "AES")
        cipher.init(Cipher.DECRYPT_MODE, secretKey)
        return cipher.doFinal(encryptedData)
    }
}
