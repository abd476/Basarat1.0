package com.fyp.basarattradeinvest.domain.model

data class StockTrade(
    val time: String,
    val price: Double,
    val volume: Long,
    val type: String  // "BUY" or "SELL"
)
