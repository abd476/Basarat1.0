package com.fyp.basarattradeinvest.data.remote.dto.recommendations

import com.google.gson.annotations.SerializedName

data class RecommendationsListResponseDto(
    @SerializedName("count") val count: Int,
    @SerializedName("risk_profile") val riskProfile: String,
    @SerializedName("recommendations") val recommendations: List<RecommendationItemDto> = emptyList()
)

data class RecommendationItemDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("signal") val signal: String,
    @SerializedName("confidence") val confidence: Double,
    @SerializedName("composite_score") val compositeScore: Double,
    @SerializedName("target_price") val targetPrice: Double? = null,
    @SerializedName("stop_loss") val stopLoss: Double? = null,
    @SerializedName("summary") val summary: String
)
