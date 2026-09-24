package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.AlertItem
import com.fyp.basarattradeinvest.domain.model.AlertRule

interface AlertsRepository {

    /**
     * Get user's alert rules.
     */
    suspend fun getAlertRules(): Result<List<AlertRule>>

    /**
     * Create a new alert rule.
     */
    suspend fun createAlertRule(
        stockId: String?,
        condition: String,
        threshold: Double
    ): Result<AlertRule>

    /**
     * Update an existing alert rule.
     */
    suspend fun updateAlertRule(
        ruleId: String,
        stockId: String? = null,
        condition: String? = null,
        threshold: Double? = null,
        isActive: Boolean? = null
    ): Result<AlertRule>

    /**
     * Delete an alert rule.
     */
    suspend fun deleteAlertRule(ruleId: String): Result<Unit>

    /**
     * Get user's triggered alerts / notification history.
     */
    suspend fun getAlerts(
        page: Int? = 1,
        limit: Int? = 50,
        unreadOnly: Boolean? = null
    ): Result<List<AlertItem>>

    /**
     * Mark an alert as read.
     */
    suspend fun markAlertAsRead(alertId: String): Result<Unit>
}
