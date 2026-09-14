package com.fyp.basarattradeinvest.domain.usecase

import com.fyp.basarattradeinvest.domain.model.MarketOverview
import com.fyp.basarattradeinvest.domain.repository.MarketRepository

class GetMarketOverviewUseCase(
    private val repository: MarketRepository
) {
    suspend operator fun invoke(): Result<MarketOverview> {
        return repository.getMarketOverview()
    }
}
