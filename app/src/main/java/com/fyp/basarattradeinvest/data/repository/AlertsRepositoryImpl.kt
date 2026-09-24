package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.core.network.NetworkErrorParser
import com.fyp.basarattradeinvest.data.mapper.AlertDtoMapper
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.AlertsApiService
import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertRuleCreateDto
import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertRuleUpdateDto
import com.fyp.basarattradeinvest.domain.model.AlertItem
import com.fyp.basarattradeinvest.domain.model.AlertRule
import com.fyp.basarattradeinvest.domain.repository.AlertsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AlertsRepositoryImpl(
    private val apiService: AlertsApiService = NetworkClient.alertsApiService
) : AlertsRepository {

    override suspend fun getAlertRules(): Result<List<AlertRule>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAlertRules()
            if (response.isSuccessful) {
                val dtoList = response.body() ?: emptyList()
                val domainList = dtoList.map { AlertDtoMapper.mapRuleDtoToDomain(it) }
                Result.success(domainList)
            } else {
                val errorMsg = NetworkErrorParser.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createAlertRule(
        stockId: String?,
        condition: String,
        threshold: Double
    ): Result<AlertRule> = withContext(Dispatchers.IO) {
        try {
            val request = AlertRuleCreateDto(
                stockId = stockId?.takeIf { it.isNotBlank() },
                condition = condition,
                threshold = threshold
            )
            val response = apiService.createAlertRule(request)
            if (response.isSuccessful && response.body() != null) {
                val domain = AlertDtoMapper.mapRuleDtoToDomain(response.body()!!)
                Result.success(domain)
            } else {
                val errorMsg = NetworkErrorParser.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAlertRule(
        ruleId: String,
        stockId: String?,
        condition: String?,
        threshold: Double?,
        isActive: Boolean?
    ): Result<AlertRule> = withContext(Dispatchers.IO) {
        try {
            val request = AlertRuleUpdateDto(
                stockId = stockId,
                condition = condition,
                threshold = threshold,
                isActive = isActive
            )
            val response = apiService.updateAlertRule(ruleId, request)
            if (response.isSuccessful && response.body() != null) {
                val domain = AlertDtoMapper.mapRuleDtoToDomain(response.body()!!)
                Result.success(domain)
            } else {
                val errorMsg = NetworkErrorParser.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAlertRule(ruleId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteAlertRule(ruleId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = NetworkErrorParser.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAlerts(
        page: Int?,
        limit: Int?,
        unreadOnly: Boolean?
    ): Result<List<AlertItem>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAlerts(
                page = page,
                limit = limit,
                unreadOnly = unreadOnly
            )
            if (response.isSuccessful) {
                val dtoList = response.body() ?: emptyList()
                val domainList = dtoList.map { AlertDtoMapper.mapAlertDtoToDomain(it) }
                Result.success(domainList)
            } else {
                val errorMsg = NetworkErrorParser.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAlertAsRead(alertId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.markAlertAsRead(alertId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                val errorMsg = NetworkErrorParser.parseError(response)
                Result.failure(Exception(errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
