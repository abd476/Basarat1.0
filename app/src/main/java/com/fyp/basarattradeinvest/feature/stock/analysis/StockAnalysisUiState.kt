package com.fyp.basarattradeinvest.feature.stock.analysis

import com.fyp.basarattradeinvest.domain.model.ForecastHistory
import com.fyp.basarattradeinvest.domain.model.StockAnalysis
import com.fyp.basarattradeinvest.domain.model.StockForecast

sealed interface StockAnalysisUiState {
    data object Loading : StockAnalysisUiState

    data class Success(
        val analysis: StockAnalysis,
        val selectedTab: AnalysisTab = AnalysisTab.TECHNICAL,
        val selectedHorizon: String = "1D",
        val forecastState: ForecastSectionState = ForecastSectionState.Loading,
        val historyState: ForecastHistorySectionState = ForecastHistorySectionState.Loading
    ) : StockAnalysisUiState

    data object Empty : StockAnalysisUiState

    data class Error(
        val message: String
    ) : StockAnalysisUiState
}

enum class AnalysisTab {
    TECHNICAL,
    FUNDAMENTALS,
    FORECAST
}

sealed interface ForecastSectionState {
    data object Loading : ForecastSectionState
    data class Success(val forecast: StockForecast) : ForecastSectionState
    data object Empty : ForecastSectionState
    data class Error(val message: String) : ForecastSectionState
}

sealed interface ForecastHistorySectionState {
    data object Loading : ForecastHistorySectionState
    data class Success(val history: ForecastHistory) : ForecastHistorySectionState
    data object Empty : ForecastHistorySectionState
    data class Error(val message: String) : ForecastHistorySectionState
}
