package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.recommendations.EngineWeights
import com.fyp.basarattradeinvest.domain.model.recommendations.RecommendationDetail
import com.fyp.basarattradeinvest.domain.model.recommendations.StockRecommendation
import com.fyp.basarattradeinvest.domain.model.recommendations.TargetStop

interface RecommendationsRepository {
    suspend fun getRecommendations(riskProfile: String? = "moderate", sector: String? = null, limit: Int = 20): Result<List<StockRecommendation>>
    suspend fun getRecommendationDetail(symbol: String): Result<RecommendationDetail>
    suspend fun getTargetStop(symbol: String): Result<TargetStop>
    suspend fun getEngineWeights(): Result<EngineWeights>
    suspend fun setEngineWeights(gruWeight: Double, technicalWeight: Double, fundamentalWeight: Double): Result<EngineWeights>
}
