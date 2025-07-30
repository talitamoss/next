package com.domain.app.ui.contacts

import androidx.compose.material3.SnackbarHostState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.network.P2PManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Add Contact screen
 * 
 * File location: app/src/main/java/com/domain/app/ui/contacts/AddContactViewModel.kt
 */
@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val p2pManager: P2PManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = _uiState.asStateFlow()
    
    val snackbarHostState = SnackbarHostState()
    
    fun setContactLink(link: String) {
        _uiState.update { it.copy(contactLink = link, error = null) }
    }
    
    fun setNickname(nickname: String) {
        _uiState.update { it.copy(nickname = nickname) }
    }
    
    fun addContact() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val result = p2pManager.addContact(
                    contactLink = _uiState.value.contactLink,
                    nickname = _uiState.value.nickname.ifBlank { null }
                )
                
                result.fold(
                    onSuccess = {
                        showMessage("Contact added successfully")
                        // Clear form
                        _uiState.update { 
                            it.copy(
                                contactLink = "",
                                nickname = "",
                                isLoading = false
                            )
                        }
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
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "An unexpected error occurred"
                    )
                }
            }
        }
    }
    
    fun showMessage(message: String) {
        viewModelScope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for the Add Contact screen
 */
data class AddContactUiState(
    val contactLink: String = "",
    val nickname: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
