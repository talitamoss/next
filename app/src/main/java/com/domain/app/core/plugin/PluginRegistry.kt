// app/src/main/java/com/domain/app/core/plugin/PluginRegistry.kt
package com.domain.app.core.plugin

import com.domain.app.plugins.counter.CounterPlugin
import com.domain.app.plugins.wellness.water.WaterPlugin
import com.domain.app.plugins.wellness.mood.MoodPlugin
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry for all available plugins
 * In the future, this could support dynamic plugin loading
 */
@Singleton
class PluginRegistry @Inject constructor() {
    
    private val plugins = mutableMapOf<String, Plugin>()
    
    init {
        // Register built-in plugins
        registerBuiltInPlugins()
    }
    
    /**
     * Register a plugin
     */
    fun register(plugin: Plugin) {
        plugins[plugin.id] = plugin
    }
    
    /**
     * Unregister a plugin
     */
    fun unregister(pluginId: String) {
        plugins.remove(pluginId)
    }
    
    /**
     * Get all registered plugins
     */
    fun getAllPlugins(): List<Plugin> {
        return plugins.values.toList()
    }
    
    /**
     * Get a specific plugin by ID
     */
    fun getPlugin(pluginId: String): Plugin? {
        return plugins[pluginId]
    }
    
    /**
     * Check if a plugin is registered
     */
    fun isRegistered(pluginId: String): Boolean {
        return plugins.containsKey(pluginId)
    }
    
    /**
     * Get plugins that support manual entry
     */
    fun getManualEntryPlugins(): List<Plugin> {
        return plugins.values.filter { it.supportsManualEntry() }
    }
    
    /**
     * Get plugins by category
     */
    fun getPluginsByCategory(category: PluginCategory): List<Plugin> {
        return plugins.values.filter { 
            it.metadata.category == category 
        }
    }
    
    private fun registerBuiltInPlugins() {
        // Register core plugins
        register(CounterPlugin())
        register(WaterPlugin())
        register(MoodPlugin())  // This line needs to be uncommented
        
        // TODO: Add these plugins once implemented:
        // register(SleepPlugin())
        
        // Future: Could scan for plugins using reflection or load from external sources
    }
}
