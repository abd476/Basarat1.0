package com.fyp.basarattradeinvest.feature.markets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.MarketRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.SymbolFilterType
import com.fyp.basarattradeinvest.domain.usecase.GetMarketIndicesUseCase
import com.fyp.basarattradeinvest.domain.usecase.GetMarketOverviewUseCase
import com.fyp.basarattradeinvest.domain.usecase.GetTopSymbolsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.fyp.basarattradeinvest.domain.usecase.GetMarketSentimentUseCase
import com.fyp.basarattradeinvest.domain.usecase.GetSectorPerformancesUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarketDashboardViewModel(
    private val getMarketIndicesUseCase: GetMarketIndicesUseCase = GetMarketIndicesUseCase(MarketRepositoryImpl()),
    private val getMarketOverviewUseCase: GetMarketOverviewUseCase = GetMarketOverviewUseCase(MarketRepositoryImpl()),
    private val getTopSymbolsUseCase: GetTopSymbolsUseCase = GetTopSymbolsUseCase(MarketRepositoryImpl()),
    private val getMarketSentimentUseCase: GetMarketSentimentUseCase = GetMarketSentimentUseCase(MarketRepositoryImpl()),
    private val getSectorPerformancesUseCase: GetSectorPerformancesUseCase = GetSectorPerformancesUseCase(MarketRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<MarketDashboardUiState>(MarketDashboardUiState.Loading)
    val uiState: StateFlow<MarketDashboardUiState> = _uiState.asStateFlow()

    private var currentFilter = SymbolFilterType.GAINERS

    init {
        loadMarketData()
    }

    fun loadMarketData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh && _uiState.value !is MarketDashboardUiState.Success) {
                _uiState.value = MarketDashboardUiState.Loading
            }

            val indicesDeferred = async { getMarketIndicesUseCase() }
            val overviewDeferred = async { getMarketOverviewUseCase() }
            val topSymbolsDeferred = async { getTopSymbolsUseCase(currentFilter) }
            val sentimentDeferred = async { getMarketSentimentUseCase() }
            val sectorsDeferred = async { getSectorPerformancesUseCase() }

            val indicesResult = indicesDeferred.await()
            val overviewResult = overviewDeferred.await()
            val topSymbolsResult = topSymbolsDeferred.await()
            val sentimentResult = sentimentDeferred.await()
            val sectorsResult = sectorsDeferred.await()

            if (indicesResult.isSuccess && overviewResult.isSuccess && topSymbolsResult.isSuccess) {
                val indices = indicesResult.getOrDefault(emptyList())
                val overview = overviewResult.getOrNull()
                val topSymbols = topSymbolsResult.getOrDefault(emptyList())
                val sentiment = sentimentResult.getOrNull()
                val sectors = sectorsResult.getOrDefault(emptyList())

                val currentTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())

                if (overview != null && sentiment != null) {
                    _uiState.value = MarketDashboardUiState.Success(
                        indices = indices,
                        overview = overview,
                        topSymbols = topSymbols,
                        selectedFilter = currentFilter,
                        sentiment = sentiment,
                        sectors = sectors,
                        lastUpdated = currentTime
                    )
                } else {
                    _uiState.value = MarketDashboardUiState.Empty
                }
            } else {
                _uiState.value = MarketDashboardUiState.Error("Unable to load market data. Please try again.")
            }
        }
    }

    fun refreshData() {
        loadMarketData(isRefresh = true)
    }

    fun setFilter(filterType: SymbolFilterType) {
        if (currentFilter == filterType) return
        currentFilter = filterType

        val currentState = _uiState.value
        viewModelScope.launch {
            val topSymbolsResult = getTopSymbolsUseCase(filterType)
            if (topSymbolsResult.isSuccess && currentState is MarketDashboardUiState.Success) {
                _uiState.value = currentState.copy(
                    topSymbols = topSymbolsResult.getOrDefault(emptyList()),
                    selectedFilter = filterType
                )
            }
        }
    }
}
