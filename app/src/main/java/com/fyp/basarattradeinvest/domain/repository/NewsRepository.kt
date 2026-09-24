package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.CorporateEvent
import com.fyp.basarattradeinvest.domain.model.NewsArticle
import com.fyp.basarattradeinvest.domain.model.NewsFeedPage
import com.fyp.basarattradeinvest.domain.model.RefreshResult
import com.fyp.basarattradeinvest.domain.model.RefreshStatus

interface NewsRepository {
    suspend fun getNewsFeed(
        category: String = "All",
        query: String = "",
        cursor: String? = null,
        limit: Int = 20
    ): Result<NewsFeedPage>

    suspend fun getNewsArticles(
        category: String = "All",
        query: String = ""
    ): Result<List<NewsArticle>>

    suspend fun getCorporateEvents(
        category: String = "All",
        query: String = "",
        symbol: String? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Result<List<CorporateEvent>>

    suspend fun getNewsArticle(articleId: String): Result<NewsArticle>

    suspend fun getStockNews(
        symbol: String,
        cursor: String? = null,
        limit: Int = 20
    ): Result<NewsFeedPage>

    suspend fun triggerManualRefresh(force: Boolean = false): Result<RefreshResult>

    suspend fun getRefreshStatus(): Result<RefreshStatus>

    suspend fun getMarketStatusText(): Result<String?>
}
