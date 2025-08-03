package com.domain.app.ui.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.domain.app.network.P2PNetworkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for adding contacts
 * Handles contact link generation and parsing for P2P connections
 * 
 * File location: app/src/main/java/com/domain/app/ui/contacts/AddContactViewModel.kt
 */
@HiltViewModel
class AddContactViewModel @Inject constructor(
    private val networkManager: P2PNetworkManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AddContactUiState())
    val uiState: StateFlow<AddContactUiState> = _uiState.asStateFlow()
    
    /**
     * Generate a unique contact link for sharing
     * Creates a temporary exchange code that allows another user to add this device as a contact
     */
    fun generateContactLink() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGenerating = true, error = null)
            
            try {
                // Generate a unique contact exchange code
                val exchangeCode = UUID.randomUUID().toString()
                
                // Register the exchange code with the network manager
                networkManager.registerContactExchangeCode(exchangeCode)
                
                // Create a shareable link format
                val contactLink = "app://contact/add/$exchangeCode"
                
                _uiState.value = _uiState.value.copy(
                    contactLink = contactLink,
                    isGenerating = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to generate contact link: ${e.message}",
                    isGenerating = false
                )
            }
        }
    }
    
    /**
     * Add a contact from a shared link
     * Parses the contact link and establishes a P2P connection
     */
    fun addContactFromLink(link: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdding = true, error = null)
            
            try {
                // Validate link format
                if (!link.startsWith("app://contact/add/")) {
                    throw IllegalArgumentException("Invalid contact link format")
                }
                
                // Extract exchange code from link
                val exchangeCode = link.removePrefix("app://contact/add/")
                
                // Validate exchange code format
                if (exchangeCode.isBlank() || !isValidUUID(exchangeCode)) {
                    throw IllegalArgumentException("Invalid exchange code")
                }
                
                // Attempt to establish contact through network manager
                val success = networkManager.addContactFromExchangeCode(exchangeCode)
                
                if (success) {
                    _uiState.value = _uiState.value.copy(
                        success = true,
                        isAdding = false
                    )
                } else {
                    throw Exception("Failed to establish contact connection")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = when (e) {
                        is IllegalArgumentException -> e.message
                        else -> "Failed to add contact: ${e.message}"
                    },
                    isAdding = false
                )
            }
        }
    }
    
    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    /**
     * Reset the UI state
     */
    fun resetState() {
        _uiState.value = AddContactUiState()
    }
    
    /**
     * Validate UUID format
     */
    private fun isValidUUID(uuid: String): Boolean {
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }
}

/**
 * UI state for the Add Contact screen
 */
data class AddContactUiState(
    val contactLink: String? = null,
    val isGenerating: Boolean = false,
    val isAdding: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)
