package com.fyp.basarattradeinvest.domain.model

data class ShariahStock(
    val symbol: String,
    val code: String,
    val companyName: String,
    val sector: String,
    val price: Double,
    val changePercent: Double,
    val purificationRate: String,
    val isKmi30: Boolean = true,
    val hasDividend: Boolean = true
)
