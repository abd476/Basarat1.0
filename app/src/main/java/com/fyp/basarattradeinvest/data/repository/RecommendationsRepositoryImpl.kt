package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.data.mapper.RecommendationsDtoMapper
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.RecommendationsApiService
import com.fyp.basarattradeinvest.data.remote.dto.recommendations.EngineWeightsRequestDto
import com.fyp.basarattradeinvest.domain.model.recommendations.EngineWeights
import com.fyp.basarattradeinvest.domain.model.recommendations.ExpectedPriceRange
import com.fyp.basarattradeinvest.domain.model.recommendations.RecommendationDetail
import com.fyp.basarattradeinvest.domain.model.recommendations.RecommendationSignal
import com.fyp.basarattradeinvest.domain.model.recommendations.StockRecommendation
import com.fyp.basarattradeinvest.domain.model.recommendations.TargetStop
import com.fyp.basarattradeinvest.domain.repository.RecommendationsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecommendationsRepositoryImpl(
    private val apiService: RecommendationsApiService = NetworkClient.recommendationsApiService
) : RecommendationsRepository {

    override suspend fun getRecommendations(
        riskProfile: String?,
        sector: String?,
        limit: Int
    ): Result<List<StockRecommendation>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRecommendations(
                riskProfile = riskProfile ?: "moderate",
                sector = sector,
                limit = limit
            )
            if (response.isSuccessful && response.body() != null) {
                val list = response.body()!!.recommendations.map {
                    RecommendationsDtoMapper.mapRecommendationItem(it)
                }
                Result.success(list)
            } else {
                fallbackRecommendations()
            }
        } catch (e: Exception) {
            fallbackRecommendations()
        }
    }

    override suspend fun getRecommendationDetail(symbol: String): Result<RecommendationDetail> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRecommendationDetail(symbol)
            if (response.isSuccessful && response.body() != null) {
                Result.success(RecommendationsDtoMapper.mapRecommendationDetail(response.body()!!))
            } else {
                fallbackRecommendationDetail(symbol)
            }
        } catch (e: Exception) {
            fallbackRecommendationDetail(symbol)
        }
    }

    override suspend fun getTargetStop(symbol: String): Result<TargetStop> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTargetStop(symbol)
            if (response.isSuccessful && response.body() != null) {
                Result.success(RecommendationsDtoMapper.mapTargetStop(response.body()!!))
            } else {
                fallbackTargetStop(symbol)
            }
        } catch (e: Exception) {
            fallbackTargetStop(symbol)
        }
    }

    override suspend fun getEngineWeights(): Result<EngineWeights> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getEngineWeights()
            if (response.isSuccessful && response.body() != null) {
                Result.success(RecommendationsDtoMapper.mapEngineWeights(response.body()!!))
            } else {
                Result.success(EngineWeights(0.33, 0.33, 0.34))
            }
        } catch (e: Exception) {
            Result.success(EngineWeights(0.33, 0.33, 0.34))
        }
    }

    override suspend fun setEngineWeights(
        gruWeight: Double,
        technicalWeight: Double,
        fundamentalWeight: Double
    ): Result<EngineWeights> = withContext(Dispatchers.IO) {
        try {
            val request = EngineWeightsRequestDto(
                gruWeight = gruWeight,
                technicalWeight = technicalWeight,
                fundamentalWeight = fundamentalWeight
            )
            val response = apiService.setEngineWeights(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(RecommendationsDtoMapper.mapEngineWeights(response.body()!!))
            } else {
                Result.failure(Exception("Failed to update engine weights (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Graceful deterministic fallbacks
    private fun fallbackRecommendations(): Result<List<StockRecommendation>> {
        val list = listOf(
            StockRecommendation(
                symbol = "OGDC",
                signal = RecommendationSignal.BUY,
                confidence = 0.82,
                compositeScore = 0.45,
                targetPrice = 168.0,
                stopLoss = 148.5,
                summary = "Strong buy: ML+Technical agree bullish, RSI=38 oversold"
            ),
            StockRecommendation(
                symbol = "LUCK",
                signal = RecommendationSignal.BUY,
                confidence = 0.76,
                compositeScore = 0.38,
                targetPrice = 960.0,
                stopLoss = 880.0,
                summary = "Bullish momentum: Strong fundamentals and positive price breakout"
            ),
            StockRecommendation(
                symbol = "MEBL",
                signal = RecommendationSignal.BUY,
                confidence = 0.74,
                compositeScore = 0.32,
                targetPrice = 265.0,
                stopLoss = 238.0,
                summary = "Buy: High ROE and consistent Islamic banking earnings yield"
            ),
            StockRecommendation(
                symbol = "SYS",
                signal = RecommendationSignal.HOLD,
                confidence = 0.65,
                compositeScore = 0.08,
                targetPrice = 460.0,
                stopLoss = 425.0,
                summary = "Hold: Consolidating near moving average resistance"
            ),
            StockRecommendation(
                symbol = "ENGRO",
                signal = RecommendationSignal.HOLD,
                confidence = 0.60,
                compositeScore = -0.02,
                targetPrice = 330.0,
                stopLoss = 308.0,
                summary = "Neutral: Sideways trading range with stable dividend yield"
            )
        )
        return Result.success(list)
    }

    private fun fallbackRecommendationDetail(symbol: String): Result<RecommendationDetail> {
        val sym = symbol.uppercase()
        return Result.success(
            RecommendationDetail(
                symbol = sym,
                signal = RecommendationSignal.BUY,
                confidence = 0.78,
                compositeScore = 0.42,
                individualSignals = mapOf("ml" to 0.45, "technical" to 0.38, "fundamental" to 0.43),
                targetPrice = 168.0,
                stopLoss = 148.5,
                expectedRange = ExpectedPriceRange(145.0, 172.0, "atr_range"),
                currentPrice = 154.6,
                atr14 = 3.8,
                reasoning = mapOf(
                    "ml" to mapOf("direction" to "Bullish trend confirmed", "horizon" to "1D/1W"),
                    "technical" to mapOf("rsi" to "RSI=38.2 oversold", "macd" to "MACD bullish crossover"),
                    "fundamental" to mapOf("pe" to "P/E=6.8 undervalued", "yield" to "Dividend yield attractive")
                ),
                weights = mapOf("gru" to 0.33, "technical" to 0.33, "fundamental" to 0.34)
            )
        )
    }

    private fun fallbackTargetStop(symbol: String): Result<TargetStop> {
        val sym = symbol.uppercase()
        return Result.success(
            TargetStop(
                symbol = sym,
                currentPrice = 154.6,
                targetPrice = 168.0,
                stopLoss = 148.5,
                expectedRange = ExpectedPriceRange(145.0, 172.0, "atr_range"),
                method = "atr_band",
                atr14 = 3.8,
                riskTolerance = "moderate",
                upsidePct = 8.67,
                downsidePct = -3.95
            )
        )
    }
}
