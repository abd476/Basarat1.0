package com.fyp.basarattradeinvest.data.remote.dto.shariah

import com.google.gson.annotations.SerializedName

/**
 * Data Transfer Objects for Shariah Module.
 * Authoritative schema matches backend OpenAPI contract at /api/v1/shariah/
 */

data class ShariahKmi30ResponseDto(
    @SerializedName("index")
    val index: String? = "KMI-30",

    @SerializedName("total_constituents")
    val totalConstituents: Int? = null,

    @SerializedName("constituents")
    val constituents: List<Kmi30ConstituentDto> = emptyList()
)

data class Kmi30ConstituentDto(
    @SerializedName("symbol")
    val symbol: String,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("ldcp")
    val ldcp: Double? = null,

    @SerializedName("current")
    val current: Double? = null,

    @SerializedName("change")
    val change: Double? = null,

    @SerializedName("change_pct")
    val changePct: Double? = null,

    @SerializedName("weight_pct")
    val weightPct: Double? = null,

    @SerializedName("index_points")
    val indexPoints: Double? = null,

    @SerializedName("volume")
    val volume: Long? = null,

    @SerializedName("freefloat_m")
    val freefloatM: Double? = null,

    @SerializedName("market_cap_m")
    val marketCapM: Double? = null,

    @SerializedName("sector")
    val sector: String? = null,

    @SerializedName("purification_rate")
    val purificationRate: Double? = null,

    @SerializedName("is_shariah_compliant")
    val isShariahCompliant: Boolean? = true
)

data class ShariahScreeningResponseDto(
    @SerializedName("symbol")
    val symbol: String,

    @SerializedName("is_shariah_compliant")
    val isShariahCompliant: Boolean,

    @SerializedName("overall_score")
    val overallScore: Double? = null,

    @SerializedName("screening_method")
    val screeningMethod: String? = null,

    @SerializedName("screened_at")
    val screenedAt: String? = null,

    @SerializedName("sector")
    val sector: String? = null,

    @SerializedName("purification_rate")
    val purificationRate: Double? = null,

    @SerializedName("compliance_summary")
    val complianceSummary: String? = null
)

data class ShariahCriteriaResponseDto(
    @SerializedName("symbol")
    val symbol: String,

    @SerializedName("is_shariah_compliant")
    val isShariahCompliant: Boolean = true,

    @SerializedName("criteria")
    val criteria: List<ShariahCriterionDto> = emptyList()
)

data class ShariahCriterionDto(
    @SerializedName("name")
    val name: String,

    @SerializedName("threshold")
    val threshold: Double,

    @SerializedName("value")
    val value: Double? = null,

    @SerializedName("passed")
    val passed: Boolean,

    @SerializedName("description")
    val description: String? = null
)

data class ShariahPurificationResponseDto(
    @SerializedName("symbol")
    val symbol: String,

    @SerializedName("holding_qty")
    val holdingQty: Int,

    @SerializedName("holding_value")
    val holdingValue: Double,

    @SerializedName("purification_amount")
    val purificationAmount: Double,

    @SerializedName("purification_rate")
    val purificationRate: Double,

    @SerializedName("notes")
    val notes: String? = null
)
