package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.core.network.NetworkErrorParser
import com.fyp.basarattradeinvest.core.session.SessionManager
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.AuthApiService
import com.fyp.basarattradeinvest.data.remote.dto.auth.ChangePasswordRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.ForgotPasswordRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.LoginRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.LogoutRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.RefreshRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.ResetPasswordRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.SignupRequest
import com.fyp.basarattradeinvest.domain.model.AuthSession
import com.fyp.basarattradeinvest.domain.model.User
import com.fyp.basarattradeinvest.domain.repository.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepositoryImpl(
    private val apiService: AuthApiService = NetworkClient.authApiService
) : AuthRepository {

    override suspend fun signup(
        email: String,
        password: String,
        fullName: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.signup(
                SignupRequest(
                    email = email,
                    password = password,
                    fullName = fullName
                )
            )
            if (response.isSuccessful) {
                val message = response.body()?.message ?: "User registered successfully"
                Result.success(message)
            } else {
                val error = NetworkErrorParser.parseError(response)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun login(
        email: String,
        password: String
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.login(
                LoginRequest(
                    email = email,
                    password = password
                )
            )
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext Result.failure(Exception("Empty login response from server"))
                
                SessionManager.saveSession(
                    accessToken = body.accessToken,
                    refreshToken = body.refreshToken,
                    userId = body.user?.id?.toString(),
                    email = body.user?.email ?: email,
                    fullName = body.user?.fullName
                )

                val user = body.user?.let {
                    User(
                        id = it.id.toString(),
                        email = it.email,
                        username = it.username,
                        fullName = it.fullName
                    )
                } ?: User(id = "", email = email)

                Result.success(
                    AuthSession(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        tokenType = body.tokenType,
                        user = user
                    )
                )
            } else {
                val error = NetworkErrorParser.parseError(response)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refresh(refreshToken: String): Result<AuthSession> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.refresh(RefreshRequest(refreshToken))
            if (response.isSuccessful) {
                val body = response.body() ?: return@withContext Result.failure(Exception("Empty refresh response from server"))
                
                SessionManager.updateTokens(body.accessToken, body.refreshToken)
                
                val user = body.user?.let {
                    User(
                        id = it.id.toString(),
                        email = it.email,
                        username = it.username,
                        fullName = it.fullName
                    )
                }

                Result.success(
                    AuthSession(
                        accessToken = body.accessToken,
                        refreshToken = body.refreshToken,
                        tokenType = body.tokenType,
                        user = user
                    )
                )
            } else {
                val error = NetworkErrorParser.parseError(response)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val refreshToken = SessionManager.getRefreshToken()
            if (!refreshToken.isNullOrBlank()) {
                try {
                    apiService.logout(LogoutRequest(refreshToken))
                } catch (_: Exception) {
                    // Safe graceful degradation: even if server call fails, local session is cleared
                }
            }
            SessionManager.clearSession()
            Result.success(Unit)
        } catch (e: Exception) {
            SessionManager.clearSession()
            Result.success(Unit)
        }
    }

    override suspend fun forgotPassword(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.forgotPassword(ForgotPasswordRequest(email))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = NetworkErrorParser.parseError(response)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun resetPassword(token: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resetPassword(ResetPasswordRequest(token, newPassword))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = NetworkErrorParser.parseError(response)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.changePassword(
                ChangePasswordRequest(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
            )
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val error = NetworkErrorParser.parseError(response)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isUserAuthenticated(): Boolean {
        return SessionManager.hasValidSession()
    }

    override fun getCurrentUser(): User? {
        val email = SessionManager.getCurrentEmail() ?: return null
        return User(
            id = SessionManager.getCurrentUserId().orEmpty(),
            email = email,
            fullName = SessionManager.getCurrentUserName()
        )
    }

    override fun clearLocalSession() {
        SessionManager.clearSession()
    }
}
