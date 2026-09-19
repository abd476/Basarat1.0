package com.fyp.basarattradeinvest.domain.model

data class NewsArticle(
    val id: String,
    val symbol: String,
    val hasDividend: Boolean = false,
    val source: String,
    val timestamp: String,
    val sentimentScore: Double,
    val sentimentLabel: String, // "Bullish", "Bearish", "Neutral"
    val headline: String,
    val snippet: String,
    val category: String = "All"
)

enum class NewsTab {
    MARKET_NEWS,
    CORPORATE_EVENTS
}
