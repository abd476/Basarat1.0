package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.AuthSession
import com.fyp.basarattradeinvest.domain.model.User

interface AuthRepository {

    suspend fun signup(
        email: String,
        password: String,
        fullName: String?
    ): Result<String>

    suspend fun login(
        email: String,
        password: String
    ): Result<AuthSession>

    suspend fun refresh(
        refreshToken: String
    ): Result<AuthSession>

    suspend fun logout(): Result<Unit>

    suspend fun forgotPassword(
        email: String
    ): Result<Unit>

    suspend fun resetPassword(
        token: String,
        newPassword: String
    ): Result<Unit>

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): Result<Unit>

    fun isUserAuthenticated(): Boolean

    fun getCurrentUser(): User?

    fun clearLocalSession()
}
