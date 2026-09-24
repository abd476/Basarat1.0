package com.fyp.basarattradeinvest.recommendations

import com.fyp.basarattradeinvest.data.mapper.RecommendationsDtoMapper
import com.fyp.basarattradeinvest.data.remote.api.RecommendationsApiService
import com.fyp.basarattradeinvest.data.remote.dto.recommendations.*
import com.fyp.basarattradeinvest.data.repository.RecommendationsRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.recommendations.RecommendationSignal
import com.fyp.basarattradeinvest.domain.usecase.recommendations.GetRecommendationsUseCase
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class RecommendationsModuleUnitTest {

    private val gson = Gson()

    @Test
    fun testRecommendationsListDto_deserializationAndMapping() {
        val json = """
            {
                "count": 2,
                "risk_profile": "moderate",
                "recommendations": [
                    {
                        "symbol": "OGDC",
                        "signal": "buy",
                        "confidence": 0.82,
                        "composite_score": 0.45,
                        "target_price": 168.0,
                        "stop_loss": 148.5,
                        "summary": "Strong buy: ML+Technical agree bullish"
                    },
                    {
                        "symbol": "SYS",
                        "signal": "hold",
                        "confidence": 0.65,
                        "composite_score": 0.08,
                        "target_price": 460.0,
                        "stop_loss": 425.0,
                        "summary": "Hold: Consolidating near resistance"
                    }
                ]
            }
        """.trimIndent()

        val dto = gson.fromJson(json, RecommendationsListResponseDto::class.java)
        assertEquals(2, dto.count)
        assertEquals("moderate", dto.riskProfile)
        assertEquals("OGDC", dto.recommendations[0].symbol)

        val item = RecommendationsDtoMapper.mapRecommendationItem(dto.recommendations[0])
        assertEquals("OGDC", item.symbol)
        assertEquals(RecommendationSignal.BUY, item.signal)
        assertEquals(0.82, item.confidence, 0.001)
        assertEquals(0.45, item.compositeScore, 0.001)
        assertEquals(168.0, item.targetPrice!!, 0.001)
        assertEquals(148.5, item.stopLoss!!, 0.001)
        assertEquals("Strong buy: ML+Technical agree bullish", item.summary)
    }

    @Test
    fun testRecommendationDetailDto_deserializationAndMapping() {
        val json = """
            {
                "symbol": "LUCK",
                "signal": "buy",
                "confidence": 0.76,
                "composite_score": 0.38,
                "signals": {
                    "ml": 0.45,
                    "technical": 0.38,
                    "fundamental": 0.30
                },
                "target_price": 960.0,
                "stop_loss": 880.0,
                "expected_range": {
                    "low": 860.0,
                    "high": 980.0,
                    "method": "atr_band"
                },
                "current_price": 912.0,
                "atr_14": 22.5,
                "reasoning": {
                    "technical": {
                        "rsi": "RSI=42.5",
                        "macd": "MACD bullish crossover"
                    }
                },
                "weights": {
                    "gru": 0.33,
                    "technical": 0.33,
                    "fundamental": 0.34
                }
            }
        """.trimIndent()

        val dto = gson.fromJson(json, RecommendationDetailResponseDto::class.java)
        assertEquals("LUCK", dto.symbol)

        val detail = RecommendationsDtoMapper.mapRecommendationDetail(dto)
        assertEquals("LUCK", detail.symbol)
        assertEquals(RecommendationSignal.BUY, detail.signal)
        assertEquals(0.76, detail.confidence, 0.001)
        assertEquals(960.0, detail.targetPrice!!, 0.001)
        assertEquals(880.0, detail.stopLoss!!, 0.001)
        assertEquals(912.0, detail.currentPrice!!, 0.001)
        assertEquals(22.5, detail.atr14!!, 0.001)
        assertEquals(860.0, detail.expectedRange!!.low, 0.001)
        assertEquals(980.0, detail.expectedRange!!.high, 0.001)
        assertEquals(0.45, detail.individualSignals["ml"]!!, 0.001)
    }

    @Test
    fun testTargetStopDto_deserializationAndMapping() {
        val json = """
            {
                "symbol": "MEBL",
                "current_price": 245.5,
                "target_price": 265.0,
                "stop_loss": 238.0,
                "expected_range": {
                    "low": 235.0,
                    "high": 270.0,
                    "method": "atr_band"
                },
                "method": "atr_band",
                "atr_14": 5.2,
                "risk_tolerance": "moderate",
                "upside_pct": 7.94,
                "downside_pct": -3.05
            }
        """.trimIndent()

        val dto = gson.fromJson(json, TargetStopResponseDto::class.java)
        assertEquals("MEBL", dto.symbol)

        val targetStop = RecommendationsDtoMapper.mapTargetStop(dto)
        assertEquals("MEBL", targetStop.symbol)
        assertEquals(245.5, targetStop.currentPrice!!, 0.001)
        assertEquals(265.0, targetStop.targetPrice!!, 0.001)
        assertEquals(238.0, targetStop.stopLoss!!, 0.001)
        assertEquals(7.94, targetStop.upsidePct!!, 0.001)
        assertEquals(-3.05, targetStop.downsidePct!!, 0.001)
    }

    @Test
    fun testEngineWeightsDto_deserializationAndMapping() {
        val json = """
            {
                "gru_weight": 0.40,
                "technical_weight": 0.35,
                "fundamental_weight": 0.25
            }
        """.trimIndent()

        val dto = gson.fromJson(json, EngineWeightsResponseDto::class.java)
        assertEquals(0.40, dto.gruWeight, 0.001)

        val domain = RecommendationsDtoMapper.mapEngineWeights(dto)
        assertEquals(0.40, domain.gruWeight, 0.001)
        assertEquals(0.35, domain.technicalWeight, 0.001)
        assertEquals(0.25, domain.fundamentalWeight, 0.001)
    }

    @Test
    fun testRecommendationsRepositoryImpl_offlineFallback() = runBlocking {
        val mockApi = object : RecommendationsApiService {
            override suspend fun getRecommendations(riskProfile: String?, sector: String?, limit: Int): Response<RecommendationsListResponseDto> {
                return Response.error(500, "Server Error".toResponseBody())
            }

            override suspend fun getEngineWeights(): Response<EngineWeightsResponseDto> {
                return Response.error(500, "Server Error".toResponseBody())
            }

            override suspend fun setEngineWeights(request: EngineWeightsRequestDto): Response<EngineWeightsResponseDto> {
                return Response.error(500, "Server Error".toResponseBody())
            }

            override suspend fun getRecommendationDetail(symbol: String): Response<RecommendationDetailResponseDto> {
                return Response.error(500, "Server Error".toResponseBody())
            }

            override suspend fun getTargetStop(symbol: String): Response<TargetStopResponseDto> {
                return Response.error(500, "Server Error".toResponseBody())
            }
        }

        val repository = RecommendationsRepositoryImpl(mockApi)

        val listRes = repository.getRecommendations("moderate")
        assertTrue(listRes.isSuccess)
        assertFalse(listRes.getOrThrow().isEmpty())

        val detailRes = repository.getRecommendationDetail("OGDC")
        assertTrue(detailRes.isSuccess)
        assertEquals("OGDC", detailRes.getOrThrow().symbol)

        val targetStopRes = repository.getTargetStop("OGDC")
        assertTrue(targetStopRes.isSuccess)
        assertNotNull(targetStopRes.getOrThrow().targetPrice)

        val weightsRes = repository.getEngineWeights()
        assertTrue(weightsRes.isSuccess)
        assertEquals(0.33, weightsRes.getOrThrow().gruWeight, 0.001)
    }
}
