package com.fyp.basarattradeinvest.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.NewsApiService
import com.fyp.basarattradeinvest.data.repository.AlertsRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.MarketRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.NewsRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.UsersRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.MarketIndex
import com.fyp.basarattradeinvest.domain.model.SymbolFilterType
import com.fyp.basarattradeinvest.domain.repository.AlertsRepository
import com.fyp.basarattradeinvest.domain.repository.MarketRepository
import com.fyp.basarattradeinvest.domain.repository.NewsRepository
import com.fyp.basarattradeinvest.domain.repository.UsersRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val marketRepository: MarketRepository = MarketRepositoryImpl(),
    private val newsRepository: NewsRepository = NewsRepositoryImpl(),
    private val usersRepository: UsersRepository = UsersRepositoryImpl(),
    private val alertsRepository: AlertsRepository = AlertsRepositoryImpl(),
    private val newsApiService: NewsApiService = NetworkClient.newsApiService,
    private val autoLoad: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentMoversTab: MoversTab = MoversTab.GAINERS
    private var currentTimeframe: String = "1D"
    private var lastSuccessfulFetchAt: Long = 0L
    private val freshnessWindowMs = 5 * 60 * 1000L

    init {
        if (autoLoad) {
            loadDashboard(forceLoad = false)
        }
    }

    private fun currentSuccessState(): HomeUiState.Success {
        return (_uiState.value as? HomeUiState.Success)
            ?: HomeUiState.Success(
                userName = "Investor",
                marketStatus = "Loading market data...",
                isMarketOpen = false,
                selectedTimeframe = currentTimeframe,
                selectedMoversTab = currentMoversTab
            )
    }

    private fun applyPartialState(update: HomeUiState.Success.() -> HomeUiState.Success) {
        val current = currentSuccessState()
        _uiState.value = update(current)
    }

    fun ensureLoaded() {
        val state = _uiState.value
        val shouldLoad = state !is HomeUiState.Success || stateHasExpired()
        if (shouldLoad) {
            loadDashboard(forceLoad = false)
        }
    }

    private fun stateHasExpired(): Boolean {
        return System.currentTimeMillis() - lastSuccessfulFetchAt > freshnessWindowMs
    }

    fun loadDashboard(isRefresh: Boolean = false, forceLoad: Boolean = isRefresh) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val hasUsableData = currentState is HomeUiState.Success
            val shouldSkip = !forceLoad && hasUsableData && !stateHasExpired()

            if (shouldSkip) return@launch
            if (!forceLoad && !hasUsableData) {
                _uiState.value = HomeUiState.Loading
            }

            try {
                coroutineScope {
                    val userDeferred = async { usersRepository.getCurrentUser() }
                    val indicesDeferred = async { marketRepository.getMarketIndices() }
                    val gainersDeferred = async { marketRepository.getTopGainers(5) }
                    val losersDeferred = async { marketRepository.getTopLosers(5) }
                    val spikesDeferred = async { marketRepository.getVolumeSpikes(5) }
                    val watchlistDeferred = async { marketRepository.getTopSymbols(SymbolFilterType.WATCHLIST) }
                    val newsDeferred = async { newsRepository.getNewsFeed(limit = 3) }
                    val alertsDeferred = async { alertsRepository.getAlerts(limit = 5, unreadOnly = true) }
                    val marketStatusDeferred = async {
                        try {
                            newsApiService.getMarketStatus()
                        } catch (e: Exception) {
                            null
                        }
                    }

                    val userRes = userDeferred.await()
                    val user = userRes.getOrNull()
                    val name = user?.fullName?.takeIf { it.isNotBlank() }
                        ?: user?.username?.takeIf { it.isNotBlank() }
                        ?: "Investor"
                    applyPartialState { copy(userName = name) }

                    lastSuccessfulFetchAt = System.currentTimeMillis()

                    val statusResponse = marketStatusDeferred.await()
                    val statusJson = statusResponse?.takeIf { it.isSuccessful }?.body()
                    val rawStatus = statusJson?.get("status")?.asString ?: "closed"
                    val isWeekend = statusJson?.get("is_weekend")?.asBoolean ?: false
                    val isOpen = rawStatus.equals("open", ignoreCase = true)
                    val statusLabel = when {
                        isOpen -> "Market Open"
                        isWeekend -> "Market Closed (Weekend)"
                        else -> "Market Closed"
                    }
                    applyPartialState { copy(marketStatus = statusLabel, isMarketOpen = isOpen) }

                    val indicesRes = indicesDeferred.await()
                    val indices = indicesRes.getOrNull() ?: emptyList()
                    val kse100 = indices.firstOrNull {
                        it.code.equals("KSE100", ignoreCase = true) ||
                            it.name.contains("100", ignoreCase = true)
                    } ?: indices.firstOrNull()
                    applyPartialState {
                        copy(
                            marketIndices = indices,
                            selectedBenchmarkIndex = kse100,
                            selectedTimeframe = currentTimeframe
                        )
                    }

                    val gainersRes = gainersDeferred.await()
                    val losersRes = losersDeferred.await()
                    val spikesRes = spikesDeferred.await()
                    val gainers = gainersRes.getOrNull() ?: emptyList()
                    val losers = losersRes.getOrNull() ?: emptyList()
                    val volumeSpikes = spikesRes.getOrNull() ?: emptyList()
                    applyPartialState {
                        copy(
                            topGainers = gainers,
                            topLosers = losers,
                            volumeSpikes = volumeSpikes,
                            selectedMoversTab = currentMoversTab
                        )
                    }

                    val watchlistRes = watchlistDeferred.await()
                    val watchlist = watchlistRes.getOrNull() ?: emptyList()
                    applyPartialState { copy(watchlistStocks = watchlist) }

                    val newsRes = newsDeferred.await()
                    val news = newsRes.getOrNull()?.articles?.take(3) ?: emptyList()
                    applyPartialState { copy(recentNews = news) }

                    val alertsRes = alertsDeferred.await()
                    val unreadAlerts = alertsRes.getOrNull() ?: emptyList()
                    applyPartialState {
                        copy(
                            activeAlertsCount = unreadAlerts.size,
                            unresolvedAlerts = unreadAlerts.take(2)
                        )
                    }

                    lastSuccessfulFetchAt = System.currentTimeMillis()
                }
            } catch (e: Exception) {
                val current = _uiState.value
                if (current !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Error(
                        e.message ?: "Unable to load market data. Please pull down to refresh."
                    )
                }
            }
        }
    }

    fun selectMoversTab(tab: MoversTab) {
        if (currentMoversTab == tab) return
        currentMoversTab = tab
        val current = _uiState.value
        if (current is HomeUiState.Success) {
            _uiState.value = current.copy(selectedMoversTab = tab)
        }
    }

    fun selectTimeframe(timeframe: String) {
        if (currentTimeframe == timeframe) return
        currentTimeframe = timeframe
        val current = _uiState.value
        if (current is HomeUiState.Success) {
            _uiState.value = current.copy(selectedTimeframe = timeframe)
        }
    }

    fun refresh() {
        loadDashboard(isRefresh = true)
    }
}
