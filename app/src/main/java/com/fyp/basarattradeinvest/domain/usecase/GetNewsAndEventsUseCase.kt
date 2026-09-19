package com.fyp.basarattradeinvest.domain.usecase

import com.fyp.basarattradeinvest.domain.model.CorporateEvent
import com.fyp.basarattradeinvest.domain.model.NewsArticle
import com.fyp.basarattradeinvest.domain.repository.NewsRepository

class GetNewsAndEventsUseCase(
    private val repository: NewsRepository
) {
    suspend fun getNews(category: String = "All", query: String = ""): Result<List<NewsArticle>> {
        return repository.getNewsArticles(category, query)
    }

    suspend fun getEvents(category: String = "All", query: String = ""): Result<List<CorporateEvent>> {
        return repository.getCorporateEvents(category, query)
    }
}
