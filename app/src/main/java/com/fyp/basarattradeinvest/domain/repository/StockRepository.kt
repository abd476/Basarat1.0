package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.CandleEntry
import com.fyp.basarattradeinvest.domain.model.StockAnalysis
import com.fyp.basarattradeinvest.domain.model.StockDetail
import com.fyp.basarattradeinvest.domain.model.StockTrade

interface StockRepository {
    suspend fun getStockDetail(symbol: String): Result<StockDetail>
    suspend fun getChartData(symbol: String, timeframe: String): Result<List<CandleEntry>>
    suspend fun getRecentTrades(symbol: String): Result<List<StockTrade>>
    suspend fun getStockAnalysis(symbol: String): Result<StockAnalysis>
}
