package com.fyp.basarattradeinvest.feature.news

import com.fyp.basarattradeinvest.domain.model.CorporateEvent
import com.fyp.basarattradeinvest.domain.model.NewsArticle
import com.fyp.basarattradeinvest.domain.model.NewsTab

sealed interface NewsEventsUiState {
    data object Loading : NewsEventsUiState

    data class Success(
        val newsArticles: List<NewsArticle>,
        val corporateEvents: List<CorporateEvent>,
        val activeTab: NewsTab = NewsTab.MARKET_NEWS,
        val activeCategory: String = "All",
        val searchQuery: String = "",
        val nextCursor: String? = null,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false,
        val isRefreshing: Boolean = false,
        val marketStatusText: String? = null,
        val refreshMessage: String? = null
    ) : NewsEventsUiState

    data object Empty : NewsEventsUiState

    data class Error(val message: String) : NewsEventsUiState
}
