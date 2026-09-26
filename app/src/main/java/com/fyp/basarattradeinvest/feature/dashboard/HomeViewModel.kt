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
    private val newsApiService: NewsApiService = NetworkClient.newsApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentMoversTab: MoversTab = MoversTab.GAINERS
    private var currentTimeframe: String = "1D"

    init {
        loadDashboard()
    }

    fun loadDashboard(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh && _uiState.value !is HomeUiState.Success) {
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
                    val indicesRes = indicesDeferred.await()
                    val gainersRes = gainersDeferred.await()
                    val losersRes = losersDeferred.await()
                    val spikesRes = spikesDeferred.await()
                    val watchlistRes = watchlistDeferred.await()
                    val newsRes = newsDeferred.await()
                    val alertsRes = alertsDeferred.await()
                    val statusResponse = marketStatusDeferred.await()

                    // User name
                    val user = userRes.getOrNull()
                    val name = user?.fullName?.takeIf { it.isNotBlank() }
                        ?: user?.username?.takeIf { it.isNotBlank() }
                        ?: "Investor"

                    // Market Status
                    val statusJson = statusResponse?.takeIf { it.isSuccessful }?.body()
                    val rawStatus = statusJson?.get("status")?.asString ?: "closed"
                    val isWeekend = statusJson?.get("is_weekend")?.asBoolean ?: false
                    val isOpen = rawStatus.equals("open", ignoreCase = true)
                    val statusLabel = when {
                        isOpen -> "Market Open"
                        isWeekend -> "Market Closed (Weekend)"
                        else -> "Market Closed"
                    }

                    // Indices
                    val indices = indicesRes.getOrNull() ?: emptyList()
                    val kse100 = indices.firstOrNull {
                        it.code.equals("KSE100", ignoreCase = true) ||
                        it.name.contains("100", ignoreCase = true)
                    } ?: indices.firstOrNull()

                    // Movers
                    val gainers = gainersRes.getOrNull() ?: emptyList()
                    val losers = losersRes.getOrNull() ?: emptyList()
                    val volumeSpikes = spikesRes.getOrNull() ?: emptyList()

                    // Watchlist & News & Alerts
                    val watchlist = watchlistRes.getOrNull() ?: emptyList()
                    val news = newsRes.getOrNull()?.articles?.take(3) ?: emptyList()
                    val unreadAlerts = alertsRes.getOrNull() ?: emptyList()

                    _uiState.value = HomeUiState.Success(
                        userName = name,
                        marketStatus = statusLabel,
                        isMarketOpen = isOpen,
                        marketIndices = indices,
                        selectedBenchmarkIndex = kse100,
                        selectedTimeframe = currentTimeframe,
                        watchlistStocks = watchlist,
                        topGainers = gainers,
                        topLosers = losers,
                        volumeSpikes = volumeSpikes,
                        selectedMoversTab = currentMoversTab,
                        recentNews = news,
                        activeAlertsCount = unreadAlerts.size,
                        unresolvedAlerts = unreadAlerts.take(2)
                    )
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
