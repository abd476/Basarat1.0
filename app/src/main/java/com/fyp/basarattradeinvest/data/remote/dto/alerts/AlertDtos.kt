package com.fyp.basarattradeinvest.data.remote.dto.alerts

import com.google.gson.annotations.SerializedName

/**
 * Exact OpenAPI schema for AlertRuleResponse
 * Returned by GET /api/v1/alerts/rules, POST /api/v1/alerts/rules, PATCH /api/v1/alerts/rules/{rule_id}
 */
data class AlertRuleResponseDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("stock_id")
    val stockId: String? = null,

    @SerializedName("condition")
    val condition: String,

    @SerializedName("threshold")
    val threshold: Double,

    @SerializedName("is_active")
    val isActive: Boolean,

    @SerializedName("created_at")
    val createdAt: String
)

/**
 * Exact OpenAPI schema for AlertRuleCreate
 * Used by POST /api/v1/alerts/rules
 */
data class AlertRuleCreateDto(
    @SerializedName("stock_id")
    val stockId: String? = null,

    @SerializedName("condition")
    val condition: String,

    @SerializedName("threshold")
    val threshold: Double
)

/**
 * Exact OpenAPI schema for AlertRuleUpdate
 * Used by PATCH /api/v1/alerts/rules/{rule_id}
 */
data class AlertRuleUpdateDto(
    @SerializedName("stock_id")
    val stockId: String? = null,

    @SerializedName("condition")
    val condition: String? = null,

    @SerializedName("threshold")
    val threshold: Double? = null,

    @SerializedName("is_active")
    val isActive: Boolean? = null
)

/**
 * Exact OpenAPI schema for AlertResponse
 * Returned by GET /api/v1/alerts
 */
data class AlertResponseDto(
    @SerializedName("id")
    val id: String,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("rule_id")
    val ruleId: String? = null,

    @SerializedName("title")
    val title: String,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("is_read")
    val isRead: Boolean,

    @SerializedName("created_at")
    val createdAt: String
)
