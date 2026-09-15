package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.domain.model.ShariahStock
import com.fyp.basarattradeinvest.domain.repository.ShariahRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ShariahRepositoryImpl : ShariahRepository {

    override suspend fun getShariahCompliantStocks(
        category: String,
        query: String?
    ): Result<List<ShariahStock>> = withContext(Dispatchers.IO) {
        val allStocks = getMockPsxShariahStocks()

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

    private fun getMockPsxShariahStocks(): List<ShariahStock> {
        return listOf(
            ShariahStock(
                symbol = "MEBL",
                code = "1001",
                companyName = "Meezan Bank Limited",
                sector = "Islamic Banking",
                price = 245.50,
                changePercent = 2.16,
                purificationRate = "0.000000",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "SYS",
                code = "1002",
                companyName = "Systems Limited",
                sector = "Technology",
                price = 442.80,
                changePercent = 5.86,
                purificationRate = "0.000000",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "LUCK",
                code = "1003",
                companyName = "Lucky Cement Limited",
                sector = "Cement",
                price = 912.00,
                changePercent = 5.56,
                purificationRate = "0.012400",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "ENGRO",
                code = "1004",
                companyName = "Engro Corporation",
                sector = "Fertilizer",
                price = 318.50,
                changePercent = 3.17,
                purificationRate = "0.018000",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "OGDC",
                code = "1005",
                companyName = "Oil & Gas Development Co.",
                sector = "Oil & Gas",
                price = 154.60,
                changePercent = 3.48,
                purificationRate = "0.000000",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "HUBC",
                code = "1006",
                companyName = "The Hub Power Company",
                sector = "Power Generation",
                price = 138.20,
                changePercent = 2.52,
                purificationRate = "0.021000",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "MARI",
                code = "1007",
                companyName = "Mari Energies Limited",
                sector = "Oil & Gas",
                price = 624.00,
                changePercent = 3.06,
                purificationRate = "0.000000",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "FFC",
                code = "1008",
                companyName = "Fauji Fertilizer Company",
                sector = "Fertilizer",
                price = 192.40,
                changePercent = 2.18,
                purificationRate = "0.014500",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "ILP",
                code = "1009",
                companyName = "Interloop Limited",
                sector = "Textile",
                price = 84.60,
                changePercent = 2.55,
                purificationRate = "0.000000",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "EFERT",
                code = "1010",
                companyName = "Engro Fertilizers Limited",
                sector = "Fertilizer",
                price = 165.20,
                changePercent = 1.95,
                purificationRate = "0.023000",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "POL",
                code = "1011",
                companyName = "Pakistan Oilfields Limited",
                sector = "Oil & Gas",
                price = 485.00,
                changePercent = 1.72,
                purificationRate = "0.000000",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "PSO",
                code = "1012",
                companyName = "Pakistan State Oil",
                sector = "Oil & Gas",
                price = 178.40,
                changePercent = 1.45,
                purificationRate = "0.011200",
                isKmi30 = true,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "AIRLINK",
                code = "1013",
                companyName = "Air Link Communication",
                sector = "Technology",
                price = 112.50,
                changePercent = 4.20,
                purificationRate = "0.000000",
                isKmi30 = false,
                hasDividend = true
            ),
            ShariahStock(
                symbol = "UNITY",
                code = "1014",
                companyName = "Unity Foods Limited",
                sector = "Food & Personal Care",
                price = 28.40,
                changePercent = 1.80,
                purificationRate = "0.000000",
                isKmi30 = false,
                hasDividend = false
            )
        )
    }
}
