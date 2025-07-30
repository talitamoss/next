package com.domain.app.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.network.P2PNetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AddContactScreen
 * 
 * File location: app/src/main/java/com/domain/app/ui/contacts/AddContactViewModel.kt
 */
@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val p2pNetworkManager: P2PNetworkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = _uiState.asStateFlow()
    
    fun updateContactLink(link: String) {
        _uiState.update { it.copy(contactLink = link.trim()) }
    }
    
    fun updateNickname(nickname: String) {
        _uiState.update { it.copy(nickname = nickname) }
    }
    
    fun addContact(onSuccess: (String) -> Unit) {
        val currentState = _uiState.value
        
        if (currentState.contactLink.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a contact link") }
            return
        }
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            p2pNetworkManager.addContact(
                contactLink = currentState.contactLink,
                nickname = currentState.nickname.ifBlank { null }
            ).fold(
                onSuccess = { contact ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            contactLink = "",
                            nickname = ""
                        )
                    }
                    onSuccess(contact.id)
                },
                onFailure = { error ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Failed to add contact"
                        )
                    }
                }
            )
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
    
    fun showSnackbar(message: String) {
        // TODO: Implement snackbar
    }
}

/**
 * UI state for add contact screen
 */
data class AddContactUiState(
    val contactLink: String = "",
    val nickname: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
