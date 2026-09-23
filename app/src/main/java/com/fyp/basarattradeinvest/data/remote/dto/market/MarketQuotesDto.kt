package com.fyp.basarattradeinvest.data.remote.dto.market

import com.google.gson.annotations.SerializedName

/**
 * Exact OpenAPI schema for MarketQuoteItem, GainersResponse, LosersResponse, VolumeSpikesResponse
 */
data class MarketQuoteItemDto(
    @SerializedName("symbol")
    val symbol: String?,
    @SerializedName("sector")
    val sector: String?,
    @SerializedName("ldcp")
    val ldcp: Double?,
    @SerializedName("open")
    val open: Double?,
    @SerializedName("high")
    val high: Double?,
    @SerializedName("low")
    val low: Double?,
    @SerializedName("current")
    val current: Double?,
    @SerializedName("change")
    val change: Double?,
    @SerializedName("change_pct")
    val changePct: Double?,
    @SerializedName("volume")
    val volume: Long?
)

data class GainersResponseDto(
    @SerializedName("gainers")
    val gainers: List<MarketQuoteItemDto>?
)

data class LosersResponseDto(
    @SerializedName("losers")
    val losers: List<MarketQuoteItemDto>?
)

data class VolumeSpikesResponseDto(
    @SerializedName("volume_spikes")
    val volumeSpikes: List<MarketQuoteItemDto>?
)
