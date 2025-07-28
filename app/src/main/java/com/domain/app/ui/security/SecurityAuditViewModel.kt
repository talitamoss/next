package com.domain.app.ui.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.plugin.PluginManager
import com.domain.app.core.plugin.security.SecurityEvent
import com.domain.app.core.plugin.security.SecurityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SecurityAuditViewModel @Inject constructor(
    private val securityMonitor: SecurityMonitor,
    private val pluginManager: PluginManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SecurityAuditUiState())
    val uiState: StateFlow<SecurityAuditUiState> = _uiState.asStateFlow()
    
    init {
        observeSecurityEvents()
        loadPluginNames()
    }
    
    private fun observeSecurityEvents() {
        // Observe all security events
        securityMonitor.securityEvents
            .onEach { events ->
                _uiState.update {
                    it.copy(
                        recentEvents = events.takeLast(50).reversed(),
                        totalEvents = events.size,
                        violations = events.filterIsInstance<SecurityEvent.SecurityViolation>().size
                    )
                }
            }
            .launchIn(viewModelScope)
        
        // Observe active violations
        securityMonitor.activeViolations
            .onEach { violations ->
                _uiState.update {
                    it.copy(
                        activeViolations = violations.toList(),
                        highRiskPlugins = violations.count { (_, pluginViolations) ->
                            pluginViolations.any { 
                                it.severity == com.domain.app.core.plugin.security.ViolationSeverity.HIGH ||
                                it.severity == com.domain.app.core.plugin.security.ViolationSeverity.CRITICAL
                            }
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }
    
    private fun loadPluginNames() {
        val plugins = pluginManager.getAllActivePlugins()
        val names = plugins.associate { it.id to it.metadata.name }
        _uiState.update { it.copy(pluginNames = names) }
    }
    
    fun refreshEvents() {
        // In real app, might trigger a refresh
        // For now, just reload
        observeSecurityEvents()
    }
}

data class SecurityAuditUiState(
    val recentEvents: List<SecurityEvent> = emptyList(),
    val activeViolations: List<Pair<String, List<SecurityEvent.SecurityViolation>>> = emptyList(),
    val pluginNames: Map<String, String> = emptyMap(),
    val totalEvents: Int = 0,
    val violations: Int = 0,
    val highRiskPlugins: Int = 0
)
