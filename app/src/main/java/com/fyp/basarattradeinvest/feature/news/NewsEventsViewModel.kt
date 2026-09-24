package com.fyp.basarattradeinvest.feature.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.NewsRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.CorporateEvent
import com.fyp.basarattradeinvest.domain.model.NewsArticle
import com.fyp.basarattradeinvest.domain.model.NewsTab
import com.fyp.basarattradeinvest.domain.usecase.GetNewsAndEventsUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private var cachedNews: MutableList<NewsArticle> = mutableListOf()
    private var cachedEvents: List<CorporateEvent> = emptyList()

    private var nextCursor: String? = null
    private var hasMoreNews: Boolean = false
    private var isLoadingMore: Boolean = false
    private var marketStatusText: String? = null

    private var searchJob: Job? = null
    private var refreshJob: Job? = null

    init {
        loadInitialData()
    }

    fun loadInitialData() {
        loadData(tab = currentTab, category = currentCategory, query = searchQuery)
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

            // Fetch Market Status asynchronously
            val marketStatusResult = getNewsAndEventsUseCase.getMarketStatusText()
            marketStatusText = marketStatusResult.getOrNull()

            // Fetch News and Events concurrently
            val newsResult = getNewsAndEventsUseCase.getNewsFeed(
                category = currentCategory,
                query = searchQuery,
                cursor = null,
                limit = 20
            )
            val eventsResult = getNewsAndEventsUseCase.getEvents(
                category = currentCategory,
                query = searchQuery
            )

            if (newsResult.isSuccess || eventsResult.isSuccess) {
                val newsPage = newsResult.getOrNull()
                cachedNews.clear()
                if (newsPage != null) {
                    cachedNews.addAll(newsPage.articles)
                    nextCursor = newsPage.nextCursor
                    hasMoreNews = newsPage.hasMore
                } else {
                    nextCursor = null
                    hasMoreNews = false
                }

                cachedEvents = eventsResult.getOrDefault(emptyList())

                val activeListEmpty = when (currentTab) {
                    NewsTab.MARKET_NEWS -> cachedNews.isEmpty()
                    NewsTab.CORPORATE_EVENTS -> cachedEvents.isEmpty()
                }

                if (activeListEmpty) {
                    _uiState.value = NewsEventsUiState.Empty
                } else {
                    _uiState.value = NewsEventsUiState.Success(
                        newsArticles = cachedNews.toList(),
                        corporateEvents = cachedEvents,
                        activeTab = currentTab,
                        activeCategory = currentCategory,
                        searchQuery = searchQuery,
                        nextCursor = nextCursor,
                        hasMore = hasMoreNews,
                        isLoadingMore = false,
                        isRefreshing = false,
                        marketStatusText = marketStatusText
                    )
                }
            } else {
                val errorMsg = newsResult.exceptionOrNull()?.message
                    ?: eventsResult.exceptionOrNull()?.message
                    ?: "Unable to load PSX news and corporate intelligence."
                _uiState.value = NewsEventsUiState.Error(errorMsg)
            }
        }
    }

    fun loadNextPage() {
        if (currentTab != NewsTab.MARKET_NEWS) return
        if (isLoadingMore || !hasMoreNews || nextCursor.isNullOrBlank()) return

        isLoadingMore = true
        val currentState = _uiState.value
        if (currentState is NewsEventsUiState.Success) {
            _uiState.value = currentState.copy(isLoadingMore = true)
        }

        viewModelScope.launch {
            val cursorToUse = nextCursor
            val result = getNewsAndEventsUseCase.getNewsFeed(
                category = currentCategory,
                query = searchQuery,
                cursor = cursorToUse,
                limit = 20
            )

            isLoadingMore = false
            if (result.isSuccess) {
                val page = result.getOrNull()
                if (page != null) {
                    nextCursor = page.nextCursor
                    hasMoreNews = page.hasMore

                    // Append without duplicates
                    val existingIds = cachedNews.map { it.id }.toSet()
                    val newUnique = page.articles.filter { it.id !in existingIds }
                    cachedNews.addAll(newUnique)

                    _uiState.value = NewsEventsUiState.Success(
                        newsArticles = cachedNews.toList(),
                        corporateEvents = cachedEvents,
                        activeTab = currentTab,
                        activeCategory = currentCategory,
                        searchQuery = searchQuery,
                        nextCursor = nextCursor,
                        hasMore = hasMoreNews,
                        isLoadingMore = false,
                        isRefreshing = false,
                        marketStatusText = marketStatusText
                    )
                }
            } else {
                // Keep existing items, just reset loading more
                if (currentState is NewsEventsUiState.Success) {
                    _uiState.value = currentState.copy(isLoadingMore = false)
                }
            }
        }
    }

    fun refreshNews() {
        if (refreshJob?.isActive == true) return

        val currentState = _uiState.value
        if (currentState is NewsEventsUiState.Success) {
            _uiState.value = currentState.copy(isRefreshing = true, refreshMessage = "Refreshing...")
        }

        refreshJob = viewModelScope.launch {
            // 1. Trigger manual refresh
            val triggerResult = getNewsAndEventsUseCase.triggerRefresh(force = false)
            if (triggerResult.isSuccess) {
                // Poll status up to 4 times (delay 1.5s)
                var attempts = 0
                while (attempts < 4) {
                    delay(1500)
                    attempts++
                    val statusRes = getNewsAndEventsUseCase.getRefreshStatus()
                    if (statusRes.isSuccess) {
                        val state = statusRes.getOrNull()?.state
                        if (state == "idle" || state == "completed") {
                            break
                        }
                    }
                }
            }

            // 2. Reload latest feed without losing screen state
            val newsResult = getNewsAndEventsUseCase.getNewsFeed(
                category = currentCategory,
                query = searchQuery,
                cursor = null,
                limit = 20
            )
            val eventsResult = getNewsAndEventsUseCase.getEvents(
                category = currentCategory,
                query = searchQuery
            )

            if (newsResult.isSuccess) {
                val page = newsResult.getOrNull()
                cachedNews.clear()
                if (page != null) {
                    cachedNews.addAll(page.articles)
                    nextCursor = page.nextCursor
                    hasMoreNews = page.hasMore
                }
            }
            if (eventsResult.isSuccess) {
                cachedEvents = eventsResult.getOrDefault(cachedEvents)
            }

            val activeListEmpty = when (currentTab) {
                NewsTab.MARKET_NEWS -> cachedNews.isEmpty()
                NewsTab.CORPORATE_EVENTS -> cachedEvents.isEmpty()
            }

            if (activeListEmpty) {
                _uiState.value = NewsEventsUiState.Empty
            } else {
                _uiState.value = NewsEventsUiState.Success(
                    newsArticles = cachedNews.toList(),
                    corporateEvents = cachedEvents,
                    activeTab = currentTab,
                    activeCategory = currentCategory,
                    searchQuery = searchQuery,
                    nextCursor = nextCursor,
                    hasMore = hasMoreNews,
                    isLoadingMore = false,
                    isRefreshing = false,
                    marketStatusText = marketStatusText,
                    refreshMessage = "Updated successfully"
                )
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
                newsArticles = cachedNews.toList(),
                corporateEvents = cachedEvents,
                activeTab = currentTab,
                activeCategory = currentCategory,
                searchQuery = searchQuery,
                nextCursor = nextCursor,
                hasMore = hasMoreNews,
                isLoadingMore = false,
                isRefreshing = false,
                marketStatusText = marketStatusText
            )
        }
    }

    fun setCategory(category: String) {
        if (currentCategory.equals(category, ignoreCase = true)) return
        loadData(tab = currentTab, category = category, query = searchQuery)
    }

    fun onSearchQueryChanged(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(400) // Debounce typing
            if (searchQuery != query) {
                loadData(tab = currentTab, category = currentCategory, query = query)
            }
        }
    }

    fun retry() {
        loadData(tab = currentTab, category = currentCategory, query = searchQuery)
    }
}
