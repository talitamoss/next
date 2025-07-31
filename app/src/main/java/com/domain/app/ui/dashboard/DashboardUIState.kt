package com.domain.app.ui.dashboard

import com.domain.app.core.plugin.Plugin
import com.domain.app.core.plugin.PluginCapability
import com.domain.app.core.plugin.PluginState

data class DashboardUiState(
    val allPlugins: List<Plugin> = emptyList(),
    val dashboardPlugins: List<Plugin> = emptyList(),
    val pluginStates: Map<String, PluginState> = emptyMap(),
    val pluginPermissions: Map<String, Set<PluginCapability>> = emptyMap(),
    val todayEntryCount: Int = 0,
    val weekEntryCount: Int = 0,
    val activePluginCount: Int = 0,
    val canAddMorePlugins: Boolean = true,
    val showQuickAdd: Boolean = false,
    val showPluginSelector: Boolean = false,
    val showSuccessFeedback: Boolean = false,
    val needsPermission: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)
