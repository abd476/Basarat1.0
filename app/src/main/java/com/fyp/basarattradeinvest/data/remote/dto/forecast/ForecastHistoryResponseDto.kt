package com.fyp.basarattradeinvest.data.remote.dto.forecast

import com.google.gson.annotations.SerializedName

/**
 * Exact Swagger DTO for GET /api/v1/forecast/{symbol}/history
 */
data class ForecastHistoryResponseDto(
    @SerializedName("symbol")
    val symbol: String,

    @SerializedName("horizon")
    val horizon: String? = null,

    @SerializedName("count")
    val count: Int,

    @SerializedName("accuracy")
    val accuracy: Double? = null,

    @SerializedName("history")
    val history: List<ForecastHistoryItemDto> = emptyList()
)

data class ForecastHistoryItemDto(
    @SerializedName("predicted_at")
    val predictedAt: String,

    @SerializedName("predicted_direction")
    val predictedDirection: String,

    @SerializedName("probabilities")
    val probabilities: Map<String, Double>? = null,

    @SerializedName("confidence")
    val confidence: Double,

    @SerializedName("target_date")
    val targetDate: String,

    @SerializedName("actual")
    val actual: ForecastActualOutcomeDto? = null
)

data class ForecastActualOutcomeDto(
    @SerializedName("direction")
    val direction: String? = null,

    @SerializedName("was_correct")
    val wasCorrect: Boolean? = null
)
