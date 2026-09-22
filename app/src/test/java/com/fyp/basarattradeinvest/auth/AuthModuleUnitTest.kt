package com.fyp.basarattradeinvest.auth

import com.fyp.basarattradeinvest.data.remote.api.AuthApiService
import com.fyp.basarattradeinvest.data.remote.dto.auth.AuthPayloads
import com.fyp.basarattradeinvest.data.remote.dto.auth.ChangePasswordRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.ForgotPasswordRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.LoginRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.LogoutRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.MessageResponse
import com.fyp.basarattradeinvest.data.remote.dto.auth.RefreshRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.ResetPasswordRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.SignupRequest
import com.fyp.basarattradeinvest.data.remote.dto.auth.TokenResponse
import com.fyp.basarattradeinvest.data.remote.dto.user.UserSummaryDto
import com.fyp.basarattradeinvest.data.repository.AuthRepositoryImpl
import com.fyp.basarattradeinvest.domain.usecase.auth.ChangePasswordUseCase
import com.fyp.basarattradeinvest.domain.usecase.auth.ForgotPasswordUseCase
import com.fyp.basarattradeinvest.domain.usecase.auth.LoginUseCase
import com.fyp.basarattradeinvest.domain.usecase.auth.ResetPasswordUseCase
import com.fyp.basarattradeinvest.domain.usecase.auth.SignUpUseCase
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class AuthModuleUnitTest {

    // Fake AuthApiService implementation for isolated, deterministic unit testing
    private val fakeApiService = object : AuthApiService {
        var shouldFailLogin = false
        var shouldFailSignup = false

        override suspend fun signup(request: SignupRequest): Response<MessageResponse> {
            return if (shouldFailSignup) {
                val errorBody = "{\"detail\":\"Email already registered\"}"
                    .toResponseBody("application/json".toMediaType())
                Response.error(400, errorBody)
            } else {
                Response.success(MessageResponse("User registered successfully"))
            }
        }

        override suspend fun login(request: LoginRequest): Response<TokenResponse> {
            return if (shouldFailLogin) {
                val errorBody = "{\"detail\":\"Incorrect email or password\"}"
                    .toResponseBody("application/json".toMediaType())
                Response.error(401, errorBody)
            } else {
                val tokenResponse = TokenResponse(
                    accessToken = "mock_jwt_access_token",
                    refreshToken = "mock_jwt_refresh_token",
                    tokenType = "bearer",
                    user = UserSummaryDto(
                        id = "user_123",
                        email = request.email,
                        username = "test_user",
                        fullName = "Test User"
                    )
                )
                Response.success(tokenResponse)
            }
        }

        override suspend fun refresh(request: RefreshRequest): Response<TokenResponse> {
            val tokenResponse = TokenResponse(
                accessToken = "new_mock_jwt_access_token",
                refreshToken = "new_mock_jwt_refresh_token",
                tokenType = "bearer"
            )
            return Response.success(tokenResponse)
        }

        override suspend fun logout(request: LogoutRequest): Response<Unit> {
            return Response.success(204, Unit)
        }

        override suspend fun forgotPassword(request: ForgotPasswordRequest): Response<Unit> {
            return Response.success(202, Unit)
        }

        override suspend fun resetPassword(request: ResetPasswordRequest): Response<Unit> {
            return Response.success(204, Unit)
        }

        override suspend fun changePassword(request: ChangePasswordRequest): Response<Unit> {
            return Response.success(204, Unit)
        }
    }

    private val authRepository = AuthRepositoryImpl(fakeApiService)

    @Test
    fun testLoginSuccess() = runBlocking {
        fakeApiService.shouldFailLogin = false
        val result = authRepository.login("test@basarat.pk", "Password123!")

        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals("mock_jwt_access_token", session?.accessToken)
        assertEquals("test@basarat.pk", session?.user?.email)
    }

    @Test
    fun testLoginFailure() = runBlocking {
        fakeApiService.shouldFailLogin = true
        val result = authRepository.login("test@basarat.pk", "WrongPassword")

        assertTrue(result.isFailure)
        val errorMsg = result.exceptionOrNull()?.message
        assertTrue(errorMsg?.contains("Incorrect email or password") == true)
    }

    @Test
    fun testSignupSuccess() = runBlocking {
        fakeApiService.shouldFailSignup = false
        val result = authRepository.signup("newuser@basarat.pk", "Password123!", "New User")

        assertTrue(result.isSuccess)
        assertEquals("User registered successfully", result.getOrNull())
    }

    @Test
    fun testSignupFailureDuplicateEmail() = runBlocking {
        fakeApiService.shouldFailSignup = true
        val result = authRepository.signup("existing@basarat.pk", "Password123!", "New User")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Email already registered") == true)
    }

    @Test
    fun testSignUpUseCaseValidation() = runBlocking {
        val signUpUseCase = SignUpUseCase(authRepository)

        // Email empty
        val emptyEmail = signUpUseCase("", "Password123!", "User")
        assertTrue(emptyEmail.isFailure)
        assertEquals("Email cannot be empty", emptyEmail.exceptionOrNull()?.message)

        // Password shorter than 8 chars (Swagger constraint)
        val shortPassword = signUpUseCase("user@basarat.pk", "short", "User")
        assertTrue(shortPassword.isFailure)
        assertEquals("Password must be at least 8 characters", shortPassword.exceptionOrNull()?.message)
    }

    @Test
    fun testLoginUseCaseValidation() = runBlocking {
        val loginUseCase = LoginUseCase(authRepository)

        val emptyEmail = loginUseCase("", "Password123!")
        assertTrue(emptyEmail.isFailure)

        val emptyPassword = loginUseCase("user@basarat.pk", "")
        assertTrue(emptyPassword.isFailure)
    }

    @Test
    fun testForgotPasswordUseCase() = runBlocking {
        val forgotPasswordUseCase = ForgotPasswordUseCase(authRepository)

        val emptyEmail = forgotPasswordUseCase("")
        assertTrue(emptyEmail.isFailure)

        val validEmail = forgotPasswordUseCase("trader@basarat.pk")
        assertTrue(validEmail.isSuccess)
    }

    @Test
    fun testResetPasswordUseCase() = runBlocking {
        val resetPasswordUseCase = ResetPasswordUseCase(authRepository)

        val emptyToken = resetPasswordUseCase("", "NewPassword123!")
        assertTrue(emptyToken.isFailure)

        val shortPassword = resetPasswordUseCase("token_123", "short")
        assertTrue(shortPassword.isFailure)

        val validReset = resetPasswordUseCase("token_123", "NewPassword123!")
        assertTrue(validReset.isSuccess)
    }

    @Test
    fun testChangePasswordUseCase() = runBlocking {
        val changePasswordUseCase = ChangePasswordUseCase(authRepository)

        val emptyCurrent = changePasswordUseCase("", "NewPassword123!")
        assertTrue(emptyCurrent.isFailure)

        val shortNew = changePasswordUseCase("CurrentPassword123!", "short")
        assertTrue(shortNew.isFailure)

        val validChange = changePasswordUseCase("CurrentPassword123!", "NewPassword123!")
        assertTrue(validChange.isSuccess)
    }

    @Test
    fun testPreBuiltAuthPayloads() {
        assertNotNull(AuthPayloads.VALID_LOGIN)
        assertEquals(AuthPayloads.DEFAULT_EMAIL, AuthPayloads.VALID_LOGIN.email)
        assertEquals(AuthPayloads.DEFAULT_PASSWORD, AuthPayloads.VALID_LOGIN.password)

        val signupPayload = AuthPayloads.signupPayload(email = "test@custom.com", password = "CustomPassword123!")
        assertEquals("test@custom.com", signupPayload.email)
        assertEquals("CustomPassword123!", signupPayload.password)

        assertEquals("short", AuthPayloads.INVALID_SHORT_PASSWORD_SIGNUP.password)
        assertTrue(AuthPayloads.INVALID_SHORT_PASSWORD_SIGNUP.password.length < 8)
    }
}
