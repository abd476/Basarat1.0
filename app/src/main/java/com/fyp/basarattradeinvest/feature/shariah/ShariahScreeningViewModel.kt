package com.fyp.basarattradeinvest.feature.shariah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.ShariahRepositoryImpl
import com.fyp.basarattradeinvest.domain.usecase.GetShariahStocksUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShariahScreeningViewModel(
    private val getShariahStocksUseCase: GetShariahStocksUseCase = GetShariahStocksUseCase(ShariahRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShariahScreeningUiState>(ShariahScreeningUiState.Loading)
    val uiState: StateFlow<ShariahScreeningUiState> = _uiState.asStateFlow()

    private var currentCategory: String = "All Stocks"
    private var searchQuery: String = ""

    init {
        loadStocks()
    }

    fun loadStocks(
        category: String = currentCategory,
        query: String = searchQuery
    ) {
        currentCategory = category
        searchQuery = query

        viewModelScope.launch {
            _uiState.value = ShariahScreeningUiState.Loading
            val result = getShariahStocksUseCase(category = currentCategory, query = searchQuery)
            if (result.isSuccess) {
                val list = result.getOrDefault(emptyList())
                if (list.isEmpty()) {
                    _uiState.value = ShariahScreeningUiState.Empty
                } else {
                    _uiState.value = ShariahScreeningUiState.Success(
                        stocks = list,
                        category = currentCategory,
                        totalCount = list.size
                    )
                }
            } else {
                _uiState.value = ShariahScreeningUiState.Error("Unable to load PSX Shariah compliant stocks.")
            }
        }
    }

    fun setCategory(category: String) {
        if (currentCategory.equals(category, ignoreCase = true)) return
        loadStocks(category = category, query = searchQuery)
    }

    fun onSearchQueryChanged(query: String) {
        loadStocks(category = currentCategory, query = query)
    }
}
