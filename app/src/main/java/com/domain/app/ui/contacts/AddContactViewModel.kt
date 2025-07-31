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
    
    fun generateContactLink() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true)
            try {
                val link = networkManager.generateContactLink()
                _uiState.value = _uiState.value.copy(
                    contactLink = link,
                    isGenerating = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to generate link: ${e.message}",
                    isGenerating = false
                )
            }
        }
    }
    
    fun addContactFromLink(link: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true)
            try {
                val contact = networkManager.addContactFromLink(link)
                _uiState.value = _uiState.value.copy(
                    isAdding = false,
                    success = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to add contact: ${e.message}",
                    isAdding = false
                )
            }
        }
    }
    
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
