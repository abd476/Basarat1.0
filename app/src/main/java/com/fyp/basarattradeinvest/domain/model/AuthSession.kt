package com.fyp.basarattradeinvest.domain.model

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val user: User? = null
)
