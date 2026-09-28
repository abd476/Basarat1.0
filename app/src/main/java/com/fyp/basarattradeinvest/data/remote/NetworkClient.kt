package com.fyp.basarattradeinvest.data.remote

import com.fyp.basarattradeinvest.core.network.AuthInterceptor
import com.fyp.basarattradeinvest.core.network.TokenAuthenticator
import com.fyp.basarattradeinvest.data.remote.api.AuthApiService
import com.fyp.basarattradeinvest.data.remote.api.MarketApiService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    // Live backend host (AWS EC2 FastAPI backend)
    const val BASE_URL = "http://16.16.26.247:8000/"

    // Live PSX Market API host (Real-time Market quotes, gainers, losers, volume spikes, indices, sectors)
    const val MARKET_BASE_URL = "http://16.16.26.247:8000/"

    // Sanitized logging interceptor: redacts sensitive Authorization headers & tokens
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }

    // BODY logging reads the complete response body before returning it, which defeats SSE
    // streaming. Keep stream logging to headers and allow more idle time for model generation.
    private val streamingLoggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }

    private val streamAwareLoggingInterceptor = Interceptor { chain ->
        val request = chain.request()
        if (request.url.encodedPath == "/api/v1/assistant/chat/stream") {
            streamingLoggingInterceptor.intercept(chain.withReadTimeout(2, TimeUnit.MINUTES))
        } else {
            loggingInterceptor.intercept(chain)
        }
    }

    private val authInterceptor = AuthInterceptor()
    private val tokenAuthenticator = TokenAuthenticator(BASE_URL)

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(streamAwareLoggingInterceptor)
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

    private val marketRetrofit = Retrofit.Builder()
        .baseUrl(MARKET_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val marketApiService: MarketApiService by lazy {
        marketRetrofit.create(MarketApiService::class.java)
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val usersApiService: com.fyp.basarattradeinvest.data.remote.api.UsersApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.UsersApiService::class.java)
    }

    val forecastApiService: com.fyp.basarattradeinvest.data.remote.api.ForecastApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.ForecastApiService::class.java)
    }

    val stocksApiService: com.fyp.basarattradeinvest.data.remote.api.StocksApiService by lazy {
        marketRetrofit.create(com.fyp.basarattradeinvest.data.remote.api.StocksApiService::class.java)
    }

    val recommendationsApiService: com.fyp.basarattradeinvest.data.remote.api.RecommendationsApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.RecommendationsApiService::class.java)
    }

    val portfolioApiService: com.fyp.basarattradeinvest.data.remote.api.PortfolioApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.PortfolioApiService::class.java)
    }

    val riskApiService: com.fyp.basarattradeinvest.data.remote.api.RiskApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.RiskApiService::class.java)
    }

    val sentimentApiService: com.fyp.basarattradeinvest.data.remote.api.SentimentApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.SentimentApiService::class.java)
    }

    val newsApiService: com.fyp.basarattradeinvest.data.remote.api.NewsApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.NewsApiService::class.java)
    }

    val alertsApiService: com.fyp.basarattradeinvest.data.remote.api.AlertsApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.AlertsApiService::class.java)
    }

    val notificationsApiService: com.fyp.basarattradeinvest.data.remote.api.NotificationsApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.NotificationsApiService::class.java)
    }

    val shariahApiService: com.fyp.basarattradeinvest.data.remote.api.ShariahApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.ShariahApiService::class.java)
    }

    val communityApiService: com.fyp.basarattradeinvest.data.remote.api.CommunityApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.CommunityApiService::class.java)
    }

    val assistantApiService: com.fyp.basarattradeinvest.data.remote.api.AssistantApiService by lazy {
        retrofit.create(com.fyp.basarattradeinvest.data.remote.api.AssistantApiService::class.java)
    }
}
