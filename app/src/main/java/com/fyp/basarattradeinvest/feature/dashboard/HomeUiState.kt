package com.fyp.basarattradeinvest.feature.dashboard

import com.fyp.basarattradeinvest.domain.model.AlertItem
import com.fyp.basarattradeinvest.domain.model.MarketIndex
import com.fyp.basarattradeinvest.domain.model.MarketStock
import com.fyp.basarattradeinvest.domain.model.NewsArticle
import com.fyp.basarattradeinvest.domain.model.live.LiveQuote
import com.fyp.basarattradeinvest.domain.model.live.WsConnectionState
import com.fyp.basarattradeinvest.domain.model.portfolio.HoldingItem
import com.fyp.basarattradeinvest.domain.model.portfolio.PerformancePoint
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioAllocation
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioOverview
import com.fyp.basarattradeinvest.domain.model.risk.PortfolioVaR
import com.fyp.basarattradeinvest.domain.model.watchlist.Watchlist

enum class MoversTab {
    GAINERS,
    LOSERS,
    MOST_ACTIVE
}

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Success(
        val userName: String,
        val marketStatus: String = "Closed",
        val isMarketOpen: Boolean = false,
        val marketIndices: List<MarketIndex> = emptyList(),
        val selectedBenchmarkIndex: MarketIndex? = null,
        val selectedTimeframe: String = "1D",
        val watchlist: WatchlistUiState = WatchlistUiState.Loading,
        val topGainers: List<MarketStock> = emptyList(),
        val topLosers: List<MarketStock> = emptyList(),
        val volumeSpikes: List<MarketStock> = emptyList(),
        val selectedMoversTab: MoversTab = MoversTab.GAINERS,
        val recentNews: List<NewsArticle> = emptyList(),
        val activeAlertsCount: Int = 0,
        val unresolvedAlerts: List<AlertItem> = emptyList(),
        // Optional compatibility fields with defaults
        val subAccount: String = "04182-PSX",
        val isCdcActive: Boolean = true,
        val portfolioOverview: PortfolioOverview? = null,
        val performancePoints: List<PerformancePoint> = emptyList(),
        val selectedPeriod: String = "1M",
        val allocation: PortfolioAllocation? = null,
        val topHoldings: List<HoldingItem> = emptyList(),
        val riskSummary: PortfolioVaR? = null,
        val liveQuotes: Map<String, LiveQuote> = emptyMap(),
        val wsConnectionState: WsConnectionState = WsConnectionState.Idle
    ) : HomeUiState {
        val currentMovers: List<MarketStock>
            get() = when (selectedMoversTab) {
                MoversTab.GAINERS -> topGainers
                MoversTab.LOSERS -> topLosers
                MoversTab.MOST_ACTIVE -> volumeSpikes
            }
    }

    data class Error(val message: String) : HomeUiState
}

sealed interface WatchlistUiState {
    data object Loading : WatchlistUiState
    data class Success(val watchlist: Watchlist) : WatchlistUiState
    data object Empty : WatchlistUiState
    data class Error(val message: String) : WatchlistUiState
}
