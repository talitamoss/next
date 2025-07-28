package com.domain.app.core.plugin

import com.domain.app.plugins.counter.CounterPlugin
import com.domain.app.plugins.wellness.water.WaterPlugin
import com.domain.app.plugins.wellness.mood.MoodPlugin
import com.domain.app.plugins.wellness.sleep.SleepPlugin
import com.domain.app.plugins.wellness.exercise.ExercisePlugin
import com.domain.app.plugins.health.MedicationPlugin
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PluginRegistry @Inject constructor() {
    
    private val plugins = mutableMapOf<String, Plugin>()
    
    init {
        registerBuiltInPlugins()
    }
    
    fun register(plugin: Plugin) {
        plugins[plugin.id] = plugin
    }
    
    fun unregister(pluginId: String) {
        plugins.remove(pluginId)
    }
    
    fun getAllPlugins(): List<Plugin> {
        return plugins.values.toList()
    }
    
    fun getPlugin(pluginId: String): Plugin? {
        return plugins[pluginId]
    }
    
    fun isRegistered(pluginId: String): Boolean {
        return plugins.containsKey(pluginId)
    }
    
    fun getManualEntryPlugins(): List<Plugin> {
        return plugins.values.filter { it.supportsManualEntry() }
    }
    
    fun getPluginsByCategory(category: PluginCategory): List<Plugin> {
        return plugins.values.filter { 
            it.metadata.category == category 
        }
    }
    
    private fun registerBuiltInPlugins() {
        // Core plugins for Phase 1
        register(WaterPlugin())
        register(MoodPlugin())
        register(SleepPlugin())
        register(ExercisePlugin())
        register(MedicationPlugin())
        register(CounterPlugin())
        
        // More plugins can be added here as they're developed
    }
}
