package com.fyp.basarattradeinvest.data.mapper

import com.fyp.basarattradeinvest.data.remote.dto.recommendations.EngineWeightsResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.recommendations.RecommendationDetailResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.recommendations.RecommendationItemDto
import com.fyp.basarattradeinvest.data.remote.dto.recommendations.TargetStopResponseDto
import com.fyp.basarattradeinvest.domain.model.recommendations.EngineWeights
import com.fyp.basarattradeinvest.domain.model.recommendations.ExpectedPriceRange
import com.fyp.basarattradeinvest.domain.model.recommendations.RecommendationDetail
import com.fyp.basarattradeinvest.domain.model.recommendations.RecommendationSignal
import com.fyp.basarattradeinvest.domain.model.recommendations.StockRecommendation
import com.fyp.basarattradeinvest.domain.model.recommendations.TargetStop

object RecommendationsDtoMapper {

    fun mapRecommendationItem(dto: RecommendationItemDto): StockRecommendation {
        return StockRecommendation(
            symbol = dto.symbol.uppercase(),
            signal = RecommendationSignal.fromString(dto.signal),
            confidence = dto.confidence,
            compositeScore = dto.compositeScore,
            targetPrice = dto.targetPrice,
            stopLoss = dto.stopLoss,
            summary = dto.summary
        )
    }

    fun mapRecommendationDetail(dto: RecommendationDetailResponseDto): RecommendationDetail {
        val range = dto.expectedRange?.let {
            val low = (it["low"] as? Number)?.toDouble() ?: 0.0
            val high = (it["high"] as? Number)?.toDouble() ?: 0.0
            val method = it["method"]?.toString() ?: "atr_range"
            ExpectedPriceRange(low = low, high = high, method = method)
        }

        return RecommendationDetail(
            symbol = dto.symbol.uppercase(),
            signal = RecommendationSignal.fromString(dto.signal),
            confidence = dto.confidence,
            compositeScore = dto.compositeScore,
            individualSignals = dto.signals,
            targetPrice = dto.targetPrice,
            stopLoss = dto.stopLoss,
            expectedRange = range,
            currentPrice = dto.currentPrice,
            atr14 = dto.atr14,
            reasoning = dto.reasoning,
            weights = dto.weights
        )
    }

    fun mapTargetStop(dto: TargetStopResponseDto): TargetStop {
        val range = dto.expectedRange?.let {
            val low = (it["low"] as? Number)?.toDouble() ?: 0.0
            val high = (it["high"] as? Number)?.toDouble() ?: 0.0
            val method = it["method"]?.toString() ?: "atr_range"
            ExpectedPriceRange(low = low, high = high, method = method)
        }

        return TargetStop(
            symbol = dto.symbol.uppercase(),
            currentPrice = dto.currentPrice,
            targetPrice = dto.targetPrice,
            stopLoss = dto.stopLoss,
            expectedRange = range,
            method = dto.method ?: "atr_band",
            atr14 = dto.atr14,
            riskTolerance = dto.riskTolerance ?: "moderate",
            upsidePct = dto.upsidePct,
            downsidePct = dto.downsidePct
        )
    }

    fun mapEngineWeights(dto: EngineWeightsResponseDto): EngineWeights {
        return EngineWeights(
            gruWeight = dto.gruWeight,
            technicalWeight = dto.technicalWeight,
            fundamentalWeight = dto.fundamentalWeight
        )
    }
}
