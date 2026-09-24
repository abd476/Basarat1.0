package com.fyp.basarattradeinvest.feature.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.AlertsRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.AlertItem
import com.fyp.basarattradeinvest.domain.model.AlertRule
import com.fyp.basarattradeinvest.domain.usecase.alerts.CreateAlertRuleUseCase
import com.fyp.basarattradeinvest.domain.usecase.alerts.DeleteAlertRuleUseCase
import com.fyp.basarattradeinvest.domain.usecase.alerts.GetAlertRulesUseCase
import com.fyp.basarattradeinvest.domain.usecase.alerts.GetAlertsUseCase
import com.fyp.basarattradeinvest.domain.usecase.alerts.MarkAlertReadUseCase
import com.fyp.basarattradeinvest.domain.usecase.alerts.UpdateAlertRuleUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlertsViewModel(
    private val getAlertRulesUseCase: GetAlertRulesUseCase = GetAlertRulesUseCase(AlertsRepositoryImpl()),
    private val createAlertRuleUseCase: CreateAlertRuleUseCase = CreateAlertRuleUseCase(AlertsRepositoryImpl()),
    private val updateAlertRuleUseCase: UpdateAlertRuleUseCase = UpdateAlertRuleUseCase(AlertsRepositoryImpl()),
    private val deleteAlertRuleUseCase: DeleteAlertRuleUseCase = DeleteAlertRuleUseCase(AlertsRepositoryImpl()),
    private val getAlertsUseCase: GetAlertsUseCase = GetAlertsUseCase(AlertsRepositoryImpl()),
    private val markAlertReadUseCase: MarkAlertReadUseCase = MarkAlertReadUseCase(AlertsRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlertsUiState>(AlertsUiState.Loading)
    val uiState: StateFlow<AlertsUiState> = _uiState.asStateFlow()

    private val _mutationState = MutableStateFlow<AlertMutationState>(AlertMutationState.Idle)
    val mutationState: StateFlow<AlertMutationState> = _mutationState.asStateFlow()

    private var currentTab: AlertsTab = AlertsTab.RULES
    private var unreadFilter: Boolean = false
    private var cachedRules: List<AlertRule> = emptyList()
    private var cachedAlerts: List<AlertItem> = emptyList()

    init {
        loadData()
    }

    fun loadData(isSwipeRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isSwipeRefresh && _uiState.value !is AlertsUiState.Success) {
                _uiState.value = AlertsUiState.Loading
            }

            // Fetch rules and alerts concurrently
            val rulesResult = getAlertRulesUseCase()
            val alertsResult = getAlertsUseCase(page = 1, limit = 50, unreadOnly = null)

            val rulesSuccess = rulesResult.isSuccess
            val alertsSuccess = alertsResult.isSuccess

            if (rulesSuccess && alertsSuccess) {
                cachedRules = rulesResult.getOrDefault(emptyList())
                cachedAlerts = alertsResult.getOrDefault(emptyList())
                emitSuccessOrEmpty()
            } else if (rulesSuccess) {
                cachedRules = rulesResult.getOrDefault(emptyList())
                emitSuccessOrEmpty()
            } else if (alertsSuccess) {
                cachedAlerts = alertsResult.getOrDefault(emptyList())
                emitSuccessOrEmpty()
            } else {
                val errorMsg = rulesResult.exceptionOrNull()?.message
                    ?: alertsResult.exceptionOrNull()?.message
                    ?: "Unable to load alerts. Please try again."
                _uiState.value = AlertsUiState.Error(errorMsg)
            }
        }
    }

    fun switchTab(tab: AlertsTab) {
        currentTab = tab
        emitSuccessOrEmpty()
    }

    fun toggleUnreadFilter(unreadOnly: Boolean) {
        unreadFilter = unreadOnly
        emitSuccessOrEmpty()
    }

    fun toggleRuleActive(rule: AlertRule, isActive: Boolean) {
        viewModelScope.launch {
            // Optimistic update in local list
            cachedRules = cachedRules.map {
                if (it.id == rule.id) it.copy(isActive = isActive) else it
            }
            emitSuccessOrEmpty()

            val result = updateAlertRuleUseCase(ruleId = rule.id, isActive = isActive)
            if (result.isSuccess) {
                val updated = result.getOrNull()
                if (updated != null) {
                    cachedRules = cachedRules.map {
                        if (it.id == updated.id) updated else it
                    }
                    emitSuccessOrEmpty()
                }
            } else {
                // Revert optimistic update
                cachedRules = cachedRules.map {
                    if (it.id == rule.id) it.copy(isActive = !isActive) else it
                }
                emitSuccessOrEmpty(
                    actionMessage = "Failed to update alert rule: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun createRule(stockId: String?, condition: String, threshold: Double) {
        viewModelScope.launch {
            _mutationState.value = AlertMutationState.Loading
            val cleanSymbol = stockId?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
            val result = createAlertRuleUseCase(
                stockId = cleanSymbol,
                condition = condition,
                threshold = threshold
            )
            if (result.isSuccess) {
                val newRule = result.getOrNull()
                if (newRule != null) {
                    cachedRules = listOf(newRule) + cachedRules
                }
                _mutationState.value = AlertMutationState.Success("Alert rule created successfully")
                emitSuccessOrEmpty(actionMessage = "Alert rule created")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to create alert rule"
                _mutationState.value = AlertMutationState.Error(error)
            }
        }
    }

    fun updateRule(ruleId: String, stockId: String?, condition: String?, threshold: Double?, isActive: Boolean?) {
        viewModelScope.launch {
            _mutationState.value = AlertMutationState.Loading
            val cleanSymbol = stockId?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
            val result = updateAlertRuleUseCase(
                ruleId = ruleId,
                stockId = cleanSymbol,
                condition = condition,
                threshold = threshold,
                isActive = isActive
            )
            if (result.isSuccess) {
                val updated = result.getOrNull()
                if (updated != null) {
                    cachedRules = cachedRules.map { if (it.id == updated.id) updated else it }
                }
                _mutationState.value = AlertMutationState.Success("Alert rule updated successfully")
                emitSuccessOrEmpty(actionMessage = "Alert rule updated")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to update alert rule"
                _mutationState.value = AlertMutationState.Error(error)
            }
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            val previousRules = cachedRules
            cachedRules = cachedRules.filter { it.id != ruleId }
            emitSuccessOrEmpty(actionMessage = "Alert rule deleted")

            val result = deleteAlertRuleUseCase(ruleId)
            if (result.isFailure) {
                // Revert if deletion failed
                cachedRules = previousRules
                emitSuccessOrEmpty(
                    actionMessage = "Failed to delete alert rule: ${result.exceptionOrNull()?.message ?: "Unknown error"}"
                )
            }
        }
    }

    fun markAlertAsRead(alertId: String) {
        viewModelScope.launch {
            // Optimistic update
            cachedAlerts = cachedAlerts.map {
                if (it.id == alertId) it.copy(isRead = true) else it
            }
            emitSuccessOrEmpty()

            markAlertReadUseCase(alertId)
        }
    }

    fun resetMutationState() {
        _mutationState.value = AlertMutationState.Idle
    }

    fun clearActionMessage() {
        val current = _uiState.value
        if (current is AlertsUiState.Success) {
            _uiState.value = current.copy(actionMessage = null)
        }
    }

    private fun emitSuccessOrEmpty(actionMessage: String? = null) {
        if (currentTab == AlertsTab.RULES && cachedRules.isEmpty()) {
            _uiState.value = AlertsUiState.Empty(
                tab = AlertsTab.RULES,
                message = "No alert rules configured yet. Create a price or volume alert to track market opportunities."
            )
        } else if (currentTab == AlertsTab.HISTORY && cachedAlerts.isEmpty()) {
            _uiState.value = AlertsUiState.Empty(
                tab = AlertsTab.HISTORY,
                message = "No alerts triggered yet. Configured alerts will notify you here when conditions are met."
            )
        } else {
            _uiState.value = AlertsUiState.Success(
                rules = cachedRules,
                alerts = cachedAlerts,
                selectedTab = currentTab,
                unreadOnlyFilter = unreadFilter,
                actionMessage = actionMessage
            )
        }
    }
}
