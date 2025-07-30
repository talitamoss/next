package com.domain.app.network

import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BitChat P2P Service - Bluetooth LE based peer-to-peer communication
 * Implements pull-based feed architecture for behavioral data sharing
 * 
 * File location: app/src/main/java/com/domain/app/network/BitChatService.kt
 */
@Singleton
class BitChatService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // BitChat service UUID - unique identifier for our P2P network
        val BITCHAT_SERVICE_UUID: UUID = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("00001235-0000-1000-8000-00805f9b34fb")
        
        // Maximum message size for BLE (consider MTU limitations)
        const val MAX_MESSAGE_SIZE = 512
        
        // Connection parameters
        const val SCAN_PERIOD_MS = 10000L
        const val ADVERTISE_PERIOD_MS = 30000L
    }
    
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var gattServer: BluetoothGattServer? = null
    
    // Connection state
    private val _connectionState = MutableStateFlow(ConnectionState.NOT_STARTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    // Connected peers
    private val _connectedPeers = MutableStateFlow<List<BitChatPeer>>(emptyList())
    val connectedPeers: StateFlow<List<BitChatPeer>> = _connectedPeers.asStateFlow()
    
    // Message handlers
    private val messageHandlers = mutableListOf<(String, ByteArray) -> Unit>()
    
    // Active GATT connections
    private val activeConnections = mutableMapOf<String, BluetoothGatt>()
    
    /**
     * Initialize BitChat service
     */
    fun initialize(): Result<Unit> {
        return try {
            if (!bluetoothAdapter.isEnabled) {
                _connectionState.value = ConnectionState.ERROR
                return Result.failure(BluetoothDisabledException())
            }
            
            _connectionState.value = ConnectionState.STARTING
            
            // Initialize BLE components
            bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            
            // Set up GATT server for incoming connections
            setupGattServer()
            
            // Start advertising our presence
            startAdvertising()
            
            // Start scanning for peers
            startScanning()
            
            _connectionState.value = ConnectionState.READY
            Result.success(Unit)
            
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.ERROR
            Result.failure(BitChatException("Failed to initialize BitChat", e))
        }
    }
    
    /**
     * Start advertising our presence to nearby devices
     */
    private fun startAdvertising() {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0) // Advertise indefinitely
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .build()
            
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BITCHAT_SERVICE_UUID))
            .build()
            
        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
        Timber.d("Started BitChat advertising")
    }
    
    /**
     * Start scanning for nearby BitChat peers
     */
    private fun startScanning() {
        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BITCHAT_SERVICE_UUID))
            .build()
            
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
            
        bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
        Timber.d("Started BitChat scanning")
    }
    
    /**
     * Set up GATT server to handle incoming connections
     */
    private fun setupGattServer() {
        gattServer = bluetoothManager.openGattServer(context, gattServerCallback)
        
        // Create BitChat service
        val service = BluetoothGattService(
            BITCHAT_SERVICE_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        
        // Add characteristic for data exchange
        val characteristic = BluetoothGattCharacteristic(
            CHARACTERISTIC_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or
            BluetoothGattCharacteristic.PROPERTY_WRITE or
            BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or
            BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        
        service.addCharacteristic(characteristic)
        gattServer?.addService(service)
    }
    
    /**
     * Send a message to a specific peer
     */
    fun sendMessage(peerId: String, message: ByteArray): Result<Unit> {
        return try {
            val connection = activeConnections[peerId]
                ?: return Result.failure(PeerNotFoundException(peerId))
                
            if (message.size > MAX_MESSAGE_SIZE) {
                // Fragment large messages
                sendFragmentedMessage(connection, message)
            } else {
                // Send single message
                sendSingleMessage(connection, message)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(BitChatException("Failed to send message", e))
        }
    }
    
    /**
     * Send a single message (fits in one BLE packet)
     */
    private fun sendSingleMessage(gatt: BluetoothGatt, message: ByteArray) {
        val characteristic = gatt.getService(BITCHAT_SERVICE_UUID)
            ?.getCharacteristic(CHARACTERISTIC_UUID)
            ?: throw BitChatException("Characteristic not found")
            
        characteristic.value = message
        gatt.writeCharacteristic(characteristic)
    }
    
    /**
     * Send fragmented message for larger data
     */
    private fun sendFragmentedMessage(gatt: BluetoothGatt, message: ByteArray) {
        // Implementation for fragmenting large messages
        // This would split the message into chunks and send them sequentially
        // For now, we'll keep messages small
        throw NotImplementedError("Message fragmentation not yet implemented")
    }
    
    /**
     * Register a message handler
     */
    fun addMessageHandler(handler: (peerId: String, message: ByteArray) -> Unit) {
        messageHandlers.add(handler)
    }
    
    /**
     * Remove a message handler
     */
    fun removeMessageHandler(handler: (peerId: String, message: ByteArray) -> Unit) {
        messageHandlers.remove(handler)
    }
    
    /**
     * Stop BitChat service
     */
    fun stop() {
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
        bluetoothLeScanner?.stopScan(scanCallback)
        
        activeConnections.values.forEach { it.close() }
        activeConnections.clear()
        
        gattServer?.close()
        
        _connectionState.value = ConnectionState.NOT_STARTED
        _connectedPeers.value = emptyList()
    }
    
    // Callbacks
    
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Timber.d("BitChat advertising started successfully")
        }
        
        override fun onStartFailure(errorCode: Int) {
            Timber.e("BitChat advertising failed: $errorCode")
            _connectionState.value = ConnectionState.ERROR
        }
    }
    
    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            result.device?.let { device ->
                // Connect to discovered peer
                connectToPeer(device)
            }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Timber.e("BitChat scan failed: $errorCode")
        }
    }
    
    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Timber.d("Peer connected: ${device.address}")
                    addConnectedPeer(device)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Timber.d("Peer disconnected: ${device.address}")
                    removeConnectedPeer(device)
                }
            }
        }
        
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            // Handle incoming message
            handleIncomingMessage(device.address, value)
            
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
        }
    }
    
    /**
     * Connect to a discovered peer
     */
    private fun connectToPeer(device: BluetoothDevice) {
        // Avoid duplicate connections
        if (activeConnections.containsKey(device.address)) {
            return
        }
        
        val gatt = device.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Timber.d("Connected to peer: ${device.address}")
                        activeConnections[device.address] = gatt
                        gatt.discoverServices()
                        addConnectedPeer(device)
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Timber.d("Disconnected from peer: ${device.address}")
                        activeConnections.remove(device.address)
                        removeConnectedPeer(device)
                    }
                }
            }
            
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Timber.d("Services discovered for ${device.address}")
                }
            }
            
            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Timber.d("Message sent successfully")
                }
            }
        })
    }
    
    /**
     * Handle incoming message from a peer
     */
    private fun handleIncomingMessage(peerId: String, message: ByteArray) {
        Timber.d("Received message from $peerId: ${message.size} bytes")
        
        // Notify all handlers
        messageHandlers.forEach { handler ->
            handler(peerId, message)
        }
    }
    
    /**
     * Add a connected peer
     */
    private fun addConnectedPeer(device: BluetoothDevice) {
        _connectedPeers.value = _connectedPeers.value + BitChatPeer(
            id = device.address,
            name = device.name ?: "Unknown",
            lastSeen = System.currentTimeMillis()
        )
    }
    
    /**
     * Remove a disconnected peer
     */
    private fun removeConnectedPeer(device: BluetoothDevice) {
        _connectedPeers.value = _connectedPeers.value.filter { it.id != device.address }
    }
}

/**
 * Connection states for BitChat
 */
enum class ConnectionState {
    NOT_STARTED,
    STARTING,
    READY,
    ERROR
}

/**
 * BitChat peer information
 */
data class BitChatPeer(
    val id: String,
    val name: String,
    val lastSeen: Long
)

/**
 * Custom exceptions
 */
class BitChatException(message: String, cause: Throwable? = null) : Exception(message, cause)
class BluetoothDisabledException : Exception("Bluetooth is disabled")
class PeerNotFoundException(peerId: String) : Exception("Peer not found: $peerId")
