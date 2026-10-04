package com.fyp.basarattradeinvest.data.remote.api

import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahCriteriaResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahKmi30ResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahPurificationResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.shariah.ShariahScreeningResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service definition for BASARAT Shariah module.
 * Source of truth: OpenAPI at <base url>/api/v1/openapi.json (see NetworkClient for the host).
 */
interface ShariahApiService {

    @GET("api/v1/shariah/kmi30")
    suspend fun getKmi30Constituents(): Response<ShariahKmi30ResponseDto>

    @GET("api/v1/shariah/{symbol}")
    suspend fun getShariahScreening(
        @Path("symbol") symbol: String
    ): Response<ShariahScreeningResponseDto>

    @GET("api/v1/shariah/{symbol}/criteria")
    suspend fun getShariahCriteria(
        @Path("symbol") symbol: String
    ): Response<ShariahCriteriaResponseDto>

    @GET("api/v1/shariah/{symbol}/purification")
    suspend fun getShariahPurification(
        @Path("symbol") symbol: String,
        @Query("dividend_income") dividendIncome: Double
    ): Response<ShariahPurificationResponseDto>
}
