package com.fyp.basarattradeinvest.core.network

import com.fyp.basarattradeinvest.core.session.SessionManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit

class TokenAuthenticator(
    private val baseUrl: String
) : Authenticator {

    private val gson = Gson()
    private val refreshClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    companion object {
        private val lock = Any()
    }

    override fun authenticate(route: Route?, response: Response): Request? {
        val requestUrl = response.request.url.encodedPath

        // Never attempt to authenticate auth requests to prevent infinite recursion
        if (requestUrl.endsWith("/api/v1/auth/refresh") ||
            requestUrl.endsWith("/api/v1/auth/login") ||
            requestUrl.endsWith("/api/v1/auth/logout")
        ) {
            return null
        }

        // Limit retry attempts to avoid infinite loops on broken endpoints
        if (responseCount(response) >= 2) {
            SessionManager.notifySessionExpired()
            return null
        }

        synchronized(lock) {
            val currentToken = SessionManager.getAccessToken()
            val requestToken = response.request.header("Authorization")
                ?.removePrefix("Bearer ")
                ?.trim()

            // If another thread already refreshed the token, retry with the updated token
            if (!currentToken.isNullOrBlank() && currentToken != requestToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentToken")
                    .build()
            }

            val refreshToken = SessionManager.getRefreshToken()
            if (refreshToken.isNullOrBlank()) {
                SessionManager.notifySessionExpired()
                return null
            }

            // Perform synchronous refresh call
            val refreshSuccess = executeRefresh(refreshToken)
            return if (refreshSuccess) {
                val newAccessToken = SessionManager.getAccessToken()
                if (!newAccessToken.isNullOrBlank()) {
                    response.request.newBuilder()
                        .header("Authorization", "Bearer $newAccessToken")
                        .build()
                } else {
                    null
                }
            } else {
                SessionManager.notifySessionExpired()
                null
            }
        }
    }

    private fun executeRefresh(refreshToken: String): Boolean {
        return try {
            val jsonPayload = JsonObject().apply {
                addProperty("refresh_token", refreshToken)
            }.toString()

            val requestBody = jsonPayload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val cleanBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val refreshUrl = "${cleanBaseUrl}api/v1/auth/refresh"

            val refreshRequest = Request.Builder()
                .url(refreshUrl)
                .post(requestBody)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build()

            val refreshResponse = refreshClient.newCall(refreshRequest).execute()
            if (refreshResponse.isSuccessful) {
                val bodyString = refreshResponse.body?.string()
                if (!bodyString.isNullOrBlank()) {
                    val json = gson.fromJson(bodyString, JsonObject::class.java)
                    val newAccessToken = json.get("access_token")?.asString
                    val newRefreshToken = json.get("refresh_token")?.asString ?: refreshToken

                    if (!newAccessToken.isNullOrBlank()) {
                        SessionManager.updateTokens(newAccessToken, newRefreshToken)
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
