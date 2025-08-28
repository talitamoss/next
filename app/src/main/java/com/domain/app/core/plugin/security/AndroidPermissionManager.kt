package com.domain.app.core.plugin.security

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability  // FIXED: Added missing import
import com.domain.app.core.plugin.getRequiredAndroidPermissions  // FIXED: Extension function import
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Android system-level permissions for plugins.
 * This is separate from PluginPermissionManager which handles app-level capabilities.
 * 
 * Separation rationale:
 * - Android permissions are OS-managed and can be revoked anytime
 * - App capabilities are our internal concept stored in DataStore
 * - Different APIs: Sync Context checks vs Async DataStore operations
 * - Single Responsibility: Each manager has one clear purpose
 */
@Singleton
class AndroidPermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Check if all Android permissions required by a plugin are granted
     */
    fun hasAllRequiredPermissions(plugin: Plugin): Boolean {
        val requiredPermissions = getRequiredPermissions(plugin)
        return requiredPermissions.all { permission ->
            isPermissionGranted(permission)
        }
    }
    
    /**
     * Get all Android permissions required by a plugin based on its capabilities
     */
    fun getRequiredPermissions(plugin: Plugin): List<String> {
        return plugin.securityManifest.requestedCapabilities
            .flatMap { capability ->
                capability.getRequiredAndroidPermissions()
            }
            .distinct()
    }
    
    /**
     * Get only the missing Android permissions for a plugin
     */
    fun getMissingPermissions(plugin: Plugin): List<String> {
        return getRequiredPermissions(plugin).filter { permission ->
            !isPermissionGranted(permission)
        }
    }
    
    /**
     * Check if a specific Android permission is granted
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == 
            PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if any dangerous permissions are required by the plugin
     * (These are the ones that need runtime requests)
     */
    fun requiresRuntimePermissions(plugin: Plugin): Boolean {
        return getRequiredPermissions(plugin).isNotEmpty()
    }
}
