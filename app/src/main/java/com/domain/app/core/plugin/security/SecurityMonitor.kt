package com.domain.app.core.plugin.security

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors and records all security-related events in the plugin system.
 * Provides real-time monitoring and historical audit trails.
 */
@Singleton
class SecurityMonitor @Inject constructor() {
    private val eventQueue = ConcurrentLinkedQueue<SecurityEvent>()
    private val _securityEvents = MutableStateFlow<List<SecurityEvent>>(emptyList())
    val securityEvents: StateFlow<List<SecurityEvent>> = _securityEvents.asStateFlow()
    
    private val _activeViolations = MutableStateFlow<Map<String, List<SecurityEvent.SecurityViolation>>>(emptyMap())
    val activeViolations: StateFlow<Map<String, List<SecurityEvent.SecurityViolation>>> = _activeViolations.asStateFlow()
    
    private val anomalyDetector = AnomalyDetector()
    
    /**
     * Record a security event
     */
    fun recordSecurityEvent(event: SecurityEvent) {
        eventQueue.offer(event)
        
        // Update state flows
        _securityEvents.value = eventQueue.toList().takeLast(1000) // Keep last 1000 events
        
        // Track violations
        if (event is SecurityEvent.SecurityViolation) {
            val currentViolations = _activeViolations.value.toMutableMap()
            val pluginViolations = currentViolations.getOrDefault(event.pluginId, emptyList())
            currentViolations[event.pluginId] = pluginViolations + event
            _activeViolations.value = currentViolations
            
            // Check for patterns
            anomalyDetector.analyzeViolation(event)
        }
        
        // Log critical events
        when (event) {
            is SecurityEvent.SecurityViolation -> {
                if (event.severity == ViolationSeverity.CRITICAL) {
                    logCriticalEvent(event)
                }
            }
            else -> {} // Handle other event types as needed
        }
    }
    
    /**
     * Get security events for a specific plugin
     */
    fun getPluginSecurityEvents(pluginId: String): List<SecurityEvent> {
        return eventQueue.filter { event ->
            when (event) {
                is SecurityEvent.PermissionRequested -> event.pluginId == pluginId
                is SecurityEvent.PermissionGranted -> event.pluginId == pluginId
                is SecurityEvent.PermissionDenied -> event.pluginId == pluginId
                is SecurityEvent.SecurityViolation -> event.pluginId == pluginId
                is SecurityEvent.DataAccess -> event.pluginId == pluginId
            }
        }
    }
    
    /**
     * Get security summary for a plugin
     */
    fun getPluginSecuritySummary(pluginId: String): SecuritySummary {
        val events = getPluginSecurityEvents(pluginId)
        
        return SecuritySummary(
            pluginId = pluginId,
            totalEvents = events.size,
            violations = events.filterIsInstance<SecurityEvent.SecurityViolation>().size,
            deniedPermissions = events.filterIsInstance<SecurityEvent.PermissionDenied>().size,
            dataAccesses = events.filterIsInstance<SecurityEvent.DataAccess>().size,
            riskScore = calculateRiskScore(events),
            lastEventTime = events.maxByOrNull { 
                when (it) {
                    is SecurityEvent.PermissionRequested -> it.timestamp
                    is SecurityEvent.PermissionGranted -> it.timestamp
                    is SecurityEvent.PermissionDenied -> it.timestamp
                    is SecurityEvent.SecurityViolation -> it.timestamp
                    is SecurityEvent.DataAccess -> it.timestamp
                }
            }?.let {
                when (it) {
                    is SecurityEvent.PermissionRequested -> it.timestamp
                    is SecurityEvent.PermissionGranted -> it.timestamp
                    is SecurityEvent.PermissionDenied -> it.timestamp
                    is SecurityEvent.SecurityViolation -> it.timestamp
                    is SecurityEvent.DataAccess -> it.timestamp
                }
            }
        )
    }
    
    /**
     * Calculate risk score for a plugin based on its behavior
     */
    private fun calculateRiskScore(events: List<SecurityEvent>): Int {
        var score = 0
        
        events.forEach { event ->
            when (event) {
                is SecurityEvent.SecurityViolation -> {
                    score += when (event.severity) {
                        ViolationSeverity.LOW -> 1
                        ViolationSeverity.MEDIUM -> 5
                        ViolationSeverity.HIGH -> 10
                        ViolationSeverity.CRITICAL -> 20
                    }
                }
                is SecurityEvent.PermissionDenied -> score += 2
                is SecurityEvent.DataAccess -> {
                    if (event.accessType == AccessType.DELETE) score += 3
                    if (event.recordCount > 100) score += 2
                }
                else -> {}
            }
        }
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * Check if a plugin should be quarantined
     */
    fun shouldQuarantine(pluginId: String): Boolean {
        val summary = getPluginSecuritySummary(pluginId)
        return summary.riskScore > 50 || 
               anomalyDetector.hasAnomalies(pluginId) ||
               hasRecentCriticalViolation(pluginId)
    }
    
    /**
     * Check for recent critical violations
     */
    private fun hasRecentCriticalViolation(pluginId: String): Boolean {
        val recentTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000) // 24 hours
        
        return getPluginSecurityEvents(pluginId)
            .filterIsInstance<SecurityEvent.SecurityViolation>()
            .any { it.severity == ViolationSeverity.CRITICAL && it.timestamp > recentTime }
    }
    
    /**
     * Clear events older than specified time
     */
    fun cleanupOldEvents(olderThanDays: Int) {
        val cutoffTime = System.currentTimeMillis() - (olderThanDays * 24 * 60 * 60 * 1000L)
        
        eventQueue.removeIf { event ->
            val timestamp = when (event) {
                is SecurityEvent.PermissionRequested -> event.timestamp
                is SecurityEvent.PermissionGranted -> event.timestamp
                is SecurityEvent.PermissionDenied -> event.timestamp
                is SecurityEvent.SecurityViolation -> event.timestamp
                is SecurityEvent.DataAccess -> event.timestamp
            }
            timestamp < cutoffTime
        }
        
        _securityEvents.value = eventQueue.toList()
    }
    
    private fun logCriticalEvent(event: SecurityEvent.SecurityViolation) {
        // In production, this would log to a secure audit system
        println("CRITICAL SECURITY EVENT: Plugin ${event.pluginId} - ${event.violationType}: ${event.details}")
    }
}

/**
 * Security summary for a plugin
 */
data class SecuritySummary(
    val pluginId: String,
    val totalEvents: Int,
    val violations: Int,
    val deniedPermissions: Int,
    val dataAccesses: Int,
    val riskScore: Int,
    val lastEventTime: Long?
)

/**
 * Detects anomalous behavior patterns
 */
private class AnomalyDetector {
    private val anomalies = mutableMapOf<String, MutableList<Anomaly>>()
    
    fun analyzeViolation(violation: SecurityEvent.SecurityViolation) {
        // Detect rapid violations
        val pluginAnomalies = anomalies.getOrPut(violation.pluginId) { mutableListOf() }
        
        // Check for rapid violations (more than 5 in 1 minute)
        val recentViolations = pluginAnomalies.filter {
            it.timestamp > System.currentTimeMillis() - 60000
        }
        
        if (recentViolations.size >= 5) {
            pluginAnomalies.add(
                Anomaly(
                    type = "RAPID_VIOLATIONS",
                    description = "Multiple violations in short time",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }
    
    fun hasAnomalies(pluginId: String): Boolean {
        return anomalies[pluginId]?.isNotEmpty() == true
    }
    
    data class Anomaly(
        val type: String,
        val description: String,
        val timestamp: Long
    )
}
