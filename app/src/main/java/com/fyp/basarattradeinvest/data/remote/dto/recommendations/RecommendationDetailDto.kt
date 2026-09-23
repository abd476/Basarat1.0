package com.fyp.basarattradeinvest.data.remote.dto.recommendations

import com.google.gson.annotations.SerializedName

data class RecommendationDetailResponseDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("signal") val signal: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("composite_score") val compositeScore: Double,
    @SerializedName("signals") val signals: Map<String, Double> = emptyMap(),
    @SerializedName("target_price") val targetPrice: Double? = null,
    @SerializedName("stop_loss") val stopLoss: Double? = null,
    @SerializedName("expected_range") val expectedRange: Map<String, Any>? = null,
    @SerializedName("current_price") val currentPrice: Double? = null,
    @SerializedName("atr_14") val atr14: Double? = null,
    @SerializedName("reasoning") val reasoning: Map<String, Map<String, String>> = emptyMap(),
    @SerializedName("weights") val weights: Map<String, Double> = emptyMap()
)
