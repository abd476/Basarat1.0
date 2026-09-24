package com.fyp.basarattradeinvest.data.mapper

import com.fyp.basarattradeinvest.data.remote.dto.forecast.*
import com.fyp.basarattradeinvest.domain.model.*

/**
 * Mapper between raw Swagger Forecast DTOs and Clean Domain models
 */
object ForecastDtoMapper {

    fun mapToDomain(dto: ForecastResponseDto): StockForecast {
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

        return StockForecast(
            symbol = dto.symbol,
            horizon = dto.horizon,
            direction = ForecastDirection.fromString(dto.direction),
            confidence = dto.confidence,
            currentPrice = dto.currentPrice,
            targetPrice = dto.targetPrice,
            targetDate = dto.targetDate,
            asOfDate = dto.asOfDate,
            stopLoss = dto.stopLoss,
            upsidePct = dto.upsidePct,
            downsidePct = dto.downsidePct,
            riskRewardRatio = dto.riskRewardRatio,
            signalRating = dto.signalRating,
            modelVersion = dto.modelVersion,
            gateReason = dto.gateReason,
            probabilities = probabilitiesDomain,
            models = modelsDomain,
            marketContext = contextDomain
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
