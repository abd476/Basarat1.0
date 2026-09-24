package com.fyp.basarattradeinvest.domain.usecase.alerts

import com.fyp.basarattradeinvest.domain.model.AlertItem
import com.fyp.basarattradeinvest.domain.model.AlertRule
import com.fyp.basarattradeinvest.domain.repository.AlertsRepository

class GetAlertRulesUseCase(private val repository: AlertsRepository) {
    suspend operator fun invoke(): Result<List<AlertRule>> {
        return repository.getAlertRules()
    }
}

class CreateAlertRuleUseCase(private val repository: AlertsRepository) {
    suspend operator fun invoke(
        stockId: String?,
        condition: String,
        threshold: Double
    ): Result<AlertRule> {
        return repository.createAlertRule(stockId, condition, threshold)
    }
}

class UpdateAlertRuleUseCase(private val repository: AlertsRepository) {
    suspend operator fun invoke(
        ruleId: String,
        stockId: String? = null,
        condition: String? = null,
        threshold: Double? = null,
        isActive: Boolean? = null
    ): Result<AlertRule> {
        return repository.updateAlertRule(ruleId, stockId, condition, threshold, isActive)
    }
}

class DeleteAlertRuleUseCase(private val repository: AlertsRepository) {
    suspend operator fun invoke(ruleId: String): Result<Unit> {
        return repository.deleteAlertRule(ruleId)
    }
}

class GetAlertsUseCase(private val repository: AlertsRepository) {
    suspend operator fun invoke(
        page: Int? = 1,
        limit: Int? = 50,
        unreadOnly: Boolean? = null
    ): Result<List<AlertItem>> {
        return repository.getAlerts(page, limit, unreadOnly)
    }
}

class MarkAlertReadUseCase(private val repository: AlertsRepository) {
    suspend operator fun invoke(alertId: String): Result<Unit> {
        return repository.markAlertAsRead(alertId)
    }
}
