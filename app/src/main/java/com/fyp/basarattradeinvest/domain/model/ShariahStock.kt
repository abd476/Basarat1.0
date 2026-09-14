package com.fyp.basarattradeinvest.domain.model

data class ShariahStock(
    val symbol: String,
    val companyName: String,
    val sector: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val complianceScore: Int, // 0 to 100
    val isKmi30Index: Boolean,
    val debtRatio: Double,
    val purificationRate: Double
)
