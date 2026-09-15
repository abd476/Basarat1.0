package com.fyp.basarattradeinvest.domain.model

data class StockAnalysis(
    val symbol: String,
    val companyName: String,
    val price: Double,
    val changeAmount: Double,
    val changePercent: Double,
    val technical: TechnicalAnalysis,
    val fundamentals: FundamentalMetrics
)

data class TechnicalAnalysis(
    val summary: TechnicalSummary,
    val oscillators: List<TechnicalIndicator>,
    val movingAverages: List<TechnicalIndicator>,
    val pivotPoints: PivotPoints
)

data class TechnicalSummary(
    val overallSignal: TechnicalSignal,
    val buyCount: Int,
    val neutralCount: Int,
    val sellCount: Int,
    val totalCount: Int = buyCount + neutralCount + sellCount
)

data class TechnicalIndicator(
    val name: String,
    val description: String,
    val value: String,
    val signal: TechnicalSignal
)

enum class TechnicalSignal(val label: String) {
    STRONG_BUY("STRONG BUY"),
    BUY("BUY"),
    NEUTRAL("NEUTRAL"),
    SELL("SELL"),
    STRONG_SELL("STRONG SELL")
}

data class PivotPoints(
    val r3: Double,
    val r2: Double,
    val r1: Double,
    val pivot: Double,
    val s1: Double,
    val s2: Double,
    val s3: Double
)

data class FundamentalMetrics(
    // Valuation Multiples
    val peRatio: Double,
    val sectorPeAvg: Double,
    val forwardPe: Double,
    val pbRatio: Double,
    val evEbitda: Double,
    val pegRatio: Double,
    // Profitability & Returns
    val eps: Double,
    val roe: Double,
    val roa: Double,
    val dividendYield: Double,
    val netProfitMargin: Double,
    // Solvency & Financial Health
    val debtToEquity: Double,
    val currentRatio: Double,
    val quickRatio: Double,
    val interestCoverage: Double,
    // Annual Performance
    val revenueTTM: String,
    val revenueGrowthYoY: Double,
    val netIncomeTTM: String,
    val netIncomeGrowthYoY: Double,
    val freeCashFlowTTM: String
)
