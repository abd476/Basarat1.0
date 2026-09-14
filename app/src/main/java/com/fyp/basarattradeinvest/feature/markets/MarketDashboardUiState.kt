package com.fyp.basarattradeinvest.feature.markets

import com.fyp.basarattradeinvest.domain.model.MarketIndex
import com.fyp.basarattradeinvest.domain.model.MarketOverview
import com.fyp.basarattradeinvest.domain.model.MarketSentiment
import com.fyp.basarattradeinvest.domain.model.MarketStock
import com.fyp.basarattradeinvest.domain.model.SectorPerformance
import com.fyp.basarattradeinvest.domain.model.SymbolFilterType

sealed interface MarketDashboardUiState {
    data object Loading : MarketDashboardUiState

    data class Success(
        val indices: List<MarketIndex>,
        val overview: MarketOverview,
        val topSymbols: List<MarketStock>,
        val selectedFilter: SymbolFilterType,
        val sentiment: MarketSentiment,
        val sectors: List<SectorPerformance>,
        val lastUpdated: String
    ) : MarketDashboardUiState

    data object Empty : MarketDashboardUiState

    data class Error(val message: String) : MarketDashboardUiState
}
