package com.domain.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.core.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PluginSelectorViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    
    fun addPluginToDashboard(pluginId: String) {
        viewModelScope.launch {
            preferencesManager.addToDashboard(pluginId)
        }
    }
}
