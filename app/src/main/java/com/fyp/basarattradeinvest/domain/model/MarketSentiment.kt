package com.fyp.basarattradeinvest.domain.model

data class MarketSentiment(
    val overallScore: Int, // 0 to 100
    val sentimentLabel: String, // e.g. "Bullish", "Neutral", "Bearish"
    val positivePercent: Int,
    val neutralPercent: Int,
    val negativePercent: Int,
    val newsCount: Int,
    val aiSummary: String
)
