package com.fyp.basarattradeinvest.domain.model

data class MarketIndex(
    val id: String,
    val name: String,
    val currentPoints: Double,
    val changePoints: Double,
    val changePercent: Double,
    val intradayPoints: List<Float>
)
