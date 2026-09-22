package com.fyp.basarattradeinvest.data.remote

import com.fyp.basarattradeinvest.core.network.AuthInterceptor
import com.fyp.basarattradeinvest.core.network.TokenAuthenticator
import com.fyp.basarattradeinvest.data.remote.api.AuthApiService
import com.fyp.basarattradeinvest.data.remote.api.MarketApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    // Live backend OpenAPI source of truth
    const val BASE_URL = "https://basarat-fyp-official.onrender.com/"

    // Sanitized logging interceptor: redacts sensitive Authorization headers & tokens
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }

    private val authInterceptor = AuthInterceptor()
    private val tokenAuthenticator = TokenAuthenticator(BASE_URL)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .authenticator(tokenAuthenticator)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val marketApiService: MarketApiService by lazy {
        retrofit.create(MarketApiService::class.java)
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val usersApiService: com.fyp.basarattradeinvest.data.remote.api.UsersApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.UsersApiService::class.java)
    }
}
