package com.fyp.basarattradeinvest.data.remote.dto.stocks

import com.google.gson.annotations.SerializedName

data class PriceHistoryResponseDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("range") val range: String? = null,
    @SerializedName("bars") val bars: List<PriceBarDto> = emptyList()
)

data class PriceBarDto(
    @SerializedName("date") val date: String,
    @SerializedName("open") val open: Double,
    @SerializedName("high") val high: Double,
    @SerializedName("low") val low: Double,
    @SerializedName("close") val close: Double,
    @SerializedName("volume") val volume: Long
)
