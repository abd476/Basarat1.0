package com.fyp.basarattradeinvest.data.mapper

import com.fyp.basarattradeinvest.data.remote.dto.forecast.*
import com.fyp.basarattradeinvest.domain.model.*

/**
 * Mapper between raw Swagger Forecast DTOs and Clean Domain models
 */
object ForecastDtoMapper {

    fun mapToDomain(dto: ForecastResponseDto, fallbackPrice: Double? = null): StockForecast {
        val probabilitiesDomain = dto.probabilities?.let { p ->
            ForecastProbabilities(
                bullish = p.bullish ?: 0.0,
                bearish = p.bearish ?: 0.0,
                sideways = p.sideways ?: 0.0
            )
        }

        val modelsDomain = dto.models?.mapValues { (_, modelDetail) ->
            ForecastModelDetail(
                direction = modelDetail.direction ?: "unknown",
                bullishPct = modelDetail.bullishPct ?: 0.0,
                bearishPct = modelDetail.bearishPct ?: 0.0,
                sidewaysPct = modelDetail.sidewaysPct ?: 0.0,
                gapPp = modelDetail.gapPp ?: 0.0
            )
        } ?: emptyMap()

        val contextDomain = dto.marketContext?.let { mc ->
            ForecastMarketContext(
                marketReturn5d = mc.marketReturn5d,
                marketReturn20d = mc.marketReturn20d,
                stockReturn20d = mc.stockReturn20d,
                stockRelativeReturn20d = mc.stockRelativeReturn20d
            )
        }

        val directionEnum = ForecastDirection.fromString(dto.direction)
        val resolvedPrice = dto.currentPrice?.takeIf { it > 0.0 } ?: fallbackPrice?.takeIf { it > 0.0 }

        val targetParams = resolveTargets(
            currentPrice = resolvedPrice,
            direction = directionEnum,
            horizon = dto.horizon,
            confidence = dto.confidence,
            bullishProb = probabilitiesDomain?.bullish ?: 50.0,
            bearishProb = probabilitiesDomain?.bearish ?: 50.0,
            apiTarget = dto.targetPrice,
            apiStop = dto.stopLoss,
            apiUpside = dto.upsidePct,
            apiDownside = dto.downsidePct,
            apiRr = dto.riskRewardRatio
        )

        return StockForecast(
            symbol = dto.symbol,
            horizon = dto.horizon,
            direction = directionEnum,
            confidence = dto.confidence,
            currentPrice = resolvedPrice,
            targetPrice = targetParams.targetPrice,
            targetDate = dto.targetDate,
            asOfDate = dto.asOfDate,
            stopLoss = targetParams.stopLoss,
            upsidePct = targetParams.upsidePct,
            downsidePct = targetParams.downsidePct,
            riskRewardRatio = targetParams.riskRewardRatio,
            signalRating = dto.signalRating,
            modelVersion = dto.modelVersion,
            gateReason = dto.gateReason,
            probabilities = probabilitiesDomain,
            models = modelsDomain,
            marketContext = contextDomain
        )
    }

    /**
     * Re-anchors target price, stop loss, and risk/reward parameters when live stock price
     * becomes available from the market quote or parent screen analysis.
     */
    fun recalculateTargets(forecast: StockForecast, livePrice: Double?): StockForecast {
        val resolvedPrice = forecast.currentPrice?.takeIf { it > 0.0 } ?: livePrice?.takeIf { it > 0.0 }
        val targetParams = resolveTargets(
            currentPrice = resolvedPrice,
            direction = forecast.direction,
            horizon = forecast.horizon,
            confidence = forecast.confidence,
            bullishProb = forecast.probabilities?.bullish ?: 50.0,
            bearishProb = forecast.probabilities?.bearish ?: 50.0,
            apiTarget = forecast.targetPrice,
            apiStop = forecast.stopLoss,
            apiUpside = forecast.upsidePct,
            apiDownside = forecast.downsidePct,
            apiRr = forecast.riskRewardRatio
        )
        return forecast.copy(
            currentPrice = resolvedPrice,
            targetPrice = targetParams.targetPrice,
            stopLoss = targetParams.stopLoss,
            upsidePct = targetParams.upsidePct,
            downsidePct = targetParams.downsidePct,
            riskRewardRatio = targetParams.riskRewardRatio
        )
    }

    data class TargetParameters(
        val targetPrice: Double?,
        val stopLoss: Double?,
        val upsidePct: Double?,
        val downsidePct: Double?,
        val riskRewardRatio: Double?
    )

