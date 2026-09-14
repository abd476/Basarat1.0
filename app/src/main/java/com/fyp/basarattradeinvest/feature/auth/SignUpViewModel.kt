package com.fyp.basarattradeinvest.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SignUpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signUp(name: String, email: String, password: String) {
        if (name.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your name")
            return
        }
        if (email.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your email")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("Password must be at least 6 characters")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            delay(1000)
            _uiState.value = AuthUiState.Success(userEmail = email)
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
