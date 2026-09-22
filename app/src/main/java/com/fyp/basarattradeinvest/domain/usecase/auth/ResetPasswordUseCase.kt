package com.fyp.basarattradeinvest.domain.usecase.auth

import com.fyp.basarattradeinvest.domain.repository.AuthRepository

class ResetPasswordUseCase(private val repository: AuthRepository) {
    suspend operator fun invoke(token: String, newPassword: String): Result<Unit> {
        val trimmedToken = token.trim()
        if (trimmedToken.isEmpty()) {
            return Result.failure(IllegalArgumentException("Reset token cannot be empty"))
        }
        if (newPassword.length < 8) {
            return Result.failure(IllegalArgumentException("New password must be at least 8 characters"))
        }
        return repository.resetPassword(trimmedToken, newPassword)
    }
}
