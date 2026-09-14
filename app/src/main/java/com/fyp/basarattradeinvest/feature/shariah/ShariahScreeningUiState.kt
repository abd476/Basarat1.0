package com.fyp.basarattradeinvest.feature.shariah

import com.fyp.basarattradeinvest.domain.model.ShariahStock

sealed interface ShariahScreeningUiState {
    data object Loading : ShariahScreeningUiState

    data class Success(
        val stocks: List<ShariahStock>,
        val category: String,
        val totalCount: Int
    ) : ShariahScreeningUiState

    data object Empty : ShariahScreeningUiState

    data class Error(val message: String) : ShariahScreeningUiState
}
