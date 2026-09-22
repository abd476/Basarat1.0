package com.fyp.basarattradeinvest.data.remote.dto.auth

import com.fyp.basarattradeinvest.data.remote.dto.user.UserSummaryDto
import com.google.gson.annotations.SerializedName

// POST /api/v1/auth/signup Request
data class SignupRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("full_name")
    val fullName: String? = null
)

// POST /api/v1/auth/login Request
data class LoginRequest(
    @SerializedName("email")
    val email: String,

    @SerializedName("password")
    val password: String
)

// POST /api/v1/auth/refresh Request
data class RefreshRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

// POST /api/v1/auth/logout Request
data class LogoutRequest(
    @SerializedName("refresh_token")
    val refreshToken: String
)

// POST /api/v1/auth/forgot-password Request
data class ForgotPasswordRequest(
    @SerializedName("email")
    val email: String
)

// POST /api/v1/auth/reset-password Request
data class ResetPasswordRequest(
    @SerializedName("token")
    val token: String,

    @SerializedName("new_password")
    val newPassword: String
)

// POST /api/v1/auth/change-password Request
data class ChangePasswordRequest(
    @SerializedName("current_password")
    val currentPassword: String,

    @SerializedName("new_password")
    val newPassword: String
)

// Response from /api/v1/auth/login and /api/v1/auth/refresh
data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String,

    @SerializedName("refresh_token")
    val refreshToken: String,

    @SerializedName("token_type")
    val tokenType: String = "bearer",

    @SerializedName("user")
    val user: UserSummaryDto? = null
)

// Response from /api/v1/auth/signup (201 Created)
data class MessageResponse(
    @SerializedName("message")
    val message: String
)

// OpenAPI 422 Validation Error Schemas
data class HTTPValidationError(
    @SerializedName("detail")
    val detail: List<ValidationErrorItem>? = null
)

data class ValidationErrorItem(
    @SerializedName("loc")
    val loc: List<Any>? = null,

    @SerializedName("msg")
    val msg: String? = null,

    @SerializedName("type")
    val type: String? = null
)
