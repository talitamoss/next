package com.domain.app.core.plugin.security

import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.DataSensitivity

/**
 * Security manifest that defines what a plugin can access and do.
 * Every plugin must declare its security requirements upfront.
 */
data class PluginSecurityManifest(
    val requestedCapabilities: Set<PluginCapability>,
    val dataSensitivity: DataSensitivity,
    val networkDomains: List<String> = emptyList(),
    val dataAccess: Set<DataAccessScope> = emptySet(),
    val cryptographicSignature: String? = null,
    val privacyPolicy: String? = null,
    val dataRetention: DataRetentionPolicy = DataRetentionPolicy.DEFAULT
)

/**
 * Defines what data scopes a plugin can access
 */
enum class DataAccessScope {
    OWN_DATA_ONLY,        // Only data the plugin created
    CATEGORY_DATA,        // Data from same category plugins
    ALL_DATA_READ,        // Read any data (high privilege)
    ALL_DATA_WRITE,       // Modify any data (highest privilege)
    USER_PROFILE,         // Access user profile information
    DEVICE_INFO,          // Access device information
    LOCATION,             // Access location data
    BIOMETRIC,            // Access health sensors
    EXPORT_DATA,          // Can export user data
    DELETE_DATA           // Can delete data
}

/**
 * Plugin trust levels based on verification
 */
enum class PluginTrustLevel {
    OFFICIAL,             // Created by app developers
    VERIFIED,             // Community reviewed & cryptographically signed
    COMMUNITY,            // Basic checks passed, community contributed
    UNTRUSTED,            // No verification, use at own risk
    BLOCKED,              // Known malicious or policy violation
    QUARANTINED           // Under review for suspicious behavior
}

/**
 * Data retention policies
 */
enum class DataRetentionPolicy {
    DEFAULT,              // Follow app defaults
    TEMPORARY,            // Delete after session
    USER_CONTROLLED,      // User decides retention
    PERMANENT             // Never auto-delete
}

/**
 * Security events that can occur
 */
sealed class SecurityEvent {
    data class PermissionRequested(
        val pluginId: String,
        val capability: PluginCapability,
        val timestamp: Long = System.currentTimeMillis()
    ) : SecurityEvent()
    
    data class PermissionGranted(
        val pluginId: String,
        val capability: PluginCapability,
        val grantedBy: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : SecurityEvent()
    
    data class PermissionDenied(
        val pluginId: String,
        val capability: PluginCapability,
        val reason: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : SecurityEvent()
    
    data class SecurityViolation(
        val pluginId: String,
        val violationType: String,
        val details: String,
        val severity: ViolationSeverity,
        val timestamp: Long = System.currentTimeMillis()
    ) : SecurityEvent()
    
    data class DataAccess(
        val pluginId: String,
        val dataType: String,
        val accessType: AccessType,
        val recordCount: Int,
        val timestamp: Long = System.currentTimeMillis()
    ) : SecurityEvent()
}

enum class ViolationSeverity {
    LOW,                  // Minor policy violation
    MEDIUM,               // Significant concern
    HIGH,                 // Serious violation
    CRITICAL              // Immediate termination required
}

enum class AccessType {
    READ,
    WRITE,
    DELETE,
    EXPORT
}
