package com.fyp.basarattradeinvest.domain.model.shariah

data class Kmi30IndexSummary(
    val index: String,
    val totalConstituents: Int,
    val constituents: List<Kmi30Constituent>
)

data class Kmi30Constituent(
    val symbol: String,
    val name: String,
    val currentPrice: Double,
    val change: Double,
    val changePct: Double,
    val weightPct: Double,
    val sector: String,
    val isCompliant: Boolean,
    val purificationRate: String
)

data class StockShariahCompliance(
    val symbol: String,
    val isCompliant: Boolean,
    val overallScore: Double?,
    val screeningMethod: String,
    val screenedAt: String?,
    val sector: String,
    val purificationRate: Double?,
    val complianceSummary: String
)

data class StockShariahCriteria(
    val symbol: String,
    val isCompliant: Boolean,
    val criteria: List<ShariahCriterionItem>
)

data class ShariahCriterionItem(
    val name: String,
    val threshold: Double,
    val value: Double?,
    val passed: Boolean,
    val description: String
)

data class DividendPurificationResult(
    val symbol: String,
    val holdingQty: Int,
    val holdingValue: Double,
    val purificationAmount: Double,
    val purificationRate: Double,
    val notes: String?
)
