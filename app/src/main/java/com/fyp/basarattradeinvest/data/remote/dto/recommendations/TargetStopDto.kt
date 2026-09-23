package com.fyp.basarattradeinvest.data.remote.dto.recommendations

import com.google.gson.annotations.SerializedName

data class TargetStopResponseDto(
    @SerializedName("symbol") val symbol: String,
    @SerializedName("current_price") val currentPrice: Double? = null,
    @SerializedName("target_price") val targetPrice: Double? = null,
    @SerializedName("stop_loss") val stopLoss: Double? = null,
    @SerializedName("expected_range") val expectedRange: Map<String, Any>? = null,
    @SerializedName("method") val method: String? = "atr_band",
    @SerializedName("atr_14") val atr14: Double? = null,
    @SerializedName("risk_tolerance") val riskTolerance: String? = "moderate",
    @SerializedName("upside_pct") val upsidePct: Double? = null,
    @SerializedName("downside_pct") val downsidePct: Double? = null
)
