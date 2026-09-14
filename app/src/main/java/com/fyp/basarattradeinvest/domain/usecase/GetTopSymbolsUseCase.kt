package com.fyp.basarattradeinvest.domain.usecase

import com.fyp.basarattradeinvest.domain.model.MarketStock
import com.fyp.basarattradeinvest.domain.model.SymbolFilterType
import com.fyp.basarattradeinvest.domain.repository.MarketRepository

class GetTopSymbolsUseCase(
    private val repository: MarketRepository
) {
    suspend operator fun invoke(filterType: SymbolFilterType): Result<List<MarketStock>> {
        return repository.getTopSymbols(filterType)
    }
}
