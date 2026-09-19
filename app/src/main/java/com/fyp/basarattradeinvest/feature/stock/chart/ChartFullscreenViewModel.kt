package com.fyp.basarattradeinvest.feature.stock.chart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.StockRepositoryImpl
import com.fyp.basarattradeinvest.domain.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChartFullscreenViewModel(
    private val repository: StockRepository = StockRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ChartFullscreenUiState>(ChartFullscreenUiState.Loading)
    val uiState: StateFlow<ChartFullscreenUiState> = _uiState.asStateFlow()

    private var currentSymbol: String = "LUCK"
    private var currentTimeframe: String = "1W"
    private val activeIndicators = mutableSetOf("MA", "VOL")
    private var currentTool: DrawingTool = DrawingTool.CROSSHAIR

    fun loadChart(symbol: String, timeframe: String = currentTimeframe) {
        currentSymbol = symbol
        currentTimeframe = timeframe

        viewModelScope.launch {
            _uiState.value = ChartFullscreenUiState.Loading

            val detailResult = repository.getStockDetail(currentSymbol)
            val chartResult = repository.getChartData(currentSymbol, currentTimeframe)

            if (detailResult.isSuccess && chartResult.isSuccess) {
                val detail = detailResult.getOrThrow()
                val candles = chartResult.getOrDefault(emptyList())

                if (candles.isEmpty()) {
                    _uiState.value = ChartFullscreenUiState.Empty
                } else {
                    _uiState.value = ChartFullscreenUiState.Success(
                        stockDetail = detail,
                        candles = candles,
                        selectedTimeframe = currentTimeframe,
                        activeIndicators = activeIndicators.toSet(),
                        selectedTool = currentTool
                    )
                }
            } else {
                _uiState.value = ChartFullscreenUiState.Error("Unable to load chart data for $symbol")
            }
        }
    }

    fun setTimeframe(timeframe: String) {
        if (currentTimeframe == timeframe) return
        currentTimeframe = timeframe
        loadChart(currentSymbol, timeframe)
    }

    fun toggleIndicator(indicator: String) {
        if (activeIndicators.contains(indicator)) {
            activeIndicators.remove(indicator)
        } else {
            activeIndicators.add(indicator)
        }

        val currentState = _uiState.value
        if (currentState is ChartFullscreenUiState.Success) {
            _uiState.value = currentState.copy(
                activeIndicators = activeIndicators.toSet()
            )
        }
    }

    fun selectTool(tool: DrawingTool) {
        currentTool = if (currentTool == tool) DrawingTool.NONE else tool
        val currentState = _uiState.value
        if (currentState is ChartFullscreenUiState.Success) {
            _uiState.value = currentState.copy(selectedTool = currentTool)
        }
    }

    fun clearDrawings() {
        currentTool = DrawingTool.NONE
        val currentState = _uiState.value
        if (currentState is ChartFullscreenUiState.Success) {
            _uiState.value = currentState.copy(selectedTool = currentTool)
        }
    }

    fun retry() {
        loadChart(currentSymbol, currentTimeframe)
    }
}
