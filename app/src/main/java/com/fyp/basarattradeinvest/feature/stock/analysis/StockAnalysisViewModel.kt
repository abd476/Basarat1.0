package com.fyp.basarattradeinvest.feature.stock.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.StockRepositoryImpl
import com.fyp.basarattradeinvest.domain.repository.StockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class StockAnalysisViewModel(
    private val repository: StockRepository = StockRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<StockAnalysisUiState>(StockAnalysisUiState.Loading)
    val uiState: StateFlow<StockAnalysisUiState> = _uiState.asStateFlow()

    private var currentSymbol: String = ""
    private var currentTab: AnalysisTab = AnalysisTab.TECHNICAL

    fun loadAnalysis(symbol: String) {
        currentSymbol = symbol
        viewModelScope.launch {
            _uiState.value = StockAnalysisUiState.Loading

            val result = repository.getStockAnalysis(symbol)
            if (result.isSuccess) {
                val analysis = result.getOrThrow()
                _uiState.value = StockAnalysisUiState.Success(
                    analysis = analysis,
                    selectedTab = currentTab
                )
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
        }
    }

    fun retry() {
        if (currentSymbol.isNotEmpty()) {
            loadAnalysis(currentSymbol)
        }
    }
}
