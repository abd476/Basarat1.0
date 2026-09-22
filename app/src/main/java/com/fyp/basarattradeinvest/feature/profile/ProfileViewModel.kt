package com.fyp.basarattradeinvest.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.AuthRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.UsersRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.User
import com.fyp.basarattradeinvest.domain.usecase.auth.CheckSessionUseCase
import com.fyp.basarattradeinvest.domain.usecase.auth.LogoutUseCase
import com.fyp.basarattradeinvest.domain.usecase.user.GetCurrentUserProfileUseCase
import com.fyp.basarattradeinvest.feature.auth.AuthUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val getCurrentUserProfileUseCase: GetCurrentUserProfileUseCase = GetCurrentUserProfileUseCase(UsersRepositoryImpl()),
    private val logoutUseCase: LogoutUseCase = LogoutUseCase(AuthRepositoryImpl()),
    private val checkSessionUseCase: CheckSessionUseCase = CheckSessionUseCase(AuthRepositoryImpl())
) : ViewModel() {

    private val _userState = MutableStateFlow<User?>(null)
    val userState: StateFlow<User?> = _userState.asStateFlow()

    private val _profileUiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    private val _logoutUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val logoutUiState: StateFlow<AuthUiState> = _logoutUiState.asStateFlow()

    init {
        loadUserProfile()
    }

    fun loadUserProfile() {
        // Fallback session info
        _userState.value = checkSessionUseCase.getUser()

        // Fetch live profile from backend
        viewModelScope.launch {
            _profileUiState.value = ProfileUiState.Loading
            val result = getCurrentUserProfileUseCase()
            result.fold(
                onSuccess = { profile ->
                    _profileUiState.value = ProfileUiState.Success(profile)
                    _userState.value = User(
                        id = profile.id,
                        email = profile.email,
                        username = profile.username,
                        fullName = profile.fullName
                    )
                },
                onFailure = { error ->
                    _profileUiState.value = ProfileUiState.Error(
                        error.message ?: "Failed to load profile details"
                    )
                }
            )
        }
    }

    fun logout() {
        viewModelScope.launch {
            _logoutUiState.value = AuthUiState.Loading
            logoutUseCase()
            _logoutUiState.value = AuthUiState.Success(message = "Logged out successfully")
        }
    }

    fun resetLogoutState() {
        _logoutUiState.value = AuthUiState.Idle
    }
}
