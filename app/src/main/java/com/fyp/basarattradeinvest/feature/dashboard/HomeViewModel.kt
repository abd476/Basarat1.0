package com.fyp.basarattradeinvest.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.core.session.AuthState
import com.fyp.basarattradeinvest.core.session.SessionManager
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.NewsApiService
import com.fyp.basarattradeinvest.data.repository.AlertsRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.MarketRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.NewsRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.UsersRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.MarketIndex
import com.fyp.basarattradeinvest.domain.model.MarketStock
import com.fyp.basarattradeinvest.domain.model.live.LiveQuote
import com.fyp.basarattradeinvest.domain.model.live.WsConnectionState
import com.fyp.basarattradeinvest.domain.repository.AlertsRepository
import com.fyp.basarattradeinvest.domain.repository.LiveMarketRepository
import com.fyp.basarattradeinvest.domain.repository.MarketRepository
import com.fyp.basarattradeinvest.domain.repository.NewsRepository
import com.fyp.basarattradeinvest.domain.repository.UsersRepository
import com.fyp.basarattradeinvest.domain.repository.WatchlistRepository
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    private val watchlistRepository: WatchlistRepository = NetworkClient.watchlistRepository,
    private val liveMarketRepository: LiveMarketRepository = NetworkClient.liveMarketRepository,
    private val autoLoad: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentMoversTab: MoversTab = MoversTab.GAINERS
    private var currentTimeframe: String = "1D"
    private var lastSuccessfulFetchAt: Long = 0L
    private val freshnessWindowMs = 5 * 60 * 1000L
    private var isDashboardLoading = false
    private var isWatchlistLoading = false
    private var isLiveUpdatesActive = false
    private var accountGeneration = 0
    private var currentAccountKey: String? = accountKeyForCurrentSession()
    private var dashboardLoadJob: Job? = null

    init {
        // Collect live quote ticks and merge into current Success state
        viewModelScope.launch {
            liveMarketRepository.quoteUpdates.collect { quote ->
                val current = _uiState.value as? HomeUiState.Success ?: return@collect
                val updated = current.liveQuotes.toMutableMap()
                updated[quote.symbol.uppercase()] = quote
                fun updateStocks(stocks: List<MarketStock>) = stocks.map { stock ->
                    if (!stock.symbol.equals(quote.symbol, ignoreCase = true)) stock
                    else stock.copy(
                        price = quote.current ?: stock.price,
                        change = quote.change ?: stock.change,
                        changePercent = quote.changePct ?: stock.changePercent,
                        volume = quote.volume?.toString() ?: stock.volume
                    )
                }
                val watchlist = current.watchlist as? WatchlistUiState.Success
                val updatedWatchlist = watchlist?.let { success ->
                    WatchlistUiState.Success(success.watchlist.copy(items = success.watchlist.items.map { item ->
                        if (!item.symbol.equals(quote.symbol, ignoreCase = true)) item
                        else item.copy(
                            currentPrice = quote.current ?: item.currentPrice,
                            change = quote.change ?: item.change,
                            changePercent = quote.changePct ?: item.changePercent,
                            isStale = quote.isStale
                        )
                    }))
                } ?: current.watchlist
                _uiState.value = current.copy(
                    liveQuotes = updated,
                    watchlist = updatedWatchlist,
                    topGainers = updateStocks(current.topGainers),
                    topLosers = updateStocks(current.topLosers),
                    volumeSpikes = updateStocks(current.volumeSpikes)
                )
            }
        }

        // Collect WebSocket connection state
        viewModelScope.launch {
            liveMarketRepository.connectionState.collect { state ->
                val current = _uiState.value as? HomeUiState.Success ?: return@collect
                _uiState.value = current.copy(wsConnectionState = state)
            }
        }

        // Reflect live watchlist updates from WatchlistRepository
        viewModelScope.launch {
            watchlistRepository.watchlistUpdates.collect { watchlist ->
                val generation = accountGeneration
                val mergedWatchlist = mergeCachedQuotes(watchlist)
                val current = _uiState.value as? HomeUiState.Success ?: return@collect
                if (generation != accountGeneration) return@collect
                _uiState.value = current.copy(
                    watchlist = if (mergedWatchlist.items.isEmpty()) WatchlistUiState.Empty
                    else WatchlistUiState.Success(mergedWatchlist)
                )
                updateWatchlistSubscriptions(mergedWatchlist.items.map { it.symbol })
            }
        }

        viewModelScope.launch {
            SessionManager.authState.collect { authState ->
                val nextAccountKey = accountKeyFor(authState)
                if (nextAccountKey == currentAccountKey) return@collect

                currentAccountKey = nextAccountKey
                accountGeneration++
                dashboardLoadJob?.cancel()
                dashboardLoadJob = null
                isDashboardLoading = false
                isWatchlistLoading = false
                lastSuccessfulFetchAt = 0L
                _uiState.value = HomeUiState.Loading
                updateWatchlistSubscriptions(emptyList())

                if (nextAccountKey != null) {
                    loadDashboard(forceLoad = true)
                }
            }
        }

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
                selectedMoversTab = currentMoversTab,
                liveQuotes = liveMarketRepository.quoteCache.value,
                wsConnectionState = liveMarketRepository.connectionState.value
            )
    }

    private fun applyPartialState(
        generation: Int = accountGeneration,
        update: HomeUiState.Success.() -> HomeUiState.Success
    ) {
        if (generation != accountGeneration) return
        val current = currentSuccessState()
        _uiState.value = update(current)
    }

    private fun accountKeyForCurrentSession(): String? = accountKeyFor(SessionManager.authState.value)

    private fun accountKeyFor(authState: AuthState): String? = (authState as? AuthState.Authenticated)
        ?.let {
            SessionManager.getCurrentUserId()?.takeIf(String::isNotBlank)
                ?: it.userEmail.takeIf(String::isNotBlank)
        }
        ?.lowercase()

    fun ensureLoaded() {
        val state = _uiState.value
        val shouldLoad = state !is HomeUiState.Success || stateHasExpired()
        if (shouldLoad) {
            loadDashboard(forceLoad = false)
        }
    }

    fun setLiveUpdatesActive(active: Boolean) {
        if (isLiveUpdatesActive == active) return
        isLiveUpdatesActive = active
        val watchlist = (_uiState.value as? HomeUiState.Success)?.watchlist
            as? WatchlistUiState.Success
        updateWatchlistSubscriptions(watchlist?.watchlist?.items?.map { it.symbol }.orEmpty())
    }

    private fun updateWatchlistSubscriptions(symbols: Collection<String>) {
        liveMarketRepository.setSubscriptions(
            owner = "home",
            symbols = if (isLiveUpdatesActive) symbols else emptyList()
        )
    }

    private fun applyCachedQuotes(stocks: List<MarketStock>): List<MarketStock> = stocks.map { stock ->
        liveMarketRepository.getCachedQuote(stock.symbol)?.let { quote ->
            stock.copy(
                price = quote.current ?: stock.price,
                change = quote.change ?: stock.change,
                changePercent = quote.changePct ?: stock.changePercent,
                volume = quote.volume?.toString() ?: stock.volume
            )
        } ?: stock
    }

    private fun mergeCachedQuotes(watchlist: com.fyp.basarattradeinvest.domain.model.watchlist.Watchlist) =
        watchlist.copy(items = watchlist.items.map { item ->
            val quote = liveMarketRepository.getCachedQuote(item.symbol) ?: return@map item
            item.copy(
                currentPrice = quote.current ?: item.currentPrice,
                change = quote.change ?: item.change,
                changePercent = quote.changePct ?: item.changePercent,
                isStale = quote.isStale
            )
        })

    private fun stateHasExpired(): Boolean {
        return System.currentTimeMillis() - lastSuccessfulFetchAt > freshnessWindowMs
    }

    fun loadDashboard(isRefresh: Boolean = false, forceLoad: Boolean = isRefresh) {
        if (currentAccountKey == null) {
            _uiState.value = HomeUiState.Loading
            return
        }
        if (isDashboardLoading) return
        val stateAtStart = _uiState.value
        val shouldSkip = !forceLoad && stateAtStart is HomeUiState.Success && !stateHasExpired()
        if (shouldSkip) return
        isDashboardLoading = true
        val loadGeneration = accountGeneration
        dashboardLoadJob = viewModelScope.launch {
            val hasUsableData = stateAtStart is HomeUiState.Success
            if (!forceLoad && !hasUsableData) {
                _uiState.value = HomeUiState.Loading
            }

            try {
                coroutineScope {
                    launch {
                        val user = usersRepository.getCurrentUser().getOrNull()
                        val name = user?.fullName?.takeIf { it.isNotBlank() }
                            ?: user?.username?.takeIf { it.isNotBlank() }
                            ?: "Investor"
                        applyPartialState(loadGeneration) { copy(userName = name) }
                    }

                    launch {
                        val statusResponse = try {
                            newsApiService.getMarketStatus()
                        } catch (e: Exception) {
                            null
                        }
                        val statusJson = statusResponse?.takeIf { it.isSuccessful }?.body()
                        val rawStatus = statusJson?.get("status")?.asString ?: "closed"
                        val isWeekend = statusJson?.get("is_weekend")?.asBoolean ?: false
                        val isOpen = rawStatus.equals("open", ignoreCase = true)
                        val statusLabel = when {
                            isOpen -> "Market Open"
                            isWeekend -> "Market Closed (Weekend)"
                            else -> "Market Closed"
                        }
                        applyPartialState(loadGeneration) { copy(marketStatus = statusLabel, isMarketOpen = isOpen) }
                    }

                    launch {
                        val indices = marketRepository.getMarketIndices().getOrNull() ?: emptyList()
                        val kse100 = indices.firstOrNull {
                            it.code.equals("KSE100", ignoreCase = true) ||
                                it.name.contains("100", ignoreCase = true)
                        } ?: indices.firstOrNull()
                        applyPartialState(loadGeneration) {
                            copy(
                                marketIndices = indices,
                                selectedBenchmarkIndex = kse100,
                                selectedTimeframe = currentTimeframe
                            )
                        }
                    }

                    launch {
                        val gainers = applyCachedQuotes(marketRepository.getTopGainers(5).getOrNull() ?: emptyList())
                        applyPartialState(loadGeneration) {
                            copy(topGainers = gainers, selectedMoversTab = currentMoversTab)
                        }
                    }

                    launch {
                        val losers = applyCachedQuotes(marketRepository.getTopLosers(5).getOrNull() ?: emptyList())
                        applyPartialState(loadGeneration) {
                            copy(topLosers = losers, selectedMoversTab = currentMoversTab)
                        }
                    }

                    launch {
                        val volumeSpikes = applyCachedQuotes(marketRepository.getVolumeSpikes(5).getOrNull() ?: emptyList())
                        applyPartialState(loadGeneration) {
                            copy(volumeSpikes = volumeSpikes, selectedMoversTab = currentMoversTab)
                        }
                    }

                    launch {
                        val watchlistRes = watchlistRepository.getDefaultWatchlist()
                        applyPartialState(loadGeneration) {
                            copy(watchlist = watchlistRes.fold(
                                onSuccess = { watchlist ->
                                    val mergedWatchlist = mergeCachedQuotes(watchlist)
                                    if (mergedWatchlist.items.isEmpty()) WatchlistUiState.Empty
                                    else WatchlistUiState.Success(mergedWatchlist)
                                },
                                onFailure = { error ->
                                    WatchlistUiState.Error(error.message ?: "Unable to load watchlist.")
                                }
                            ))
                        }
                        if (loadGeneration == accountGeneration) {
                            updateWatchlistSubscriptions(watchlistRes.getOrNull()?.items?.map { it.symbol }.orEmpty())
                        }
                    }

                    launch {
                        val newsRes = newsRepository.getNewsFeed(limit = 3)
                        val news = newsRes.getOrNull()?.articles?.take(3) ?: emptyList()
                        applyPartialState(loadGeneration) { copy(recentNews = news) }
                    }

                    launch {
                        val unreadAlerts = alertsRepository.getAlerts(limit = 5, unreadOnly = true).getOrNull() ?: emptyList()
                        applyPartialState(loadGeneration) {
                            copy(
                                activeAlertsCount = unreadAlerts.size,
                                unresolvedAlerts = unreadAlerts.take(2)
                            )
                        }
                    }
                }
                if (loadGeneration == accountGeneration) {
                    lastSuccessfulFetchAt = System.currentTimeMillis()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (e: Exception) {
                val current = _uiState.value
                if (loadGeneration == accountGeneration && current !is HomeUiState.Success) {
                    _uiState.value = HomeUiState.Error(
                        e.message ?: "Unable to load market data. Please pull down to refresh."
                    )
                }
            } finally {
                if (loadGeneration == accountGeneration) {
                    isDashboardLoading = false
                    dashboardLoadJob = null
                }
            }
        }
    }

    fun retryWatchlist() {
        if (currentAccountKey == null) return
        if (isWatchlistLoading) return
        val requestGeneration = accountGeneration
        isWatchlistLoading = true
        applyPartialState(requestGeneration) { copy(watchlist = WatchlistUiState.Loading) }
        viewModelScope.launch {
            try {
                val result = watchlistRepository.getDefaultWatchlist()
                applyPartialState(requestGeneration) {
                    copy(watchlist = result.fold(
                        onSuccess = {
                            val mergedWatchlist = mergeCachedQuotes(it)
                            if (mergedWatchlist.items.isEmpty()) WatchlistUiState.Empty
                            else WatchlistUiState.Success(mergedWatchlist)
                        },
                        onFailure = { WatchlistUiState.Error(it.message ?: "Unable to load watchlist.") }
                    ))
                }
            } finally {
                if (requestGeneration == accountGeneration) {
                    isWatchlistLoading = false
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
