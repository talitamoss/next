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
 * 
 * File location: app/src/main/java/com/domain/app/ui/contacts/AddContactViewModel.kt
 */
@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val networkManager: P2PNetworkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = _uiState.asStateFlow()
    
    fun onContactLinkChanged(link: String) {
        _uiState.value = _uiState.value.copy(contactLink = link)
    }
    
    fun onNicknameChanged(nickname: String) {
        _uiState.value = _uiState.value.copy(nickname = nickname)
    }
    
    fun addContact() {
        val link = _uiState.value.contactLink
        val nickname = _uiState.value.nickname
        
        if (link.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "Please enter a contact link"
            )
            return
        }
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
            try {
                // TODO: Implement contact adding in BitChat
                // For now, just simulate success
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to add contact"
                )
            }
        }
    }
}

data class AddContactUiState(
    val contactLink: String = "",
    val nickname: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)
