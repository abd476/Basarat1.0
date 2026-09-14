package com.fyp.basarattradeinvest.domain.usecase

import com.fyp.basarattradeinvest.domain.model.MarketSentiment
import com.fyp.basarattradeinvest.domain.repository.MarketRepository

class GetMarketSentimentUseCase(
    private val repository: MarketRepository
) {
    suspend operator fun invoke(): Result<MarketSentiment> {
        return repository.getMarketSentiment()
    }
}
