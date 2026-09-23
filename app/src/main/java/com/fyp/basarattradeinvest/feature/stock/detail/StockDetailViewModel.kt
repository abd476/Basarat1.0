package com.fyp.basarattradeinvest.feature.stock.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.StockRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.CandleEntry
import com.fyp.basarattradeinvest.domain.model.StockTrade
import com.fyp.basarattradeinvest.domain.repository.StockRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StockDetailViewModel(
    private val repository: StockRepository = StockRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<StockDetailUiState>(StockDetailUiState.Loading)
    val uiState: StateFlow<StockDetailUiState> = _uiState.asStateFlow()

    private var currentSymbol: String = ""
    private var currentTimeframe: String = "Weekly"
    private var currentSection: String = "Quotes"
    private var currentSubPill: String = "By Price"

    fun loadStock(symbol: String) {
        currentSymbol = symbol
        viewModelScope.launch {
            _uiState.value = StockDetailUiState.Loading

            coroutineScope {
                val detailDeferred = async { repository.getStockOverview(symbol) }
                val chartDeferred = async { repository.getPriceHistory(symbol, currentTimeframe) }
                val tradesDeferred = async { repository.getRecentTrades(symbol) }

                val detailResult = detailDeferred.await()
                val chartResult = chartDeferred.await()
                val tradesResult = tradesDeferred.await()

                if (detailResult.isSuccess) {
                    val detail = detailResult.getOrThrow()
                    val candles = chartResult.getOrDefault(emptyList())
                    val trades = tradesResult.getOrDefault(emptyList())

                    _uiState.value = StockDetailUiState.Success(
                        detail = detail,
                        candles = candles,
                        trades = trades,
                        selectedTimeframe = currentTimeframe,
                        selectedSection = currentSection,
                        selectedSubPill = currentSubPill
                    )
                } else {
                    _uiState.value = StockDetailUiState.Error("Unable to load stock details for $symbol")
                }
            }
        }
    }

    fun setTimeframe(timeframe: String) {
        if (currentTimeframe == timeframe) return
        currentTimeframe = timeframe
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is StockDetailUiState.Success) {
                val chartResult = repository.getPriceHistory(currentSymbol, timeframe)
                val candles = chartResult.getOrDefault(emptyList())
                _uiState.value = currentState.copy(
                    candles = candles,
                    selectedTimeframe = timeframe
                )
            }
        }
    }

    fun setSection(section: String) {
        currentSection = section
        // Reset sub-pill to first option when switching sections
        currentSubPill = when (section) {
            "Quotes" -> "By Price"
            "News" -> "Latest"
            "Profile" -> "Overview"
            else -> "By Price"
        }
        val currentState = _uiState.value
        if (currentState is StockDetailUiState.Success) {
            _uiState.value = currentState.copy(
                selectedSection = currentSection,
                selectedSubPill = currentSubPill
            )
        }
    }

    fun setSubPill(pill: String) {
        currentSubPill = pill
        val currentState = _uiState.value
        if (currentState is StockDetailUiState.Success) {
            _uiState.value = currentState.copy(selectedSubPill = pill)
        }
    }

    fun retry() {
        if (currentSymbol.isNotEmpty()) {
            loadStock(currentSymbol)
        }
    }
}
