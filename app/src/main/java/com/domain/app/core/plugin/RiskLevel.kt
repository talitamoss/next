package com.domain.app.core.plugin

/**
 * Risk level definitions for plugin capabilities and operations
 * Used to communicate privacy and security implications to users
 * 
 * File location: app/src/main/java/com/domain/app/core/plugin/RiskLevel.kt
 */

/**
 * Risk levels for plugin capabilities and operations
 */
enum class RiskLevel {
    LOW,       // Minimal risk to user privacy/security
    MEDIUM,    // Some risk, requires user awareness  
    HIGH,      // Significant risk, requires explicit consent
    CRITICAL   // Maximum risk, requires strong justification
}
