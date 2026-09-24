package com.fyp.basarattradeinvest.domain.usecase.forecast

import com.fyp.basarattradeinvest.domain.model.ForecastHistory
import com.fyp.basarattradeinvest.domain.repository.ForecastRepository

/**
 * UseCase to fetch historical forecast performance and accuracy
 */
class GetForecastHistoryUseCase(
    private val repository: ForecastRepository
) {
    suspend operator fun invoke(symbol: String, limit: Int = 30): Result<ForecastHistory> {
        if (symbol.isBlank()) {
            return Result.failure(IllegalArgumentException("Stock symbol cannot be blank"))
        }
        val safeLimit = limit.coerceIn(1, 100)
        return repository.getForecastHistory(symbol.trim().uppercase(), safeLimit)
    }
}
