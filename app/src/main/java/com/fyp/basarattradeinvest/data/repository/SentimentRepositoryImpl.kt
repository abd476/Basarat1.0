package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.data.mapper.SentimentDtoMapper
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.SentimentApiService
import com.fyp.basarattradeinvest.domain.model.sentiment.MarketSentimentOverview
import com.fyp.basarattradeinvest.domain.model.sentiment.SentimentHistory
import com.fyp.basarattradeinvest.domain.model.sentiment.SentimentNewsPage
import com.fyp.basarattradeinvest.domain.model.sentiment.StockSentiment
import com.fyp.basarattradeinvest.domain.repository.SentimentRepository
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SentimentRepositoryImpl
 *
 * Concrete implementation of SentimentRepository.
 * Uses the shared NetworkClient Retrofit instance (existing auth interceptor handles tokens).
 * Error handling follows the same pattern as RiskRepositoryImpl.
 */
class SentimentRepositoryImpl(
    private val apiService: SentimentApiService = NetworkClient.sentimentApiService
) : SentimentRepository {

    // ─── Stock Sentiment ───────────────────────────────────────────────────────

    override suspend fun getStockSentiment(
        symbol: String,
        days: Int?
    ): Result<StockSentiment> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStockSentiment(symbol, days)
            if (response.isSuccessful && response.body() != null) {
                Result.success(SentimentDtoMapper.mapStockSentiment(response.body()!!))
            } else {
                val msg = parseErrorMessage(
                    response.errorBody()?.string(),
                    response.code(),
                    "Unable to load sentiment for $symbol"
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Unable to connect to sentiment service"))
        }
    }

    // ─── Sentiment History ─────────────────────────────────────────────────────

    override suspend fun getSentimentHistory(
        symbol: String,
        period: String?,
        limit: Int?
    ): Result<SentimentHistory> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSentimentHistory(symbol, period, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(SentimentDtoMapper.mapSentimentHistory(response.body()!!))
            } else {
                val msg = parseErrorMessage(
                    response.errorBody()?.string(),
                    response.code(),
                    "Unable to load sentiment history for $symbol"
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Unable to load sentiment history"))
        }
    }

    // ─── Sentiment News ────────────────────────────────────────────────────────

    override suspend fun getSentimentNews(
        symbol: String,
        page: Int,
        limit: Int,
        sentiment: String?
    ): Result<SentimentNewsPage> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getSentimentNews(symbol, page, limit, sentiment)
            if (response.isSuccessful && response.body() != null) {
                Result.success(SentimentDtoMapper.mapSentimentNewsPage(response.body()!!))
            } else {
                val msg = parseErrorMessage(
                    response.errorBody()?.string(),
                    response.code(),
                    "Unable to load news for $symbol"
                )
                Result.failure(Exception(msg))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Unable to load sentiment news"))
        }
    }

    // ─── Market Sentiment Overview ─────────────────────────────────────────────

    override suspend fun getMarketSentiment(): Result<MarketSentimentOverview> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMarketSentiment()
                if (response.isSuccessful && response.body() != null) {
                    Result.success(SentimentDtoMapper.mapMarketSentimentOverview(response.body()!!))
                } else {
                    val msg = parseErrorMessage(
                        response.errorBody()?.string(),
                        response.code(),
                        "Unable to load market sentiment"
                    )
                    Result.failure(Exception(msg))
                }
            } catch (e: Exception) {
                Result.failure(Exception(e.localizedMessage ?: "Unable to connect to sentiment service"))
            }
        }

    // ─── Error Parsing ─────────────────────────────────────────────────────────

    private fun parseErrorMessage(errorBody: String?, statusCode: Int, defaultMsg: String): String {
        if (errorBody.isNullOrBlank()) {
            return when (statusCode) {
                401 -> "Session expired. Please log in again."
                403 -> "Access denied."
                404 -> "Sentiment data not found for this symbol."
                422 -> "Invalid request parameters."
                500, 502, 503 -> "Sentiment service temporarily unavailable. Please try again."
                else -> defaultMsg
            }
        }
        return try {
            val json = Gson().fromJson(errorBody, JsonObject::class.java)
            when {
                json.has("error") -> {
                    val errorObj = json.getAsJsonObject("error")
                    errorObj.get("message")?.asString ?: defaultMsg
                }
                json.has("detail") -> json.get("detail").asString
                json.has("message") -> json.get("message").asString
                else -> defaultMsg
            }
        } catch (e: Exception) {
            defaultMsg
        }
    }
}
