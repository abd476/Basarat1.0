package com.fyp.basarattradeinvest.feature.stock.analysis

import com.fyp.basarattradeinvest.domain.model.StockAnalysis

sealed interface StockAnalysisUiState {
    data object Loading : StockAnalysisUiState

    data class Success(
        val analysis: StockAnalysis,
        val selectedTab: AnalysisTab = AnalysisTab.TECHNICAL
    ) : StockAnalysisUiState

    data object Empty : StockAnalysisUiState

    data class Error(
        val message: String
    ) : StockAnalysisUiState
}

enum class AnalysisTab {
    TECHNICAL,
    FUNDAMENTALS
}
