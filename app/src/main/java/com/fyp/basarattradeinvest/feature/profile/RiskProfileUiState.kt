package com.fyp.basarattradeinvest.feature.profile

import com.fyp.basarattradeinvest.domain.model.InvestmentProfileOptions
import com.fyp.basarattradeinvest.domain.model.UserProfile

sealed interface RiskProfileUiState {
    data object Loading : RiskProfileUiState
    data class Success(
        val options: InvestmentProfileOptions,
        val currentProfile: UserProfile?
    ) : RiskProfileUiState
    data class Error(val message: String) : RiskProfileUiState
}
