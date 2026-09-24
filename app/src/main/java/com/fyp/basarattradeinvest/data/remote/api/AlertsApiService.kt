package com.fyp.basarattradeinvest.data.remote.api

import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertRuleCreateDto
import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertRuleResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertRuleUpdateDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit API interface for BASARAT Alerts & Alert Rules endpoints.
 * Source of truth: OpenAPI /api/v1/openapi.json
 */
interface AlertsApiService {

    /**
     * Get user's configured alert rules.
     */
    @GET("api/v1/alerts/rules")
    suspend fun getAlertRules(): Response<List<AlertRuleResponseDto>>

    /**
     * Create a new alert rule.
     */
    @POST("api/v1/alerts/rules")
    suspend fun createAlertRule(
        @Body request: AlertRuleCreateDto
    ): Response<AlertRuleResponseDto>

    /**
     * Partially update an alert rule (toggle active, update condition, threshold, or stock).
     */
    @PATCH("api/v1/alerts/rules/{rule_id}")
    suspend fun updateAlertRule(
        @Path("rule_id") ruleId: String,
        @Body request: AlertRuleUpdateDto
    ): Response<AlertRuleResponseDto>

    /**
     * Delete an alert rule.
     */
    @DELETE("api/v1/alerts/rules/{rule_id}")
    suspend fun deleteAlertRule(
        @Path("rule_id") ruleId: String
    ): Response<Unit>

    /**
     * Get user's triggered alerts / notification history.
     */
    @GET("api/v1/alerts")
    suspend fun getAlerts(
        @Query("page") page: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("unread_only") unreadOnly: Boolean? = null
    ): Response<List<AlertResponseDto>>

    /**
     * Mark a triggered alert as read.
     */
    @PATCH("api/v1/alerts/{alert_id}/read")
    suspend fun markAlertAsRead(
        @Path("alert_id") alertId: String
    ): Response<Unit>
}
