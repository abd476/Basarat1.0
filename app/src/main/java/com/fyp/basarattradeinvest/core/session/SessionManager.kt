package com.fyp.basarattradeinvest.core.session

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionManager {

    private var tokenStorage: SecureTokenStorage? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun init(context: Context) {
        if (tokenStorage == null) {
            tokenStorage = SecureTokenStorage(context.applicationContext)
            checkInitialSession()
        }
    }

    // Visible for testing / dependency injection
    fun init(storage: SecureTokenStorage) {
        tokenStorage = storage
        checkInitialSession()
    }

    private fun checkInitialSession() {
        val storage = tokenStorage ?: return
        if (storage.hasValidTokens()) {
            val email = storage.getUserEmail().orEmpty()
            val name = storage.getUserName()
            _authState.value = AuthState.Authenticated(userEmail = email, userName = name)
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun hasValidSession(): Boolean {
        return tokenStorage?.hasValidTokens() == true
    }

    fun getAccessToken(): String? = tokenStorage?.getAccessToken()

    fun getRefreshToken(): String? = tokenStorage?.getRefreshToken()

    fun getCurrentEmail(): String? = tokenStorage?.getUserEmail()

    fun getCurrentUserName(): String? = tokenStorage?.getUserName()

    fun getCurrentUserId(): String? = tokenStorage?.getUserId()

    fun saveSession(
        accessToken: String,
        refreshToken: String,
        userId: String? = null,
        email: String? = null,
        fullName: String? = null
    ) {
        val storage = tokenStorage ?: return
        storage.saveTokens(accessToken, refreshToken)
        if (email != null || fullName != null || userId != null) {
            storage.saveUserData(userId, email, fullName)
        }
        _authState.value = AuthState.Authenticated(
            userEmail = email ?: storage.getUserEmail().orEmpty(),
            userName = fullName ?: storage.getUserName()
        )
    }

    fun updateTokens(accessToken: String, refreshToken: String) {
        val storage = tokenStorage ?: return
        storage.saveTokens(accessToken, refreshToken)
        _authState.value = AuthState.Authenticated(
            userEmail = storage.getUserEmail().orEmpty(),
            userName = storage.getUserName()
        )
    }

    fun clearSession() {
        tokenStorage?.clear()
        _authState.value = AuthState.Unauthenticated
    }

    fun notifySessionExpired() {
        tokenStorage?.clear()
        _authState.value = AuthState.SessionExpired
    }
}
