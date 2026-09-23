package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.ForecastHistory
import com.fyp.basarattradeinvest.domain.model.StockForecast

/**
 * Repository contract for BASARAT ML Forecast operations
 */
interface ForecastRepository {
    suspend fun getStockForecast(symbol: String, horizon: String = "1D"): Result<StockForecast>
    suspend fun getForecastHistory(symbol: String, limit: Int = 30): Result<ForecastHistory>
}
