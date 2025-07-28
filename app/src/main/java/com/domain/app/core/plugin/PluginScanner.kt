package com.domain.app.core.plugin

import android.util.Log

/**
 * Utility for discovering and analyzing plugins
 * Can be used for documentation generation and validation
 */
object PluginScanner {
    
    private const val TAG = "PluginScanner"
    
    /**
     * Discover all plugins via the registry
     */
    fun discoverPlugins(): List<Plugin> {
        return try {
            val registry = PluginRegistry()
            registry.getAllPlugins()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to discover plugins", e)
            emptyList()
        }
    }
    
    /**
     * Validate plugin implementation
     */
    fun validatePlugin(plugin: Plugin): List<String> {
        val issues = mutableListOf<String>()
        
        // Check required metadata
        if (plugin.metadata.name.isBlank()) {
            issues.add("Plugin name is required")
        }
        
        if (plugin.metadata.description.isBlank()) {
            issues.add("Plugin description is required")
        }
        
        if (plugin.metadata.version.isBlank()) {
            issues.add("Plugin version is required")
        }
        
        // Check export functionality
        if (plugin.exportHeaders().isEmpty()) {
            issues.add("Plugin should define export headers")
        }
        
        // Check for natural language aliases
        if (plugin.metadata.naturalLanguageAliases.isEmpty()) {
            issues.add("Plugin should define natural language aliases for voice/text input")
        }
        
        return issues
    }
    
    /**
     * Generate documentation for a plugin
     */
    fun generateDocumentation(plugin: Plugin): String {
        val doc = StringBuilder()
        
        doc.appendLine("# ${plugin.metadata.name}")
        doc.appendLine()
        doc.appendLine("**ID:** `${plugin.id}`")
        doc.appendLine("**Version:** ${plugin.metadata.version}")
        doc.appendLine("**Category:** ${plugin.metadata.category}")
        doc.appendLine("**Data Pattern:** ${plugin.metadata.dataPattern}")
        doc.appendLine("**Privacy Level:** ${plugin.metadata.dataSensitivity}")
        doc.appendLine()
        
        doc.appendLine("## Description")
        doc.appendLine(plugin.metadata.description)
        doc.appendLine()
        
        if (plugin.metadata.tags.isNotEmpty()) {
            doc.appendLine("## Tags")
            doc.appendLine(plugin.metadata.tags.joinToString(", "))
            doc.appendLine()
        }
        
        if (plugin.metadata.naturalLanguageAliases.isNotEmpty()) {
            doc.appendLine("## Voice/Text Commands")
            plugin.metadata.naturalLanguageAliases.forEach {
                doc.appendLine("- \"$it\"")
            }
            doc.appendLine()
        }
        
        if (plugin.metadata.relatedPlugins.isNotEmpty()) {
            doc.appendLine("## Related Plugins")
            doc.appendLine(plugin.metadata.relatedPlugins.joinToString(", "))
            doc.appendLine()
        }
        
        if (plugin.supportsManualEntry()) {
            doc.appendLine("## Quick Add Support")
            doc.appendLine("This plugin supports manual data entry.")
            
            if (plugin.metadata.supportsMultiStage) {
                doc.appendLine("Multi-stage input is supported.")
            }
            doc.appendLine()
        }
        
        doc.appendLine("## Export Format")
        doc.appendLine("Data can be exported as: ${plugin.metadata.exportFormat}")
        doc.appendLine("Headers: ${plugin.exportHeaders().joinToString(", ")}")
        
        return doc.toString()
    }
}
