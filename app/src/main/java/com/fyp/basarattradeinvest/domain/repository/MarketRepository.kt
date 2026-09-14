package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.MarketIndex
import com.fyp.basarattradeinvest.domain.model.MarketOverview
import com.fyp.basarattradeinvest.domain.model.MarketSentiment
import com.fyp.basarattradeinvest.domain.model.MarketStock
import com.fyp.basarattradeinvest.domain.model.SectorPerformance
import com.fyp.basarattradeinvest.domain.model.SymbolFilterType

interface MarketRepository {
    suspend fun getMarketIndices(): Result<List<MarketIndex>>
    suspend fun getMarketOverview(): Result<MarketOverview>
    suspend fun getTopSymbols(filterType: SymbolFilterType): Result<List<MarketStock>>
    suspend fun getMarketSentiment(): Result<MarketSentiment>
    suspend fun getSectorPerformances(): Result<List<SectorPerformance>>
}
