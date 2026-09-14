package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.MarketApiService
import com.fyp.basarattradeinvest.data.remote.mapper.toDomain
import com.fyp.basarattradeinvest.domain.model.MarketIndex
import com.fyp.basarattradeinvest.domain.model.MarketOverview
import com.fyp.basarattradeinvest.domain.model.MarketSentiment
import com.fyp.basarattradeinvest.domain.model.MarketStock
import com.fyp.basarattradeinvest.domain.model.SectorPerformance
import com.fyp.basarattradeinvest.domain.model.SymbolFilterType
import com.fyp.basarattradeinvest.domain.repository.MarketRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MarketRepositoryImpl(
    private val apiService: MarketApiService = NetworkClient.marketApiService
) : MarketRepository {

    companion object {
        // Toggle for development / offline mock mode per Blueprint Phase 3
        // Set to false when live backend API endpoint is active
        var IS_MOCK_MODE = true
    }

    override suspend fun getMarketIndices(): Result<List<MarketIndex>> = withContext(Dispatchers.IO) {
        if (IS_MOCK_MODE) {
            return@withContext Result.success(getFallbackIndices())
        }
        try {
            val response = apiService.getIndices()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.success(getFallbackIndices())
            }
        } catch (e: Exception) {
            Result.success(getFallbackIndices())
        }
    }

    override suspend fun getMarketOverview(): Result<MarketOverview> = withContext(Dispatchers.IO) {
        if (IS_MOCK_MODE) {
            return@withContext Result.success(getFallbackOverview())
        }
        try {
            val response = apiService.getMarketOverview()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.success(getFallbackOverview())
            }
        } catch (e: Exception) {
            Result.success(getFallbackOverview())
        }
    }

    override suspend fun getTopSymbols(filterType: SymbolFilterType): Result<List<MarketStock>> = withContext(Dispatchers.IO) {
        if (IS_MOCK_MODE) {
            return@withContext Result.success(getFallbackSymbols(filterType))
        }
        try {
            val response = apiService.getSymbols(filterType.name.lowercase())
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.success(getFallbackSymbols(filterType))
            }
        } catch (e: Exception) {
            Result.success(getFallbackSymbols(filterType))
        }
    }

    override suspend fun getMarketSentiment(): Result<MarketSentiment> = withContext(Dispatchers.IO) {
        if (IS_MOCK_MODE) {
            return@withContext Result.success(getFallbackSentiment())
        }
        try {
            val response = apiService.getMarketSentiment()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.toDomain())
            } else {
                Result.success(getFallbackSentiment())
            }
        } catch (e: Exception) {
            Result.success(getFallbackSentiment())
        }
    }

    override suspend fun getSectorPerformances(): Result<List<SectorPerformance>> = withContext(Dispatchers.IO) {
        if (IS_MOCK_MODE) {
            return@withContext Result.success(getFallbackSectors())
        }
        try {
            val response = apiService.getSectorPerformances()
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.map { it.toDomain() })
            } else {
                Result.success(getFallbackSectors())
            }
        } catch (e: Exception) {
            Result.success(getFallbackSectors())
        }
    }

    private fun getFallbackIndices(): List<MarketIndex> {
        return listOf(
            MarketIndex(
                id = "KSE-100",
                name = "KSE-100",
                currentPoints = 85421.32,
                changePoints = -142.70,
                changePercent = -1.30,
                intradayPoints = listOf(
                    85620f, 85580f, 85510f, 85460f, 85390f, 85340f, 85290f, 85240f,
                    85270f, 85310f, 85280f, 85220f, 85180f, 85240f, 85300f, 85360f,
                    85390f, 85410f, 85435f, 85421f
                )
            ),
            MarketIndex(
                id = "KMI-30",
                name = "KMI-30",
                currentPoints = 143579.19,
                changePoints = -53.44,
                changePercent = -0.25,
                intradayPoints = listOf(
                    143710f, 143680f, 143630f, 143590f, 143540f, 143500f, 143470f, 143490f,
                    143520f, 143480f, 143450f, 143430f, 143460f, 143500f, 143530f, 143550f,
                    143565f, 143580f, 143570f, 143579f
                )
            )
        )
    }

    private fun getFallbackOverview(): MarketOverview {
        return MarketOverview(
            tradedValue = "12.83B",
            tradedVolume = "187.14M",
            totalTrades = "378,731",
            upCount = 65,
            unchangedCount = 55,
            downCount = 254,
            totalStocks = 374
        )
    }

    private fun getFallbackSymbols(filterType: SymbolFilterType): List<MarketStock> {
        return when (filterType) {
            SymbolFilterType.GAINERS -> listOf(
                MarketStock(1, "SYS", "Systems Limited", "Technology", 442.80, 24.50, 5.86, "4.2M", true),
                MarketStock(2, "LUCK", "Lucky Cement Limited", "Cement", 912.00, 48.00, 5.56, "3.1M", true),
                MarketStock(3, "OGDC", "Oil & Gas Development", "Oil & Gas", 154.60, 5.20, 3.48, "8.9M", true),
                MarketStock(4, "ENGRO", "Engro Corporation", "Fertilizer", 318.50, 9.80, 3.17, "2.7M", true),
                MarketStock(5, "HUBC", "The Hub Power Company", "Power", 138.20, 3.40, 2.52, "12.4M", true),
                MarketStock(6, "FFC", "Fauji Fertilizer", "Fertilizer", 192.40, 4.10, 2.18, "5.6M", true),
                MarketStock(7, "HBL", "Habib Bank Limited", "Commercial Banks", 121.50, 2.10, 1.76, "3.8M", false)
            )
            SymbolFilterType.LOSERS -> listOf(
                MarketStock(1, "PRL", "Pakistan Refinery Limited", "Refinery", 28.50, -2.85, -9.09, "14.2M", true),
                MarketStock(2, "TRG", "TRG Pakistan Limited", "Technology", 62.40, -4.10, -6.17, "9.1M", true),
                MarketStock(3, "DGKC", "D.G. Khan Cement", "Cement", 78.20, -3.90, -4.75, "4.3M", true),
                MarketStock(4, "WTL", "WorldCall Telecom", "Technology", 1.25, -0.06, -4.58, "28.5M", false),
                MarketStock(5, "PAEL", "Pak Elektron Limited", "Engineering", 22.80, -0.98, -4.12, "6.7M", true),
                MarketStock(6, "UNITY", "Unity Foods Limited", "Food & Personal", 26.40, -1.10, -4.00, "11.2M", true),
                MarketStock(7, "SEARL", "The Searle Company", "Pharmaceuticals", 58.60, -2.10, -3.46, "2.4M", true)
            )
            SymbolFilterType.MOST_ACTIVE -> listOf(
                MarketStock(1, "WTL", "WorldCall Telecom", "Technology", 1.25, -0.06, -4.58, "28.5M", false),
                MarketStock(2, "OGDC", "Oil & Gas Development", "Oil & Gas", 154.60, 5.20, 3.48, "18.9M", true),
                MarketStock(3, "PRL", "Pakistan Refinery Limited", "Refinery", 28.50, -2.85, -9.09, "14.2M", true),
                MarketStock(4, "HUBC", "The Hub Power Company", "Power", 138.20, 3.40, 2.52, "12.4M", true),
                MarketStock(5, "UNITY", "Unity Foods Limited", "Food & Personal", 26.40, -1.10, -4.00, "11.2M", true)
            )
            SymbolFilterType.VOLUME_SPIKES -> listOf(
                MarketStock(1, "SYS", "Systems Limited", "Technology", 442.80, 24.50, 5.86, "3.8x Avg", true),
                MarketStock(2, "LUCK", "Lucky Cement Limited", "Cement", 912.00, 48.00, 5.56, "2.9x Avg", true),
                MarketStock(3, "FFC", "Fauji Fertilizer", "Fertilizer", 192.40, 4.10, 2.18, "2.4x Avg", true)
            )
            SymbolFilterType.WATCHLIST -> listOf(
                MarketStock(1, "SYS", "Systems Limited", "Technology", 442.80, 24.50, 5.86, "4.2M", true),
                MarketStock(2, "LUCK", "Lucky Cement Limited", "Cement", 912.00, 48.00, 5.56, "3.1M", true),
                MarketStock(3, "ENGRO", "Engro Corporation", "Fertilizer", 318.50, 9.80, 3.17, "2.7M", true)
            )
        }
    }

    private fun getFallbackSentiment(): MarketSentiment {
        return MarketSentiment(
            overallScore = 68,
            sentimentLabel = "Bullish",
            positivePercent = 62,
            neutralPercent = 23,
            negativePercent = 15,
            newsCount = 142,
            aiSummary = "PSX sentiment remains strongly bullish following SBP policy rate cuts, robust earnings in Technology, and foreign institutional inflows."
        )
    }

    private fun getFallbackSectors(): List<SectorPerformance> {
        return listOf(
            SectorPerformance("tech", "Technology & Comm.", 3.42, "42.1M", true),
            SectorPerformance("banks", "Commercial Banks", 1.85, "68.4M", true),
            SectorPerformance("oil", "Oil & Gas Exploration", 0.51, "29.7M", true),
            SectorPerformance("fert", "Fertilizer", 0.38, "18.3M", true),
            SectorPerformance("power", "Power Generation", -0.42, "14.2M", false),
            SectorPerformance("cement", "Cement", -0.94, "22.6M", false),
            SectorPerformance("refinery", "Refinery", -2.85, "16.8M", false)
        )
    }
}
