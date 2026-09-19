package com.fyp.basarattradeinvest.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.NewsRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.CorporateEvent
import com.fyp.basarattradeinvest.domain.model.NewsArticle
import com.fyp.basarattradeinvest.domain.model.NewsTab
import com.fyp.basarattradeinvest.domain.usecase.GetNewsAndEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewsEventsViewModel(
    private val getNewsAndEventsUseCase: GetNewsAndEventsUseCase = GetNewsAndEventsUseCase(NewsRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsEventsUiState>(NewsEventsUiState.Loading)
    val uiState: StateFlow<NewsEventsUiState> = _uiState.asStateFlow()

    private var currentTab: NewsTab = NewsTab.MARKET_NEWS
    private var currentCategory: String = "All"
    private var searchQuery: String = ""

    private var cachedNews: List<NewsArticle> = emptyList()
    private var cachedEvents: List<CorporateEvent> = emptyList()

    init {
        loadData()
    }

    fun loadData(
        tab: NewsTab = currentTab,
        category: String = currentCategory,
        query: String = searchQuery
    ) {
        currentTab = tab
        currentCategory = category
        searchQuery = query

        viewModelScope.launch {
            _uiState.value = NewsEventsUiState.Loading

            val newsResult = getNewsAndEventsUseCase.getNews(category = currentCategory, query = searchQuery)
            val eventsResult = getNewsAndEventsUseCase.getEvents(category = currentCategory, query = searchQuery)

            if (newsResult.isSuccess && eventsResult.isSuccess) {
                cachedNews = newsResult.getOrDefault(emptyList())
                cachedEvents = eventsResult.getOrDefault(emptyList())

                val activeListEmpty = when (currentTab) {
                    NewsTab.MARKET_NEWS -> cachedNews.isEmpty()
                    NewsTab.CORPORATE_EVENTS -> cachedEvents.isEmpty()
                }

                if (activeListEmpty) {
                    _uiState.value = NewsEventsUiState.Empty
                } else {
                    _uiState.value = NewsEventsUiState.Success(
                        newsArticles = cachedNews,
                        corporateEvents = cachedEvents,
                        activeTab = currentTab,
                        activeCategory = currentCategory,
                        searchQuery = searchQuery
                    )
                }
            } else {
                _uiState.value = NewsEventsUiState.Error("Unable to load PSX news and corporate intelligence.")
            }
        }
    }

    fun setTab(tab: NewsTab) {
        if (currentTab == tab && _uiState.value is NewsEventsUiState.Success) return
        currentTab = tab

        val activeListEmpty = when (currentTab) {
            NewsTab.MARKET_NEWS -> cachedNews.isEmpty()
            NewsTab.CORPORATE_EVENTS -> cachedEvents.isEmpty()
        }

        if (activeListEmpty && cachedNews.isEmpty() && cachedEvents.isEmpty()) {
            loadData(tab = currentTab)
        } else if (activeListEmpty) {
            _uiState.value = NewsEventsUiState.Empty
        } else {
            _uiState.value = NewsEventsUiState.Success(
                newsArticles = cachedNews,
                corporateEvents = cachedEvents,
                activeTab = currentTab,
                activeCategory = currentCategory,
                searchQuery = searchQuery
            )
        }
    }

    fun setCategory(category: String) {
        if (currentCategory.equals(category, ignoreCase = true)) return
        loadData(tab = currentTab, category = category, query = searchQuery)
    }

    fun onSearchQueryChanged(query: String) {
        if (searchQuery == query) return
        loadData(tab = currentTab, category = currentCategory, query = query)
    }

    fun retry() {
        loadData()
    }
}
