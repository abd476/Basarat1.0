package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.CorporateEvent
import com.fyp.basarattradeinvest.domain.model.NewsArticle

interface NewsRepository {
    suspend fun getNewsArticles(category: String = "All", query: String = ""): Result<List<NewsArticle>>
    suspend fun getCorporateEvents(category: String = "All", query: String = ""): Result<List<CorporateEvent>>
}
