package com.fyp.basarattradeinvest.feature.markets

import com.fyp.basarattradeinvest.domain.model.market.MarketIndex
import com.fyp.basarattradeinvest.domain.model.market.IndexConstituent
import com.fyp.basarattradeinvest.domain.model.market.MarketOverview
import com.fyp.basarattradeinvest.domain.model.sentiment.MarketSentiment
import com.fyp.basarattradeinvest.domain.model.market.MarketStock
import com.fyp.basarattradeinvest.domain.model.market.SectorPerformance
import com.fyp.basarattradeinvest.domain.model.stock.SymbolFilterType
import com.fyp.basarattradeinvest.domain.model.live.LiveQuote
import com.fyp.basarattradeinvest.domain.model.live.MarketSessionStatus
import com.fyp.basarattradeinvest.domain.model.live.WsConnectionState

sealed interface MarketDashboardUiState {
    data object Loading : MarketDashboardUiState

    data class Success(
        val indices: List<MarketIndex>,
        val overview: MarketOverview,
        val topSymbols: List<MarketStock>,
        val selectedFilter: SymbolFilterType,
        val sentiment: MarketSentiment,
        val sectors: List<SectorPerformance>,
        val selectedConstituentIndex: String = "KSE-100",
        val constituents: List<com.fyp.basarattradeinvest.domain.model.market.IndexConstituent> = emptyList(),
        val isConstituentsLoading: Boolean = false,
        val lastUpdated: String,
        val liveQuotes: Map<String, LiveQuote> = emptyMap(),
        val wsConnectionState: WsConnectionState = WsConnectionState.Idle,
        /** Null when the backend did not report a session; the header hides instead of guessing. */
        val marketSession: MarketSessionStatus? = null
    ) : MarketDashboardUiState

    data object Empty : MarketDashboardUiState

    data class Error(val message: String) : MarketDashboardUiState
}
