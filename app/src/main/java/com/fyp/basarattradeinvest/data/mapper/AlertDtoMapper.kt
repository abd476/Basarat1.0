package com.fyp.basarattradeinvest.data.mapper

import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.alerts.AlertRuleResponseDto
import com.fyp.basarattradeinvest.domain.model.AlertItem
import com.fyp.basarattradeinvest.domain.model.AlertRule

object AlertDtoMapper {

    fun mapRuleDtoToDomain(dto: AlertRuleResponseDto): AlertRule {
        return AlertRule(
            id = dto.id,
            userId = dto.userId,
            stockSymbol = dto.stockId,
            condition = dto.condition,
            threshold = dto.threshold,
            isActive = dto.isActive,
            createdAt = dto.createdAt
        )
    }

    fun mapAlertDtoToDomain(dto: AlertResponseDto): AlertItem {
        return AlertItem(
            id = dto.id,
            userId = dto.userId,
            ruleId = dto.ruleId,
            title = dto.title,
            message = dto.message,
            isRead = dto.isRead,
            createdAt = dto.createdAt
        )
    }
}
