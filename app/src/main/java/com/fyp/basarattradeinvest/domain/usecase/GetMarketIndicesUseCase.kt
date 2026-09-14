package com.fyp.basarattradeinvest.domain.usecase

import com.fyp.basarattradeinvest.domain.model.MarketIndex
import com.fyp.basarattradeinvest.domain.repository.MarketRepository

class GetMarketIndicesUseCase(
    private val repository: MarketRepository
) {
    suspend operator fun invoke(): Result<List<MarketIndex>> {
        return repository.getMarketIndices()
    }
}
