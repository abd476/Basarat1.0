package com.fyp.basarattradeinvest.domain.usecase.recommendations

import com.fyp.basarattradeinvest.domain.model.recommendations.RecommendationDetail
import com.fyp.basarattradeinvest.domain.repository.RecommendationsRepository

class GetRecommendationDetailUseCase(
    private val repository: RecommendationsRepository
) {
    suspend operator fun invoke(symbol: String): Result<RecommendationDetail> {
        return repository.getRecommendationDetail(symbol)
    }
}
