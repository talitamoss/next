package com.domain.app.core.plugin


import com.domain.app.plugins.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registry for all available plugins using flat structure
 * Plugins are organized by metadata rather than directory structure
 */
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
    
    fun getPluginsByDataPattern(pattern: DataPattern): List<Plugin> {
        return plugins.values.filter {
            it.metadata.dataPattern == pattern
        }
    }
    
    fun getPluginsByTag(tag: String): List<Plugin> {
        return plugins.values.filter {
            it.metadata.tags.contains(tag)
        }
    }
    
    fun getPluginsBySensitivity(sensitivity: DataSensitivity): List<Plugin> {
        return plugins.values.filter {
            it.metadata.dataSensitivity == sensitivity
        }
    }
    
    fun searchPlugins(query: String): List<Plugin> {
        val lowercaseQuery = query.lowercase()
        return plugins.values.filter { plugin ->
            plugin.metadata.name.lowercase().contains(lowercaseQuery) ||
            plugin.metadata.description.lowercase().contains(lowercaseQuery) ||
            plugin.metadata.tags.any { it.lowercase().contains(lowercaseQuery) } ||
            plugin.metadata.naturalLanguageAliases.any { it.lowercase().contains(lowercaseQuery) }
        }
    }
    
    private fun registerBuiltInPlugins() {
        // Register all plugins from flat structure
	register(WaterPlugin())
	register(SleepPlugin())
	register(MovementPlugin())
	register(WorkPlugin())
	register(CaffeinePlugin())
	register(AlcoholPlugin())
	register(ScreenTimePlugin())
	register(SocialPlugin())
	register(MeditationPlugin())
	register(FoodPlugin())
	register(JournalPlugin())
	register(AudioPlugin())
	register(MedicalPlugin())
        
        // Future plugins will be added here
        // register(JournalPlugin())
        // register(ExpensePlugin())
        // register(HabitPlugin())
        // etc.
    }
}
