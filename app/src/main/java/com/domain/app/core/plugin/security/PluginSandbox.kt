package com.domain.app.core.plugin.security

import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

/**
 * Sandbox environment for secure plugin execution.
 * Enforces permissions, monitors resource usage, and prevents malicious behavior.
 */
class PluginSandbox(
    private val plugin: Plugin,
    private val grantedCapabilities: Set<PluginCapability>,
    private val securityMonitor: SecurityMonitor
) {
    private val resourceUsage = ResourceUsage()
    private val operationCounts = ConcurrentHashMap<String, AtomicInteger>()
    private var sandboxScope: CoroutineScope? = null
    
    /**
     * Execute plugin code within sandbox
     */
    suspend fun <T> executeInSandbox(
        operation: String,
        block: suspend () -> T
    ): Result<T> {
        return try {
            // Pre-execution checks
            checkPermissions(operation)
            checkRateLimits(operation)
            
            // Create isolated coroutine scope
            withContext(createSandboxedContext()) {
                // Monitor execution
                val startTime = System.currentTimeMillis()
                val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                
                try {
                    // Execute plugin code
                    val result = block()
                    
                    // Post-execution monitoring
                    recordResourceUsage(
                        operation = operation,
                        duration = System.currentTimeMillis() - startTime,
                        memoryDelta = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - startMemory
                    )
                    
                    Result.success(result)
                } catch (e: Exception) {
                    // Log security events
                    securityMonitor.recordSecurityEvent(
                        SecurityEvent.SecurityViolation(
                            pluginId = plugin.id,
                            violationType = "EXECUTION_ERROR",
                            details = e.message ?: "Unknown error",
                            severity = ViolationSeverity.MEDIUM
                        )
                    )
                    Result.failure(e)
                }
            }
        } catch (e: SecurityException) {
            Result.failure(e)
        }
    }
    
    /**
     * Create sandboxed coroutine context with restrictions
     */
    private fun createSandboxedContext(): CoroutineContext {
        return Dispatchers.Default + SupervisorJob() + CoroutineExceptionHandler { _, exception ->
            securityMonitor.recordSecurityEvent(
                SecurityEvent.SecurityViolation(
                    pluginId = plugin.id,
                    violationType = "COROUTINE_EXCEPTION",
                    details = exception.message ?: "Coroutine exception",
                    severity = ViolationSeverity.HIGH
                )
            )
        }
    }
    
    /**
     * Check if plugin has required permissions
     */
    private fun checkPermissions(operation: String) {
        val requiredCapability = getRequiredCapability(operation)
        
        if (requiredCapability != null && requiredCapability !in grantedCapabilities) {
            securityMonitor.recordSecurityEvent(
                SecurityEvent.PermissionDenied(
                    pluginId = plugin.id,
                    capability = requiredCapability,
                    reason = "Capability not granted for operation: $operation"
                )
            )
            throw SecurityException("Permission denied: $requiredCapability required for $operation")
        }
    }
    
    /**
     * Enforce rate limits to prevent abuse
     */
    private fun checkRateLimits(operation: String) {
        val count = operationCounts.computeIfAbsent(operation) { AtomicInteger(0) }
        val currentCount = count.incrementAndGet()
        
        val limit = getRateLimit(operation)
        if (currentCount > limit) {
            securityMonitor.recordSecurityEvent(
                SecurityEvent.SecurityViolation(
                    pluginId = plugin.id,
                    violationType = "RATE_LIMIT_EXCEEDED",
                    details = "Operation $operation exceeded limit of $limit",
                    severity = ViolationSeverity.HIGH
                )
            )
            throw SecurityException("Rate limit exceeded for operation: $operation")
        }
    }
    
    /**
     * Record resource usage for monitoring
     */
    private fun recordResourceUsage(operation: String, duration: Long, memoryDelta: Long) {
        resourceUsage.recordOperation(operation, duration, memoryDelta)
        
        // Check for excessive resource usage
        if (duration > 5000) { // 5 seconds
            securityMonitor.recordSecurityEvent(
                SecurityEvent.SecurityViolation(
                    pluginId = plugin.id,
                    violationType = "EXCESSIVE_EXECUTION_TIME",
                    details = "Operation $operation took ${duration}ms",
                    severity = ViolationSeverity.MEDIUM
                )
            )
        }
        
        if (memoryDelta > 50 * 1024 * 1024) { // 50MB
            securityMonitor.recordSecurityEvent(
                SecurityEvent.SecurityViolation(
                    pluginId = plugin.id,
                    violationType = "EXCESSIVE_MEMORY_USAGE",
                    details = "Operation $operation used ${memoryDelta / 1024 / 1024}MB",
                    severity = ViolationSeverity.MEDIUM
                )
            )
        }
    }
    
    /**
     * Map operations to required capabilities
     */
    private fun getRequiredCapability(operation: String): PluginCapability? {
        return when {
            operation.startsWith("data.read") -> PluginCapability.READ_OWN_DATA
            operation.startsWith("data.write") -> PluginCapability.COLLECT_DATA
            operation.startsWith("data.delete") -> PluginCapability.DELETE_DATA
            operation.startsWith("network") -> PluginCapability.NETWORK_ACCESS
            operation.startsWith("file") -> PluginCapability.FILE_ACCESS
            operation.startsWith("notification") -> PluginCapability.SHOW_NOTIFICATIONS
            else -> null
        }
    }
    
    /**
     * Get rate limit for operation
     */
    private fun getRateLimit(operation: String): Int {
        return when {
            operation.startsWith("data.read") -> 1000 // 1000 reads per session
            operation.startsWith("data.write") -> 100 // 100 writes per session
            operation.startsWith("network") -> 50 // 50 network requests
            operation.startsWith("notification") -> 10 // 10 notifications
            else -> 100 // Default limit
        }
    }
    
    /**
     * Clean up sandbox resources
     */
    fun cleanup() {
        sandboxScope?.cancel()
        operationCounts.clear()
    }
    
    /**
     * Get current resource usage
     */
    fun getResourceUsage(): ResourceUsage = resourceUsage
}

/**
 * Track resource usage for plugins
 */
class ResourceUsage {
    private val operations = mutableListOf<OperationRecord>()
    
    fun recordOperation(operation: String, duration: Long, memoryDelta: Long) {
        operations.add(
            OperationRecord(
                operation = operation,
                duration = duration,
                memoryDelta = memoryDelta,
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    fun getTotalOperations(): Int = operations.size
    
    fun getTotalDuration(): Long = operations.sumOf { it.duration }
    
    fun getTotalMemoryUsage(): Long = operations.sumOf { it.memoryDelta }
    
    fun getOperationsByType(type: String): List<OperationRecord> {
        return operations.filter { it.operation.startsWith(type) }
    }
    
    data class OperationRecord(
        val operation: String,
        val duration: Long,
        val memoryDelta: Long,
        val timestamp: Long
    )
}
