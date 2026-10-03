package com.nk.expensetracker.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nk.expensetracker.core.security.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PinMode {
    SETUP,
    VERIFY
}

data class PinUiState(
    val mode: PinMode = PinMode.VERIFY,
    val currentPin: String = "",
    val error: String? = null,
    val isAuthenticated: Boolean = false,
    val isSetupComplete: Boolean = false
)

@HiltViewModel
class PinViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(PinUiState())
    val uiState: StateFlow<PinUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val isEnabled = authManager.isPinEnabled.first()
            _uiState.update { 
                it.copy(
                    mode = if (isEnabled) PinMode.VERIFY else PinMode.SETUP,
                    isAuthenticated = !isEnabled // If not enabled, user is authenticated by default
                )
            }
        }
    }

    fun onPinChanged(pin: String) {
        if (pin.length > 4) return
        
        _uiState.update { it.copy(currentPin = pin, error = null) }
        
        if (pin.length == 4) {
            submitPin()
        }
    }

    private fun submitPin() {
        val pin = _uiState.value.currentPin
        
        viewModelScope.launch {
            if (_uiState.value.mode == PinMode.SETUP) {
                authManager.setPin(pin)
                _uiState.update { it.copy(isSetupComplete = true, isAuthenticated = true) }
            } else {
                val isValid = authManager.verifyPin(pin)
                if (isValid) {
                    _uiState.update { it.copy(isAuthenticated = true, currentPin = "") }
                } else {
                    _uiState.update { it.copy(error = "Incorrect PIN", currentPin = "") }
                }
            }
        }
    }
}
