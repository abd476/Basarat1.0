package com.fyp.basarattradeinvest.feature.auth

sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Success(
        val message: String? = null,
        val userEmail: String? = null
    ) : AuthUiState
    data class Error(val message: String) : AuthUiState
}
