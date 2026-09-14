package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.ShariahStock

interface ShariahRepository {
    suspend fun getShariahCompliantStocks(sectorFilter: String? = null): Result<List<ShariahStock>>
}
