package com.fyp.basarattradeinvest.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email")
            return
        }
        if (password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your password")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            // Simulate authentication request
            delay(1000)
            _uiState.value = AuthUiState.Success(userEmail = email)
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
