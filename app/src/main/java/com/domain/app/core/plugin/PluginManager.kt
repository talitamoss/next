// Update the enablePlugin function (around line 160):
suspend fun enablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
    val plugin = activePlugins[pluginId] ?: return@withContext
    
    try {
        // Check permissions
        val grantedPermissions = permissionManager.getGrantedPermissions(pluginId)
        val requiredPermissions = plugin.securityManifest.requestedCapabilities
        
        // Official plugins can be enabled without explicit permissions
        if (plugin.trustLevel == PluginTrustLevel.OFFICIAL && !grantedPermissions.containsAll(requiredPermissions)) {
            // Auto-grant permissions for official plugins
            permissionManager.grantPermissions(
                pluginId = pluginId,
                permissions = requiredPermissions,
                grantedBy = "system_auto"
            )
        } else if (!grantedPermissions.containsAll(requiredPermissions)) {
            // Non-official plugins need explicit permissions
            val missingPermissions = requiredPermissions - grantedPermissions
            
            securityMonitor.recordSecurityEvent(
                SecurityEvent.PermissionDenied(
                    pluginId = pluginId,
                    capability = missingPermissions.first(),
                    reason = "Missing required permissions to enable plugin"
                )
            )
            
            return@withContext
        }
        
        // Update database state
        database.pluginStateDao().updateCollectingState(pluginId, true)
        database.pluginStateDao().insertOrUpdate(
            database.pluginStateDao().getState(pluginId)?.copy(
                isEnabled = true,
                isCollecting = true
            ) ?: PluginStateEntity(
                pluginId = pluginId,
                isEnabled = true,
                isCollecting = true
            )
        )
        database.pluginStateDao().clearErrors(pluginId)
        
        EventBus.emit(Event.PluginStateChanged(pluginId, true))
        
    } catch (e: Exception) {
        handlePluginError(pluginId, e)
    }
}

// Update the disablePlugin function:
suspend fun disablePlugin(pluginId: String) = withContext(Dispatchers.IO) {
    val plugin = activePlugins[pluginId] ?: return@withContext
    
    try {
        // Update database state
        database.pluginStateDao().updateCollectingState(pluginId, false)
        database.pluginStateDao().insertOrUpdate(
            database.pluginStateDao().getState(pluginId)?.copy(
                isEnabled = false,
                isCollecting = false
            ) ?: PluginStateEntity(
                pluginId = pluginId,
                isEnabled = false,
                isCollecting = false
            )
        )
        
        EventBus.emit(Event.PluginStateChanged(pluginId, false))
        
    } catch (e: Exception) {
        handlePluginError(pluginId, e)
    }
}
