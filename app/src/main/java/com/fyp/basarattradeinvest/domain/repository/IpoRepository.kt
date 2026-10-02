package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.IpoCalendarEvent
import com.fyp.basarattradeinvest.domain.model.IpoItem
import com.fyp.basarattradeinvest.domain.model.IpoPerformanceSummary

interface IpoRepository {
    suspend fun getIpos(): Result<List<IpoItem>>
    suspend fun getIpoDetail(symbol: String): Result<IpoItem>
    suspend fun getIpoCalendar(): Result<List<IpoCalendarEvent>>
    suspend fun getIpoPerformance(): Result<IpoPerformanceSummary>
}
