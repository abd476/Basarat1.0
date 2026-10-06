package com.fyp.basarattradeinvest.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.core.session.AuthState
import com.fyp.basarattradeinvest.core.session.SessionManager
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.repository.AlertsRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.MarketRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.NewsRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.StockRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.UsersRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.market.MarketIndex
import com.fyp.basarattradeinvest.domain.model.market.MarketStock
import com.fyp.basarattradeinvest.domain.model.live.LiveQuote
import com.fyp.basarattradeinvest.domain.model.live.WsConnectionState
import com.fyp.basarattradeinvest.domain.repository.AlertsRepository
import com.fyp.basarattradeinvest.domain.repository.LiveMarketRepository
import com.fyp.basarattradeinvest.domain.repository.MarketRepository
import com.fyp.basarattradeinvest.domain.repository.NewsRepository
import com.fyp.basarattradeinvest.domain.repository.StockRepository
import com.fyp.basarattradeinvest.domain.repository.UsersRepository
import com.fyp.basarattradeinvest.domain.repository.WatchlistRepository
import com.fyp.basarattradeinvest.domain.usecase.market.BuildBenchmarkTrendUseCase
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val marketRepository: MarketRepository = MarketRepositoryImpl(),
    private val newsRepository: NewsRepository = NewsRepositoryImpl(),
    private val usersRepository: UsersRepository = UsersRepositoryImpl(),
    private val alertsRepository: AlertsRepository = AlertsRepositoryImpl(),
    private val watchlistRepository: WatchlistRepository = NetworkClient.watchlistRepository,
    private val liveMarketRepository: LiveMarketRepository = NetworkClient.liveMarketRepository,
    private val buildBenchmarkTrend: BuildBenchmarkTrendUseCase = BuildBenchmarkTrendUseCase(
        marketRepository = marketRepository,
        stockRepository = StockRepositoryImpl()
    ),
    private val autoLoad: Boolean = true
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private var currentMoversTab: MoversTab = MoversTab.GAINERS
    private var currentTimeframe: String = "1D"
    private var currentBenchmarkCode: String? = null
    private var benchmarkTrendJob: Job? = null
    private var lastSuccessfulFetchAt: Long = 0L
    private val freshnessWindowMs = 5 * 60 * 1000L
    private var isDashboardLoading = false
    private var isWatchlistLoading = false
    private var isLiveUpdatesActive = false
    private var accountGeneration = 0
    private var currentAccountKey: String? = accountKeyForCurrentSession()
    private var dashboardLoadJob: Job? = null
    private val pendingQuotes = mutableMapOf<String, LiveQuote>()
    private var quoteFlushJob: Job? = null
    private val pendingMutations = mutableListOf<Pair<Int, HomeUiState.Success.() -> HomeUiState.Success>>()
    private var mutationFlushJob: Job? = null

    init {
        // Quote ticks arrive one per symbol, so a single snapshot would otherwise rebuild the
        // watchlist and all three movers lists once per symbol. Accumulate and publish once
        // per flush window instead.
        viewModelScope.launch {
            liveMarketRepository.quoteUpdates.collect { quote ->
                pendingQuotes[quote.symbol.uppercase()] = quote
                if (quoteFlushJob?.isActive != true) {
                    quoteFlushJob = viewModelScope.launch {
                        delay(QUOTE_FLUSH_INTERVAL_MS)
                        applyPendingQuotes()
                    }
                }
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
                quoteFlushJob?.cancel()
                quoteFlushJob = null
                pendingQuotes.clear()
                mutationFlushJob?.cancel()
                mutationFlushJob = null
                pendingMutations.clear()
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

    private fun applyPendingQuotes() {
        quoteFlushJob = null
        if (pendingQuotes.isEmpty()) return
        val batch = pendingQuotes.toMap()
        pendingQuotes.clear()
        val current = _uiState.value as? HomeUiState.Success ?: return

        fun updateStocks(stocks: List<MarketStock>) = stocks.map { stock ->
            val quote = batch[stock.symbol.uppercase()] ?: return@map stock
            stock.copy(
                price = quote.current ?: stock.price,
                change = quote.change ?: stock.change,
                changePercent = quote.changePct ?: stock.changePercent,
                volume = quote.volume?.toString() ?: stock.volume
            )
        }

        val watchlist = current.watchlist as? WatchlistUiState.Success
        val updatedWatchlist = watchlist?.let { success ->
            WatchlistUiState.Success(
                success.watchlist.copy(items = success.watchlist.items.map { item ->
                    val quote = batch[item.symbol.uppercase()] ?: return@map item
                    item.copy(
                        currentPrice = quote.current ?: item.currentPrice,
                        change = quote.change ?: item.change,
                        changePercent = quote.changePct ?: item.changePercent,
                        isStale = quote.isStale
                    )
                })
            )
        } ?: current.watchlist

        _uiState.value = current.copy(
            liveQuotes = current.liveQuotes.toMutableMap().apply { putAll(batch) },
            watchlist = updatedWatchlist,
            topGainers = updateStocks(current.topGainers),
            topLosers = updateStocks(current.topLosers),
            volumeSpikes = updateStocks(current.volumeSpikes)
        )
    }

    private fun currentSuccessState(): HomeUiState.Success {
        return (_uiState.value as? HomeUiState.Success)
            ?: HomeUiState.Success(
                userName = "Investor",
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
        // The nine parallel loads each want to publish; queue them and write the state once per
        // window so a dashboard load emits one state instead of nine fragment rebind passes.
        pendingMutations.add(generation to update)
        if (mutationFlushJob?.isActive != true) {
            mutationFlushJob = viewModelScope.launch {
                delay(PARTIAL_FLUSH_INTERVAL_MS)
                flushPendingMutations()
            }
        }
    }

    private fun flushPendingMutations() {
        mutationFlushJob = null
        if (pendingMutations.isEmpty()) return
        val mutations = pendingMutations.toList()
        pendingMutations.clear()
        val fresh = mutations.filter { it.first == accountGeneration }
        // Writing an empty fold would replace Loading with a synthetic Success and hide the
        // skeleton with no real data, so drop the batch entirely when the account changed.
        if (fresh.isEmpty()) return
        _uiState.value = fresh.fold(currentSuccessState()) { state, (_, update) -> state.update() }
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
        if (forceLoad) buildBenchmarkTrend.invalidateCache()
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
                        val session = liveMarketRepository.getMarketSessionStatus().getOrNull()
                        applyPartialState(loadGeneration) {
                            copy(
                                marketStatus = session?.label,
                                isMarketOpen = session?.isOpen == true
                            )
                        }
                    }

                    launch {
                        val indices = marketRepository.getMarketIndices().getOrNull() ?: emptyList()
                        val kse100 = indices.firstOrNull {
                            it.code.equals("KSE100", ignoreCase = true) ||
                                it.name.contains("100", ignoreCase = true)
                        } ?: indices.firstOrNull()
                        currentBenchmarkCode = kse100?.code
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
                mutationFlushJob?.cancel()
                flushPendingMutations()
                if (loadGeneration == accountGeneration) {
                    lastSuccessfulFetchAt = System.currentTimeMillis()
                    // Kept out of the scope above: the 100-request fan-out must not hold up
                    // the rest of the dashboard or leave the refresh spinner running.
                    loadBenchmarkTrend(currentTimeframe, loadGeneration)
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
            _uiState.value = current.copy(
                selectedTimeframe = timeframe,
                benchmarkTrend = BenchmarkTrendUiState.Loading
            )
        }
        loadBenchmarkTrend(timeframe, accountGeneration)
    }

    private fun loadBenchmarkTrend(timeframe: String, generation: Int) {
        val indexCode = currentBenchmarkCode
        if (indexCode == null) {
            applyPartialState(generation) { copy(benchmarkTrend = BenchmarkTrendUiState.Empty) }
            return
        }
        benchmarkTrendJob?.cancel()
        val requestGeneration = generation
        benchmarkTrendJob = viewModelScope.launch {
            val result = buildBenchmarkTrend(indexCode, timeframe)
            applyPartialState(requestGeneration) {
                copy(
                    benchmarkTrend = result.fold(
                        onSuccess = { BenchmarkTrendUiState.Success(it) },
                        onFailure = {
                            BenchmarkTrendUiState.Error(
                                it.message ?: "Unable to build the benchmark trend."
                            )
                        }
                    )
                )
            }
        }
    }

    fun refresh() {
        loadDashboard(isRefresh = true)
    }

    companion object {
        private const val QUOTE_FLUSH_INTERVAL_MS = 750L
        private const val PARTIAL_FLUSH_INTERVAL_MS = 120L
    }
}
