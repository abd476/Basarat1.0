package com.fyp.basarattradeinvest.feature.stock.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.ForecastRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.StockRepositoryImpl
import com.fyp.basarattradeinvest.domain.repository.ForecastRepository
import com.fyp.basarattradeinvest.domain.repository.StockRepository
import com.fyp.basarattradeinvest.domain.usecase.forecast.GetForecastHistoryUseCase
import com.fyp.basarattradeinvest.domain.usecase.forecast.GetStockForecastUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StockAnalysisViewModel(
    private val repository: StockRepository = StockRepositoryImpl(),
    private val forecastRepository: ForecastRepository = ForecastRepositoryImpl(),
    private val getStockForecastUseCase: GetStockForecastUseCase = GetStockForecastUseCase(forecastRepository),
    private val getForecastHistoryUseCase: GetForecastHistoryUseCase = GetForecastHistoryUseCase(forecastRepository)
) : ViewModel() {

    private val _uiState = MutableStateFlow<StockAnalysisUiState>(StockAnalysisUiState.Loading)
    val uiState: StateFlow<StockAnalysisUiState> = _uiState.asStateFlow()

    private var currentSymbol: String = ""
    private var currentTab: AnalysisTab = AnalysisTab.TECHNICAL
    private var currentHorizon: String = "1D"

    fun loadAnalysis(symbol: String) {
        currentSymbol = symbol
        viewModelScope.launch {
            _uiState.value = StockAnalysisUiState.Loading

            val result = repository.getStockAnalysis(symbol)
            if (result.isSuccess) {
                val analysis = result.getOrThrow()
                _uiState.value = StockAnalysisUiState.Success(
                    analysis = analysis,
                    selectedTab = currentTab,
                    selectedHorizon = currentHorizon,
                    forecastState = ForecastSectionState.Loading,
                    historyState = ForecastHistorySectionState.Loading
                )
                // Load forecast data
                loadForecast(symbol, currentHorizon)
                loadForecastHistory(symbol)
            } else {
                _uiState.value = StockAnalysisUiState.Error(
                    result.exceptionOrNull()?.message ?: "Unable to load stock analysis for $symbol"
                )
            }
        }
    }

    fun selectTab(tab: AnalysisTab) {
        if (currentTab == tab) return
        currentTab = tab
        val currentState = _uiState.value
        if (currentState is StockAnalysisUiState.Success) {
            _uiState.value = currentState.copy(selectedTab = tab)
            if (tab == AnalysisTab.FORECAST) {
                if (currentState.forecastState is ForecastSectionState.Error || currentState.forecastState is ForecastSectionState.Loading) {
                    loadForecast(currentSymbol, currentHorizon)
                }
                if (currentState.historyState is ForecastHistorySectionState.Error || currentState.historyState is ForecastHistorySectionState.Loading) {
                    loadForecastHistory(currentSymbol)
                }
            }
        }
    }

    fun selectHorizon(horizon: String) {
        if (currentHorizon == horizon) return
        currentHorizon = horizon
        val currentState = _uiState.value
        if (currentState is StockAnalysisUiState.Success) {
            _uiState.value = currentState.copy(
                selectedHorizon = horizon,
                forecastState = ForecastSectionState.Loading
            )
            loadForecast(currentSymbol, horizon)
        }
    }

    fun retryForecast() {
        if (currentSymbol.isNotEmpty()) {
            val currentState = _uiState.value
            if (currentState is StockAnalysisUiState.Success) {
                _uiState.value = currentState.copy(forecastState = ForecastSectionState.Loading)
            }
            loadForecast(currentSymbol, currentHorizon)
        }
    }

    fun retryHistory() {
        if (currentSymbol.isNotEmpty()) {
            val currentState = _uiState.value
            if (currentState is StockAnalysisUiState.Success) {
                _uiState.value = currentState.copy(historyState = ForecastHistorySectionState.Loading)
            }
            loadForecastHistory(currentSymbol)
        }
    }

    private fun loadForecast(symbol: String, horizon: String) {
        viewModelScope.launch {
            val result = getStockForecastUseCase(symbol, horizon)
            val currentState = _uiState.value
            if (currentState is StockAnalysisUiState.Success) {
                if (result.isSuccess) {
                    val forecast = result.getOrThrow()
                    _uiState.value = currentState.copy(
                        forecastState = ForecastSectionState.Success(forecast)
                    )
                } else {
                    _uiState.value = currentState.copy(
                        forecastState = ForecastSectionState.Error(
                            result.exceptionOrNull()?.message ?: "Unable to load ML forecast"
                        )
                    )
                }
            }
        }
    }

    private fun loadForecastHistory(symbol: String) {
        viewModelScope.launch {
            val result = getForecastHistoryUseCase(symbol, limit = 30)
            val currentState = _uiState.value
            if (currentState is StockAnalysisUiState.Success) {
                if (result.isSuccess) {
                    val history = result.getOrThrow()
                    _uiState.value = currentState.copy(
                        historyState = if (history.history.isEmpty()) {
                            ForecastHistorySectionState.Empty
                        } else {
                            ForecastHistorySectionState.Success(history)
                        }
                    )
                } else {
                    _uiState.value = currentState.copy(
                        historyState = ForecastHistorySectionState.Error(
                            result.exceptionOrNull()?.message ?: "Unable to load forecast history"
                        )
                    )
                }
            }
        }
    }

    fun retry() {
        if (currentSymbol.isNotEmpty()) {
            loadAnalysis(currentSymbol)
        }
    }
}
