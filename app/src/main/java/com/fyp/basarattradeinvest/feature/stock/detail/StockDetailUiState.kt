package com.fyp.basarattradeinvest.feature.stock.detail

import com.fyp.basarattradeinvest.domain.model.CandleEntry
import com.fyp.basarattradeinvest.domain.model.StockAnalysis
import com.fyp.basarattradeinvest.domain.model.StockDetail
import com.fyp.basarattradeinvest.domain.model.StockForecast
import com.fyp.basarattradeinvest.domain.model.StockTrade

sealed interface StockDetailUiState {
    data object Loading : StockDetailUiState

    data class Success(
        val detail: StockDetail,
        val candles: List<CandleEntry>,
        val trades: List<StockTrade>,
        val selectedTimeframe: String,
        val selectedSection: String,   // "Quotes", "Analysis", "News", "Profile"
        val selectedSubPill: String,   // Quotes: "By Price", "Trades", "Detailed Quote", "Liquidity" | Analysis: "Technicals", "AI Forecast", "Fundamentals", "Shariah"
        val analysis: StockAnalysis? = null,
        val forecast: StockForecast? = null,
        val isAnalysisLoading: Boolean = false
    ) : StockDetailUiState

    data object Empty : StockDetailUiState

    data class Error(val message: String) : StockDetailUiState
}

