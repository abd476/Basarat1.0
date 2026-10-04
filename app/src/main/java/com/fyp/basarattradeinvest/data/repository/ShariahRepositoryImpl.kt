package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.core.network.NetworkErrorParser
import com.fyp.basarattradeinvest.data.mapper.ShariahDtoMapper
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.ShariahApiService
import com.fyp.basarattradeinvest.domain.model.ShariahStock
import com.fyp.basarattradeinvest.domain.model.shariah.DividendPurificationResult
import com.fyp.basarattradeinvest.domain.model.shariah.Kmi30IndexSummary
import com.fyp.basarattradeinvest.domain.model.shariah.StockShariahCompliance
import com.fyp.basarattradeinvest.domain.model.shariah.StockShariahCriteria
import com.fyp.basarattradeinvest.domain.repository.ShariahRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShariahRepositoryImpl(
    private val apiService: ShariahApiService = NetworkClient.shariahApiService
) : ShariahRepository {

    override suspend fun getKmi30Constituents(): Result<Kmi30IndexSummary> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getKmi30Constituents()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(ShariahDtoMapper.mapKmi30Response(body))
            } else {
                Result.failure(Exception(NetworkErrorParser.parseError(response)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Unable to load the KMI-30 Shariah screening list."))
        }
    }

    override suspend fun getStockShariahScreening(symbol: String): Result<StockShariahCompliance> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShariahScreening(symbol)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(ShariahDtoMapper.mapScreeningResponse(body))
            } else {
                Result.failure(Exception(NetworkErrorParser.parseError(response)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Unable to load Shariah compliance for $symbol."))
        }
    }

    override suspend fun getStockShariahCriteria(symbol: String): Result<StockShariahCriteria> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShariahCriteria(symbol)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(ShariahDtoMapper.mapCriteriaResponse(body))
            } else {
                Result.failure(Exception(NetworkErrorParser.parseError(response)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Unable to load Shariah screening criteria for $symbol."))
        }
    }

    override suspend fun calculatePurification(
        symbol: String,
        dividendIncome: Double
    ): Result<DividendPurificationResult> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShariahPurification(symbol, dividendIncome)
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(ShariahDtoMapper.mapPurificationResponse(body))
            } else {
                Result.failure(Exception(NetworkErrorParser.parseError(response)))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception(e.message ?: "Unable to calculate purification for $symbol."))
        }
    }

    override suspend fun getShariahCompliantStocks(
        category: String,
        query: String?
    ): Result<List<ShariahStock>> = withContext(Dispatchers.IO) {
        getKmi30Constituents().mapCatching { summary ->
            val allStocks = summary.constituents.map { ShariahDtoMapper.mapConstituentToLegacyStock(it) }

            val filteredByCategory = when {
                category.equals("KMI-30", ignoreCase = true) -> allStocks.filter { it.isKmi30 }
                else -> allStocks
            }

            if (query.isNullOrBlank()) {
                filteredByCategory
            } else {
                filteredByCategory.filter {
                    it.symbol.contains(query, ignoreCase = true) ||
                        it.companyName.contains(query, ignoreCase = true)
                }
            }
        }
    }
}
