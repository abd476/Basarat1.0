package com.fyp.basarattradeinvest.feature.shariah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.ShariahRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.ShariahStock
import com.fyp.basarattradeinvest.domain.repository.ShariahRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShariahScreeningViewModel(
    private val repository: ShariahRepository = ShariahRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShariahScreeningUiState>(ShariahScreeningUiState.Loading)
    val uiState: StateFlow<ShariahScreeningUiState> = _uiState.asStateFlow()

    private var currentCategory: String = "KMI-30"
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
            val result = repository.getShariahCompliantStocks(category = currentCategory, query = searchQuery)
            result.fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _uiState.value = ShariahScreeningUiState.Empty
                    } else {
                        _uiState.value = ShariahScreeningUiState.Success(
                            stocks = list,
                            category = currentCategory,
                            totalCount = list.size
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = ShariahScreeningUiState.Error(
                        error.message ?: "Unable to load PSX Shariah compliant stocks."
                    )
                }
            )
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
