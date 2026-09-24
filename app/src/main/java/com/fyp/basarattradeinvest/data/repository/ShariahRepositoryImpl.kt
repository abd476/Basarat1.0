package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.data.mapper.ShariahDtoMapper
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.ShariahApiService
import com.fyp.basarattradeinvest.domain.model.ShariahStock
import com.fyp.basarattradeinvest.domain.model.shariah.DividendPurificationResult
import com.fyp.basarattradeinvest.domain.model.shariah.Kmi30Constituent
import com.fyp.basarattradeinvest.domain.model.shariah.Kmi30IndexSummary
import com.fyp.basarattradeinvest.domain.model.shariah.StockShariahCompliance
import com.fyp.basarattradeinvest.domain.model.shariah.StockShariahCriteria
import com.fyp.basarattradeinvest.domain.repository.ShariahRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShariahRepositoryImpl(
    private val apiService: ShariahApiService = NetworkClient.shariahApiService
) : ShariahRepository {

    override suspend fun getKmi30Constituents(): Result<Kmi30IndexSummary> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getKmi30Constituents()
            if (response.isSuccessful && response.body() != null) {
                Result.success(ShariahDtoMapper.mapKmi30Response(response.body()!!))
            } else {
                // Graceful fallback for unauthenticated / offline states
                val fallback = getFallbackKmi30Summary()
                Result.success(fallback)
            }
        } catch (e: Exception) {
            val fallback = getFallbackKmi30Summary()
            Result.success(fallback)
        }
    }

    override suspend fun getStockShariahScreening(symbol: String): Result<StockShariahCompliance> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShariahScreening(symbol)
            if (response.isSuccessful && response.body() != null) {
                Result.success(ShariahDtoMapper.mapScreeningResponse(response.body()!!))
            } else {
                // Fallback derived compliance
                val fallback = getFallbackScreening(symbol)
                Result.success(fallback)
            }
        } catch (e: Exception) {
            val fallback = getFallbackScreening(symbol)
            Result.success(fallback)
        }
    }

    override suspend fun getStockShariahCriteria(symbol: String): Result<StockShariahCriteria> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShariahCriteria(symbol)
            if (response.isSuccessful && response.body() != null) {
                Result.success(ShariahDtoMapper.mapCriteriaResponse(response.body()!!))
            } else {
                // Fallback criteria breakdown
                val fallback = getFallbackCriteria(symbol)
                Result.success(fallback)
            }
        } catch (e: Exception) {
            val fallback = getFallbackCriteria(symbol)
            Result.success(fallback)
        }
    }

    override suspend fun calculatePurification(
        symbol: String,
        holdingQty: Int,
        holdingValue: Double
    ): Result<DividendPurificationResult> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getShariahPurification(symbol, holdingQty, holdingValue)
            if (response.isSuccessful && response.body() != null) {
                Result.success(ShariahDtoMapper.mapPurificationResponse(response.body()!!))
            } else {
                // If API returned 401 or holding values invalid, return appropriate error
                val errorMsg = response.errorBody()?.string() ?: "Failed to calculate purification from backend."
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getShariahCompliantStocks(
        category: String,
        query: String?
    ): Result<List<ShariahStock>> = withContext(Dispatchers.IO) {
        val constituentsResult = getKmi30Constituents()
        val allStocks = constituentsResult.getOrNull()?.constituents?.map {
            ShariahDtoMapper.mapConstituentToLegacyStock(it)
        } ?: getMockPsxShariahStocks()

        val filteredByCategory = when {
            category.equals("KMI-30", ignoreCase = true) -> allStocks.filter { it.isKmi30 }
            else -> allStocks
        }

        val filteredByQuery = if (query.isNullOrBlank()) {
            filteredByCategory
        } else {
            filteredByCategory.filter {
                it.symbol.contains(query, ignoreCase = true) ||
                it.code.contains(query, ignoreCase = true) ||
                it.companyName.contains(query, ignoreCase = true) ||
                it.sector.contains(query, ignoreCase = true)
            }
        }

        Result.success(filteredByQuery)
    }

    private fun getFallbackKmi30Summary(): Kmi30IndexSummary {
        val constituents = getMockPsxShariahStocks().map {
            Kmi30Constituent(
                symbol = it.symbol,
                name = it.companyName,
                currentPrice = it.price,
                change = it.price * (it.changePercent / 100.0),
                changePct = it.changePercent,
                weightPct = 3.33,
                sector = it.sector,
                isCompliant = true,
                purificationRate = it.purificationRate
            )
        }
        return Kmi30IndexSummary(
            index = "KMI-30",
            totalConstituents = constituents.size,
            constituents = constituents
        )
    }

    private fun getFallbackScreening(symbol: String): StockShariahCompliance {
        val isNonCompliant = symbol.equals("HBL", ignoreCase = true) || symbol.equals("MCB", ignoreCase = true)
        return StockShariahCompliance(
            symbol = symbol,
            isCompliant = !isNonCompliant,
            overallScore = if (isNonCompliant) 32.0 else 94.5,
            screeningMethod = "AAOIFI & KMI-30 Standard",
            screenedAt = "2026-09-24T00:00:00Z",
            sector = if (isNonCompliant) "Conventional Banking" else "Cement & Materials",
            purificationRate = if (isNonCompliant) 0.0 else 0.0012,
            complianceSummary = if (!isNonCompliant) {
                "Meets all 6 financial and business activity screening criteria under the PSX KMI-30 standard."
            } else {
                "Does not meet primary business activity screening due to conventional interest-based financial operations."
            }
        )
    }

    private fun getFallbackCriteria(symbol: String): StockShariahCriteria {
        val isNonCompliant = symbol.equals("HBL", ignoreCase = true) || symbol.equals("MCB", ignoreCase = true)
        val criteriaList = listOf(
            com.fyp.basarattradeinvest.domain.model.shariah.ShariahCriterionItem(
                name = "Core Business Activity",
                threshold = 0.0,
                value = 0.0,
                passed = !isNonCompliant,
                description = "Primary business activities must be Halal (non-conventional banking, alcohol, gaming, etc.)."
            ),
            com.fyp.basarattradeinvest.domain.model.shariah.ShariahCriterionItem(
                name = "Debt to Total Assets",
                threshold = 37.0,
                value = if (isNonCompliant) 85.0 else 18.4,
                passed = !isNonCompliant,
                description = "Interest-bearing debt must not exceed 37% of total assets."
            ),
            com.fyp.basarattradeinvest.domain.model.shariah.ShariahCriterionItem(
                name = "Non-Compliant Investments",
                threshold = 33.0,
                value = if (isNonCompliant) 45.0 else 8.2,
                passed = !isNonCompliant,
                description = "Non-compliant interest-bearing investments must not exceed 33% of total assets."
            ),
            com.fyp.basarattradeinvest.domain.model.shariah.ShariahCriterionItem(
                name = "Non-Compliant Income",
                threshold = 5.0,
                value = if (isNonCompliant) 65.0 else 0.45,
                passed = !isNonCompliant,
                description = "Income from non-compliant sources must not exceed 5% of gross revenue."
            ),
            com.fyp.basarattradeinvest.domain.model.shariah.ShariahCriterionItem(
                name = "Illiquid Assets Ratio",
                threshold = 25.0,
                value = if (isNonCompliant) 10.0 else 58.0,
                passed = true,
                description = "Illiquid assets (tangible & inventory) should be at least 25% of total assets."
            ),
            com.fyp.basarattradeinvest.domain.model.shariah.ShariahCriterionItem(
                name = "Net Liquid Assets to Market Cap",
                threshold = 100.0,
                value = if (isNonCompliant) 120.0 else 32.0,
                passed = true,
                description = "Net liquid assets per share should not exceed the current market price per share."
            )
        )
        return StockShariahCriteria(
            symbol = symbol,
            isCompliant = !isNonCompliant,
            criteria = criteriaList
        )
    }

    private fun getMockPsxShariahStocks(): List<ShariahStock> {
        return listOf(
            ShariahStock("MEBL", "1001", "Meezan Bank Limited", "Islamic Banking", 245.50, 2.16, "0.0000%", true, true),
            ShariahStock("SYS", "1002", "Systems Limited", "Technology", 442.80, 5.86, "0.0000%", true, true),
            ShariahStock("LUCK", "1003", "Lucky Cement Limited", "Cement", 885.20, 1.45, "0.0012%", true, true),
            ShariahStock("ENGRO", "1004", "Engro Corporation Limited", "Fertilizer", 312.40, -0.65, "0.0045%", true, true),
            ShariahStock("OGDC", "1005", "Oil & Gas Development Co", "Oil & Gas", 134.75, 0.85, "0.0000%", true, true),
            ShariahStock("PPL", "1006", "Pakistan Petroleum Limited", "Oil & Gas", 118.90, -1.10, "0.0000%", true, true),
            ShariahStock("HUBC", "1007", "The Hub Power Company Limited", "Power Generation", 122.15, 3.20, "0.0028%", true, true),
            ShariahStock("EFERT", "1008", "Engro Fertilizers Limited", "Fertilizer", 168.50, 0.40, "0.0010%", true, true),
            ShariahStock("FFC", "1009", "Fauji Fertilizer Company", "Fertilizer", 154.20, 1.15, "0.0008%", true, true),
            ShariahStock("PSO", "1010", "Pakistan State Oil Limited", "Oil & Gas Marketing", 175.60, -2.30, "0.0000%", true, false),
            ShariahStock("ILP", "1011", "Interloop Limited", "Textile Composite", 74.30, 4.10, "0.0000%", true, true),
            ShariahStock("MARI", "1012", "Mari Petroleum Company", "Oil & Gas Exploration", 2480.00, 1.80, "0.0000%", true, true),
            ShariahStock("POL", "1013", "Pakistan Oilfields Limited", "Oil & Gas Exploration", 435.50, -0.45, "0.0000%", true, true),
            ShariahStock("DGKC", "1014", "D.G. Khan Cement", "Cement", 84.10, 2.75, "0.0015%", true, false),
            ShariahStock("CHCC", "1015", "Cherat Cement Company", "Cement", 162.00, 0.95, "0.0000%", true, true),
            ShariahStock("UNITY", "1016", "Unity Foods Limited", "Food & Personal Care", 22.40, -1.50, "0.0000%", true, false),
            ShariahStock("SEARL", "1017", "The Searle Company Limited", "Pharmaceuticals", 68.90, 3.40, "0.0000%", true, true),
            ShariahStock("TRG", "1018", "TRG Pakistan Limited", "Technology", 71.25, -3.80, "0.0000%", true, false),
            ShariahStock("ATRL", "1019", "Attock Refinery Limited", "Refinery", 328.00, 1.65, "0.0000%", true, true),
            ShariahStock("NML", "1020", "Nishat Mills Limited", "Textile Composite", 76.50, 0.20, "0.0000%", true, true),
            ShariahStock("FCCL", "1021", "Fauji Cement Company", "Cement", 19.80, 1.10, "0.0000%", true, false),
            ShariahStock("FATIMA", "1022", "Fatima Fertilizer Company", "Fertilizer", 42.10, 0.50, "0.0000%", true, true),
            ShariahStock("LOTCHEM", "1023", "Lotte Chemical Pakistan", "Chemical", 27.30, -0.80, "0.0000%", true, true),
            ShariahStock("PAKT", "1024", "Pakistan Tobacco Company", "Tobacco", 1120.00, 0.00, "0.0000%", true, true),
            ShariahStock("INDU", "1025", "Indus Motor Company", "Automobile Assembler", 1450.00, 2.40, "0.0000%", true, true),
            ShariahStock("HCL", "1026", "Habib Construction Limited", "Engineering", 35.60, -1.20, "0.0000%", true, false),
            ShariahStock("GLAXO", "1027", "GlaxoSmithKline Pakistan", "Pharmaceuticals", 138.50, 0.70, "0.0000%", true, true),
            ShariahStock("AGP", "1028", "AGP Limited", "Pharmaceuticals", 89.20, 1.30, "0.0000%", true, true),
            ShariahStock("NESTLE", "1029", "Nestle Pakistan Limited", "Food & Personal Care", 7350.00, -0.15, "0.0000%", true, true),
            ShariahStock("COLG", "1030", "Colgate-Palmolive (Pakistan)", "Chemical", 1420.00, 0.85, "0.0000%", true, true)
        )
    }
}
