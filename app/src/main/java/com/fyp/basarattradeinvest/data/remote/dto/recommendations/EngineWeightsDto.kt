package com.fyp.basarattradeinvest.data.remote.dto.recommendations

import com.google.gson.annotations.SerializedName

data class EngineWeightsResponseDto(
    @SerializedName("gru_weight") val gruWeight: Double,
    @SerializedName("technical_weight") val technicalWeight: Double,
    @SerializedName("fundamental_weight") val fundamentalWeight: Double
)

data class EngineWeightsRequestDto(
    @SerializedName("gru_weight") val gruWeight: Double = 0.33,
    @SerializedName("technical_weight") val technicalWeight: Double = 0.33,
    @SerializedName("fundamental_weight") val fundamentalWeight: Double = 0.34
)
