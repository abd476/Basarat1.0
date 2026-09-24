package com.fyp.basarattradeinvest.alerts

import com.fyp.basarattradeinvest.data.mapper.AlertDtoMapper
import com.fyp.basarattradeinvest.data.remote.api.AlertsApiService
import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertRuleCreateDto
import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertRuleResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertRuleUpdateDto
import com.fyp.basarattradeinvest.data.repository.AlertsRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.AlertCondition
import com.fyp.basarattradeinvest.domain.usecase.alerts.CreateAlertRuleUseCase
import com.fyp.basarattradeinvest.domain.usecase.alerts.DeleteAlertRuleUseCase
import com.fyp.basarattradeinvest.domain.usecase.alerts.GetAlertRulesUseCase
import com.fyp.basarattradeinvest.domain.usecase.alerts.GetAlertsUseCase
import com.fyp.basarattradeinvest.domain.usecase.alerts.MarkAlertReadUseCase
import com.fyp.basarattradeinvest.domain.usecase.alerts.UpdateAlertRuleUseCase
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AlertsModuleUnitTest {

    private class FakeAlertsApiService : AlertsApiService {
        var shouldFail: Boolean = false
        val storedRules = mutableListOf<AlertRuleResponseDto>()
        val storedAlerts = mutableListOf<AlertResponseDto>()

        override suspend fun getAlertRules(): Response<List<AlertRuleResponseDto>> {
            if (shouldFail) {
                return Response.error(500, "{\"detail\":\"Internal server error\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            return Response.success(storedRules.toList())
        }

        override suspend fun createAlertRule(request: AlertRuleCreateDto): Response<AlertRuleResponseDto> {
            if (shouldFail) {
                return Response.error(422, "{\"detail\":\"Validation error\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val newRule = AlertRuleResponseDto(
                id = "rule-${storedRules.size + 1}",
                userId = "user-123",
                stockId = request.stockId,
                condition = request.condition,
                threshold = request.threshold,
                isActive = true,
                createdAt = "2026-09-24T10:00:00Z"
            )
            storedRules.add(0, newRule)
            return Response.success(201, newRule)
        }

        override suspend fun updateAlertRule(
            ruleId: String,
            request: AlertRuleUpdateDto
        ): Response<AlertRuleResponseDto> {
            if (shouldFail) {
                return Response.error(404, "{\"detail\":\"Rule not found\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val index = storedRules.indexOfFirst { it.id == ruleId }
            val existing = storedRules[index]
            val updated = existing.copy(
                stockId = request.stockId ?: existing.stockId,
                condition = request.condition ?: existing.condition,
                threshold = request.threshold ?: existing.threshold,
                isActive = request.isActive ?: existing.isActive
            )
            storedRules[index] = updated
            return Response.success(updated)
        }

        override suspend fun deleteAlertRule(ruleId: String): Response<Unit> {
            if (shouldFail) {
                return Response.error(404, "{\"detail\":\"Rule not found\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            storedRules.removeAll { it.id == ruleId }
            return Response.success(204, Unit)
        }

        override suspend fun getAlerts(
            page: Int?,
            limit: Int?,
            unreadOnly: Boolean?
        ): Response<List<AlertResponseDto>> {
            if (shouldFail) {
                return Response.error(500, "{\"detail\":\"Internal server error\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val filtered = if (unreadOnly == true) storedAlerts.filter { !it.isRead } else storedAlerts
            return Response.success(filtered)
        }

        override suspend fun markAlertAsRead(alertId: String): Response<Unit> {
            if (shouldFail) {
                return Response.error(404, "{\"detail\":\"Alert not found\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val index = storedAlerts.indexOfFirst { it.id == alertId }
            if (index != -1) {
                storedAlerts[index] = storedAlerts[index].copy(isRead = true)
            }
            return Response.success(204, Unit)
        }
    }

    private lateinit var fakeApiService: FakeAlertsApiService
    private lateinit var repository: AlertsRepositoryImpl
    private lateinit var getRulesUseCase: GetAlertRulesUseCase
    private lateinit var createRuleUseCase: CreateAlertRuleUseCase
    private lateinit var updateRuleUseCase: UpdateAlertRuleUseCase
    private lateinit var deleteRuleUseCase: DeleteAlertRuleUseCase
    private lateinit var getAlertsUseCase: GetAlertsUseCase
    private lateinit var markReadUseCase: MarkAlertReadUseCase

    @Before
    fun setUp() {
        fakeApiService = FakeAlertsApiService()
        repository = AlertsRepositoryImpl(fakeApiService)
        getRulesUseCase = GetAlertRulesUseCase(repository)
        createRuleUseCase = CreateAlertRuleUseCase(repository)
        updateRuleUseCase = UpdateAlertRuleUseCase(repository)
        deleteRuleUseCase = DeleteAlertRuleUseCase(repository)
        getAlertsUseCase = GetAlertsUseCase(repository)
        markReadUseCase = MarkAlertReadUseCase(repository)
    }

    @Test
    fun testAlertDtoMapper_mapRuleDtoToDomain() {
        val dto = AlertRuleResponseDto(
            id = "rule-1",
            userId = "user-1",
            stockId = "luck",
            condition = "price_above",
            threshold = 125.5,
            isActive = true,
            createdAt = "2026-09-24T10:00:00Z"
        )
        val domain = AlertDtoMapper.mapRuleDtoToDomain(dto)

        assertEquals("rule-1", domain.id)
        assertEquals("LUCK", domain.displayTarget)
        assertEquals("Price Rises Above", domain.formattedCondition)
        assertEquals("PKR 125.50", domain.formattedThreshold)
        assertTrue(domain.isActive)
    }

    @Test
    fun testAlertDtoMapper_mapAlertDtoToDomain() {
        val dto = AlertResponseDto(
            id = "alert-1",
            userId = "user-1",
            ruleId = "rule-1",
            title = "Target Hit",
            message = "LUCK reached 125.50",
            isRead = false,
            createdAt = "2026-09-24T10:00:00Z"
        )
        val domain = AlertDtoMapper.mapAlertDtoToDomain(dto)

        assertEquals("alert-1", domain.id)
        assertEquals("Target Hit", domain.title)
        assertFalse(domain.isRead)
        assertNotNull(domain.formattedTime)
    }

    @Test
    fun testAlertConditionEnum() {
        assertEquals(AlertCondition.PRICE_ABOVE, AlertCondition.fromKey("price_above"))
        assertEquals(AlertCondition.PRICE_BELOW, AlertCondition.fromKey("PRICE_BELOW"))
        assertEquals(AlertCondition.CHANGE_PERCENT_ABOVE, AlertCondition.fromKey("change_percent_above"))
        assertEquals(AlertCondition.VOLUME_ABOVE, AlertCondition.fromKey("volume_above"))
        assertEquals(AlertCondition.PRICE_ABOVE, AlertCondition.fromKey("unknown_condition"))
    }

    @Test
    fun testRepository_CRUD_Lifecycle() = runBlocking {
        // 1. Initial GET -> empty
        val initialRules = repository.getAlertRules()
        assertTrue(initialRules.isSuccess)
        assertTrue(initialRules.getOrNull()!!.isEmpty())

        // 2. CREATE
        val createResult = repository.createAlertRule(
            stockId = "LUCK",
            condition = "price_above",
            threshold = 125.5
        )
        assertTrue(createResult.isSuccess)
        val createdRule = createResult.getOrNull()!!
        assertEquals("rule-1", createdRule.id)
        assertEquals("LUCK", createdRule.stockSymbol)
        assertEquals(125.5, createdRule.threshold, 0.001)

        // 3. GET after CREATE
        val rulesAfterCreate = repository.getAlertRules()
        assertTrue(rulesAfterCreate.isSuccess)
        assertEquals(1, rulesAfterCreate.getOrNull()!!.size)

        // 4. UPDATE
        val updateResult = repository.updateAlertRule(
            ruleId = createdRule.id,
            threshold = 130.0,
            isActive = false
        )
        assertTrue(updateResult.isSuccess)
        val updatedRule = updateResult.getOrNull()!!
        assertEquals(130.0, updatedRule.threshold, 0.001)
        assertFalse(updatedRule.isActive)

        // 5. DELETE
        val deleteResult = repository.deleteAlertRule(createdRule.id)
        assertTrue(deleteResult.isSuccess)

        // 6. GET after DELETE
        val rulesAfterDelete = repository.getAlertRules()
        assertTrue(rulesAfterDelete.isSuccess)
        assertTrue(rulesAfterDelete.getOrNull()!!.isEmpty())
    }

    @Test
    fun testRepository_TriggeredAlerts_MarkRead() = runBlocking {
        fakeApiService.storedAlerts.add(
            AlertResponseDto(
                id = "alert-101",
                userId = "user-1",
                ruleId = "rule-1",
                title = "High Volume Alert",
                message = "OGDC crossed 1,000,000 volume",
                isRead = false,
                createdAt = "2026-09-24T10:00:00Z"
            )
        )

        // 1. GET alerts
        val alertsResult = repository.getAlerts()
        assertTrue(alertsResult.isSuccess)
        val list = alertsResult.getOrNull()!!
        assertEquals(1, list.size)
        assertFalse(list[0].isRead)

        // 2. Mark as read
        val markResult = repository.markAlertAsRead("alert-101")
        assertTrue(markResult.isSuccess)

        // 3. GET alerts after read
        val alertsAfterRead = repository.getAlerts()
        assertTrue(alertsAfterRead.isSuccess)
        assertTrue(alertsAfterRead.getOrNull()!![0].isRead)
    }

    @Test
    fun testUseCasesExecution() = runBlocking {
        // Create rule through use case
        val created = createRuleUseCase("SYS", "price_above", 450.0).getOrNull()
        assertNotNull(created)
        assertEquals("SYS", created?.stockSymbol)

        // Fetch through use case
        val rules = getRulesUseCase().getOrNull()
        assertEquals(1, rules?.size)

        // Update through usecase
        val updated = updateRuleUseCase(created!!.id, threshold = 460.0).getOrNull()
        assertEquals(460.0, updated?.threshold ?: 0.0, 0.001)

        // Delete through usecase
        val deleted = deleteRuleUseCase(created.id)
        assertTrue(deleted.isSuccess)

        val emptyRules = getRulesUseCase().getOrNull()
        assertTrue(emptyRules?.isEmpty() == true)
    }
}
