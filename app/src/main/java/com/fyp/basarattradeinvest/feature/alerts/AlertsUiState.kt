package com.fyp.basarattradeinvest.feature.alerts

import com.fyp.basarattradeinvest.domain.model.AlertItem
import com.fyp.basarattradeinvest.domain.model.AlertRule

enum class AlertsTab {
    RULES,
    HISTORY
}

sealed interface AlertsUiState {
    data object Loading : AlertsUiState

    data class Success(
        val rules: List<AlertRule> = emptyList(),
        val alerts: List<AlertItem> = emptyList(),
        val selectedTab: AlertsTab = AlertsTab.RULES,
        val unreadOnlyFilter: Boolean = false,
        val isRulesRefreshing: Boolean = false,
        val isAlertsRefreshing: Boolean = false,
        val actionMessage: String? = null
    ) : AlertsUiState {
        val filteredAlerts: List<AlertItem>
            get() = if (unreadOnlyFilter) alerts.filter { !it.isRead } else alerts

        val unreadCount: Int
            get() = alerts.count { !it.isRead }

        val activeRulesCount: Int
            get() = rules.count { it.isActive }
    }

    data class Empty(
        val tab: AlertsTab,
        val message: String
    ) : AlertsUiState

    data class Error(
        val message: String
    ) : AlertsUiState
}

sealed interface AlertMutationState {
    data object Idle : AlertMutationState
    data object Loading : AlertMutationState
    data class Success(val message: String) : AlertMutationState
    data class Error(val message: String) : AlertMutationState
}
