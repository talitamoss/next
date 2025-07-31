package com.domain.app.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.network.P2PNetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for adding contacts
 */
@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val networkManager: P2PNetworkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = _uiState.asStateFlow()
    
    
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }


}

data class AddContactUiState(
    val contactLink: String? = null,
    val isGenerating: Boolean = false,
    val isAdding: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)