    private fun resolveTargets(
        currentPrice: Double?,
        direction: ForecastDirection,
        horizon: String,
        confidence: Double,
        bullishProb: Double,
        bearishProb: Double,
        apiTarget: Double?,
        apiStop: Double?,
        apiUpside: Double?,
        apiDownside: Double?,
        apiRr: Double?
    ): TargetParameters {
        // If API already provided valid target and stop loss, preserve them and compute missing ratios
        if (apiTarget != null && apiTarget > 0.0 && apiStop != null && apiStop > 0.0 && currentPrice != null && currentPrice > 0.0) {
            val upPct = apiUpside ?: (Math.round(((apiTarget - currentPrice) / currentPrice) * 1000.0) / 10.0)
            val downPct = apiDownside ?: (Math.round(((apiStop - currentPrice) / currentPrice) * 1000.0) / 10.0)
            val rr = apiRr ?: if (downPct != 0.0) Math.round(Math.abs(upPct / downPct) * 100.0) / 100.0 else 1.50
            return TargetParameters(
                targetPrice = apiTarget,
                stopLoss = apiStop,
                upsidePct = upPct,
                downsidePct = downPct,
                riskRewardRatio = rr
            )
        }

        // If no price reference exists anywhere, return whatever API passed
        if (currentPrice == null || currentPrice <= 0.0) {
            return TargetParameters(
                targetPrice = apiTarget,
                stopLoss = apiStop,
                upsidePct = apiUpside,
                downsidePct = apiDownside,
                riskRewardRatio = apiRr
            )
        }

        val normHorizon = horizon.uppercase().trim()
        val confFactor = ((confidence.coerceIn(0.40, 0.95) - 0.40) / 0.55).coerceIn(0.0, 1.0)

        // Direction & horizon-based financial volatility boundaries
        val (baseUpPct, baseDownPct) = when (direction) {
            ForecastDirection.BULLISH -> {
                when (normHorizon) {
                    "1D" -> Pair(2.4 + 1.2 * confFactor, -1.4 - 0.3 * (1.0 - confFactor))
                    "1W" -> Pair(5.2 + 2.4 * confFactor, -2.8 - 0.7 * (1.0 - confFactor))
                    else -> Pair(9.8 + 4.2 * confFactor, -5.0 - 1.5 * (1.0 - confFactor)) // 1M
                }
            }
            ForecastDirection.BEARISH -> {
                when (normHorizon) {
                    "1D" -> Pair(-2.4 - 1.2 * confFactor, 1.4 + 0.3 * (1.0 - confFactor))
                    "1W" -> Pair(-5.2 - 2.4 * confFactor, 2.8 + 0.7 * (1.0 - confFactor))
                    else -> Pair(-9.8 - 4.2 * confFactor, 5.0 + 1.5 * (1.0 - confFactor)) // 1M
                }
            }
            ForecastDirection.SIDEWAYS -> {
                // Sideways channel trading: upper resistance target, lower support stop loss
                val bullTilt = if (bullishProb + bearishProb > 0.0) {
                    ((bullishProb - bearishProb) / (bullishProb + bearishProb)).coerceIn(-0.4, 0.4)
                } else 0.0

                when (normHorizon) {
                    "1D" -> Pair(1.9 + bullTilt * 1.5, -1.4 + bullTilt * 0.4)
                    "1W" -> Pair(4.0 + bullTilt * 2.5, -2.7 + bullTilt * 0.6)
                    else -> Pair(7.5 + bullTilt * 4.0, -4.8 + bullTilt * 1.0) // 1M
                }
            }
        }

        val roundedUpPct = apiUpside ?: (Math.round(baseUpPct * 10.0) / 10.0)
        val roundedDownPct = apiDownside ?: (Math.round(baseDownPct * 10.0) / 10.0)

        val target = apiTarget?.takeIf { it > 0.0 }
            ?: (Math.round(currentPrice * (1.0 + roundedUpPct / 100.0) * 100.0) / 100.0)

        val stop = apiStop?.takeIf { it > 0.0 }
            ?: (Math.round(currentPrice * (1.0 + roundedDownPct / 100.0) * 100.0) / 100.0)

        val rr = apiRr ?: if (roundedDownPct != 0.0) {
            Math.round(Math.abs(roundedUpPct / roundedDownPct) * 100.0) / 100.0
        } else 1.50

        return TargetParameters(
            targetPrice = target,
            stopLoss = stop,
            upsidePct = roundedUpPct,
            downsidePct = roundedDownPct,
            riskRewardRatio = rr
        )
    }

    fun mapToDomain(dto: ForecastHistoryResponseDto): ForecastHistory {
        val records = dto.history.map { item ->
            val actualDomain = item.actual?.let { act ->
                ForecastActualOutcome(
                    direction = act.direction ?: "unknown",
                    wasCorrect = act.wasCorrect ?: false
                )
            }

            ForecastHistoryRecord(
                predictedAt = item.predictedAt,
                predictedDirection = ForecastDirection.fromString(item.predictedDirection),
                probabilities = item.probabilities ?: emptyMap(),
                confidence = item.confidence,
                targetDate = item.targetDate,
                actualOutcome = actualDomain
            )
        }

        return ForecastHistory(
            symbol = dto.symbol,
            horizon = dto.horizon ?: "1D",
            count = dto.count,
            accuracy = dto.accuracy,
            history = records
        )
    }
}
