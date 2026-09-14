package com.fyp.basarattradeinvest.data.remote.api

import com.fyp.basarattradeinvest.data.remote.dto.MarketIndexDto
import com.fyp.basarattradeinvest.data.remote.dto.MarketOverviewDto
import com.fyp.basarattradeinvest.data.remote.dto.MarketSentimentDto
import com.fyp.basarattradeinvest.data.remote.dto.MarketStockDto
import com.fyp.basarattradeinvest.data.remote.dto.SectorPerformanceDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MarketApiService {

    @GET("api/v1/markets/indices")
    suspend fun getIndices(): Response<List<MarketIndexDto>>

    @GET("api/v1/markets/overview")
    suspend fun getMarketOverview(): Response<MarketOverviewDto>

    @GET("api/v1/markets/symbols")
    suspend fun getSymbols(
        @Query("filter") filter: String
    ): Response<List<MarketStockDto>>

    @GET("api/v1/markets/sentiment")
    suspend fun getMarketSentiment(): Response<MarketSentimentDto>

    @GET("api/v1/markets/sectors")
    suspend fun getSectorPerformances(): Response<List<SectorPerformanceDto>>
}
