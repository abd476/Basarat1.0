package com.fyp.basarattradeinvest.domain.model.recommendations

data class RecommendationDetail(
    val symbol: String,
    val signal: RecommendationSignal,
    val confidence: Double,
    val compositeScore: Double,
    val individualSignals: Map<String, Double>,
    val targetPrice: Double?,
    val stopLoss: Double?,
    val expectedRange: ExpectedPriceRange?,
    val currentPrice: Double?,
    val atr14: Double?,
    val reasoning: Map<String, Map<String, String>>,
    val weights: Map<String, Double>
)

data class ExpectedPriceRange(
    val low: Double,
    val high: Double,
    val method: String
)
