package com.fyp.basarattradeinvest.feature.markets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.core.session.AuthState
import com.fyp.basarattradeinvest.core.session.SessionManager
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.repository.MarketRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.IndexConstituent
import com.fyp.basarattradeinvest.domain.model.MarketOverview
import com.fyp.basarattradeinvest.domain.model.MarketStock
import com.fyp.basarattradeinvest.domain.model.SymbolFilterType
import com.fyp.basarattradeinvest.domain.model.live.LiveQuote
import com.fyp.basarattradeinvest.domain.model.live.WsConnectionState
import com.fyp.basarattradeinvest.domain.repository.LiveMarketRepository
import com.fyp.basarattradeinvest.domain.repository.MarketRepository
import com.fyp.basarattradeinvest.domain.usecase.GetMarketIndicesUseCase
import com.fyp.basarattradeinvest.domain.usecase.GetSectorPerformancesUseCase
import com.fyp.basarattradeinvest.domain.usecase.market.GetKmi30ConstituentsUseCase
import com.fyp.basarattradeinvest.domain.usecase.market.GetKse100ConstituentsUseCase
import com.fyp.basarattradeinvest.domain.usecase.market.GetKse30ConstituentsUseCase
import com.fyp.basarattradeinvest.domain.usecase.market.GetMarketSentimentOverviewUseCase
import com.fyp.basarattradeinvest.domain.usecase.market.GetTopGainersUseCase
import com.fyp.basarattradeinvest.domain.usecase.market.GetTopLosersUseCase
import com.fyp.basarattradeinvest.domain.usecase.market.GetVolumeSpikesUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MarketDashboardViewModel(
    private val repository: MarketRepository = MarketRepositoryImpl(),
    private val getMarketIndicesUseCase: GetMarketIndicesUseCase = GetMarketIndicesUseCase(repository),
    private val getMarketSentimentOverviewUseCase: GetMarketSentimentOverviewUseCase = GetMarketSentimentOverviewUseCase(repository),
    private val getTopGainersUseCase: GetTopGainersUseCase = GetTopGainersUseCase(repository),
    private val getTopLosersUseCase: GetTopLosersUseCase = GetTopLosersUseCase(repository),
    private val getVolumeSpikesUseCase: GetVolumeSpikesUseCase = GetVolumeSpikesUseCase(repository),
    private val getKse100ConstituentsUseCase: GetKse100ConstituentsUseCase = GetKse100ConstituentsUseCase(repository),
    private val getKse30ConstituentsUseCase: GetKse30ConstituentsUseCase = GetKse30ConstituentsUseCase(repository),
    private val getKmi30ConstituentsUseCase: GetKmi30ConstituentsUseCase = GetKmi30ConstituentsUseCase(repository),
    private val getSectorPerformancesUseCase: GetSectorPerformancesUseCase = GetSectorPerformancesUseCase(repository),
    private val liveMarketRepository: LiveMarketRepository = NetworkClient.liveMarketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MarketDashboardUiState>(MarketDashboardUiState.Loading)
    val uiState: StateFlow<MarketDashboardUiState> = _uiState.asStateFlow()

    private var currentFilter = SymbolFilterType.GAINERS
    private var currentConstituentIndex = "KSE-100"
    private var lastSuccessfulFetchAt: Long = 0L
    private val freshnessWindowMs = 5 * 60 * 1000L
    private var isMarketDataLoading = false
    private var isLiveUpdatesActive = false
    private var accountGeneration = 0
    private var currentAccountKey: String? = accountKeyForCurrentSession()
    private var marketDataJob: Job? = null

    init {
        // Collect live quote ticks → update liveQuotes in Success state
        viewModelScope.launch {
            liveMarketRepository.quoteUpdates.collect { quote ->
                val current = _uiState.value as? MarketDashboardUiState.Success ?: return@collect
                val updated = current.liveQuotes.toMutableMap()
                updated[quote.symbol.uppercase()] = quote
                val updatedTopSymbols = current.topSymbols.map { stock ->
                    if (!stock.symbol.equals(quote.symbol, ignoreCase = true)) stock
                    else stock.withQuote(quote)
                }
                val updatedConstituents = current.constituents.map { constituent ->
                    if (!constituent.symbol.equals(quote.symbol, ignoreCase = true)) constituent
                    else constituent.withQuote(quote)
                }
                _uiState.value = current.copy(
                    liveQuotes = updated,
                    topSymbols = updatedTopSymbols,
                    constituents = updatedConstituents
                )
            }
        }

        // Collect connection state → update wsConnectionState in Success state
        viewModelScope.launch {
            liveMarketRepository.connectionState.collect { connState ->
                val current = _uiState.value as? MarketDashboardUiState.Success ?: return@collect
                _uiState.value = current.copy(wsConnectionState = connState)
            }
        }

        viewModelScope.launch {
            SessionManager.authState.collect { authState ->
                val nextAccountKey = accountKeyFor(authState)
                if (nextAccountKey == currentAccountKey) return@collect

                currentAccountKey = nextAccountKey
                accountGeneration++
                marketDataJob?.cancel()
                marketDataJob = null
                isMarketDataLoading = false
                lastSuccessfulFetchAt = 0L
                currentFilter = SymbolFilterType.GAINERS
                _uiState.value = MarketDashboardUiState.Loading
                updateMarketSubscriptions()

                if (nextAccountKey != null) {
                    loadMarketData(isRefresh = true)
                }
            }
        }

        ensureLoaded()
    }

    fun ensureLoaded() {
        val state = _uiState.value
        val shouldLoad = state !is MarketDashboardUiState.Success || System.currentTimeMillis() - lastSuccessfulFetchAt > freshnessWindowMs
        if (shouldLoad) {
            loadMarketData(isRefresh = false)
        }
    }

    fun loadMarketData(isRefresh: Boolean = false) {
        if (isMarketDataLoading) return
        isMarketDataLoading = true
        val loadGeneration = accountGeneration
        marketDataJob = viewModelScope.launch {
            try {
                val currentState = _uiState.value
                val hasUsableData = currentState is MarketDashboardUiState.Success
                val shouldSkip = !isRefresh && hasUsableData && System.currentTimeMillis() - lastSuccessfulFetchAt <= freshnessWindowMs
                if (shouldSkip) return@launch
                if (!isRefresh && !hasUsableData) {
                    _uiState.value = MarketDashboardUiState.Loading
                }

                val indicesDeferred = async { getMarketIndicesUseCase() }
                val sentimentDeferred = async { getMarketSentimentOverviewUseCase() }
                val topSymbolsDeferred = async { fetchSymbolsForFilter(currentFilter) }
                val sectorsDeferred = async { getSectorPerformancesUseCase() }
                val constituentsDeferred = async { fetchConstituentsForIndex(currentConstituentIndex) }

                // Await only the above-the-fold data first so the dashboard paints as soon as
                // indices/sentiment/top symbols are ready. Sectors and constituents are
                // secondary and fill in progressively below.
                val indicesResult = indicesDeferred.await()
                val sentimentResult = sentimentDeferred.await()
                val topSymbolsResult = topSymbolsDeferred.await()
                if (loadGeneration != accountGeneration) return@launch

                if (indicesResult.isSuccess || sentimentResult.isSuccess) {
                    val indices = indicesResult.getOrDefault(emptyList())
                    val sentiment = sentimentResult.getOrNull()
                    val topSymbols = topSymbolsResult.getOrDefault(emptyList())

                    val upCount = sentiment?.advancing ?: 0
                    val downCount = sentiment?.declining ?: 0
                    val unchangedCount = sentiment?.unchanged ?: 0
                    val totalStocks = upCount + downCount + unchangedCount

                    val adRatioFormatted = String.format(Locale.US, "%.2fx", sentiment?.advanceDeclineRatio ?: 0.0)
                    val advancingFormatted = upCount.toString()
                    val decliningFormatted = downCount.toString()

                    val overview = MarketOverview(
                        tradedValue = adRatioFormatted,
                        tradedVolume = advancingFormatted,
                        totalTrades = decliningFormatted,
                        totalStocks = if (totalStocks > 0) totalStocks else 374,
                        upCount = upCount,
                        downCount = downCount,
                        unchangedCount = unchangedCount
                    )

                    val currentTime = SimpleDateFormat("HH:mm", Locale.US).format(Date())
                    lastSuccessfulFetchAt = System.currentTimeMillis()

                    if (sentiment != null) {
                        _uiState.value = MarketDashboardUiState.Success(
                            indices = indices,
                            overview = overview,
                            topSymbols = applyCachedMarketStocks(topSymbols),
                            selectedFilter = currentFilter,
                            sentiment = sentiment,
                            sectors = emptyList(),
                            selectedConstituentIndex = currentConstituentIndex,
                            constituents = emptyList(),
                            isConstituentsLoading = true,
                            lastUpdated = currentTime,
                            liveQuotes = liveMarketRepository.quoteCache.value,
                            wsConnectionState = liveMarketRepository.connectionState.value
                        )
                    } else {
                        _uiState.value = MarketDashboardUiState.Empty
                    }
                    updateMarketSubscriptions()

                    // Secondary sections: fill in once they arrive without blocking first paint.
                    val sectors = sectorsDeferred.await().getOrDefault(emptyList())
                    val constituents = constituentsDeferred.await().getOrDefault(emptyList())
                    if (loadGeneration != accountGeneration) return@launch
                    val stateAfterSecondary = _uiState.value
                    if (stateAfterSecondary is MarketDashboardUiState.Success) {
                        _uiState.value = stateAfterSecondary.copy(
                            sectors = sectors,
                            constituents = applyCachedConstituents(constituents),
                            isConstituentsLoading = false
                        )
                        updateMarketSubscriptions()
                    }
                } else {
                    val errorMsg = indicesResult.exceptionOrNull()?.message
                        ?: sentimentResult.exceptionOrNull()?.message
                        ?: "Unable to load market data. Please check connection."
                    _uiState.value = MarketDashboardUiState.Error(errorMsg)
                }
            } finally {
                if (loadGeneration == accountGeneration) {
                    isMarketDataLoading = false
                    marketDataJob = null
                }
            }
        }
    }

    fun refreshData() {
        loadMarketData(isRefresh = true)
    }

    fun setLiveUpdatesActive(active: Boolean) {
        if (isLiveUpdatesActive == active) return
        isLiveUpdatesActive = active
        updateMarketSubscriptions()
    }

    private fun updateMarketSubscriptions() {
        val state = _uiState.value as? MarketDashboardUiState.Success
        val symbols = if (isLiveUpdatesActive) {
            (state?.topSymbols.orEmpty().map { it.symbol } + state?.constituents.orEmpty().map { it.symbol }).toSet()
        } else {
            emptySet()
        }
        liveMarketRepository.setSubscriptions("markets", symbols)
    }

    fun setFilter(filterType: SymbolFilterType) {
        if (currentFilter == filterType && _uiState.value is MarketDashboardUiState.Success) return
        currentFilter = filterType

        // 1. Immediately update UI state so selectedFilter changes without delay
        val stateBefore = _uiState.value
        if (stateBefore is MarketDashboardUiState.Success) {
            _uiState.value = stateBefore.copy(selectedFilter = filterType)
        }

        viewModelScope.launch {
            val requestGeneration = accountGeneration
            val topSymbolsResult = fetchSymbolsForFilter(filterType)
            if (requestGeneration != accountGeneration || currentFilter != filterType) return@launch
            val currentState = _uiState.value
            if (currentState is MarketDashboardUiState.Success) {
                val symbols = topSymbolsResult.getOrDefault(emptyList())
                _uiState.value = currentState.copy(
                    topSymbols = symbols,
                    selectedFilter = filterType
                )
            }
            updateMarketSubscriptions()
        }
    }

    fun selectConstituentIndex(indexName: String) {
        if (currentConstituentIndex == indexName && _uiState.value is MarketDashboardUiState.Success) return
        currentConstituentIndex = indexName

        val currentState = _uiState.value
        if (currentState is MarketDashboardUiState.Success) {
            _uiState.value = currentState.copy(
                selectedConstituentIndex = indexName,
                isConstituentsLoading = true
            )
        }

        viewModelScope.launch {
            val requestGeneration = accountGeneration
            val result = fetchConstituentsForIndex(indexName)
            if (requestGeneration != accountGeneration || currentConstituentIndex != indexName) return@launch
            val updatedState = _uiState.value
            if (updatedState is MarketDashboardUiState.Success) {
                _uiState.value = updatedState.copy(
                    selectedConstituentIndex = indexName,
                    constituents = result.getOrDefault(emptyList()),
                    isConstituentsLoading = false
                )
            }
            updateMarketSubscriptions()
        }
    }

    private suspend fun fetchSymbolsForFilter(filterType: SymbolFilterType): Result<List<MarketStock>> = when (filterType) {
        SymbolFilterType.GAINERS -> getTopGainersUseCase(10)
        SymbolFilterType.LOSERS -> getTopLosersUseCase(10)
        SymbolFilterType.VOLUME_SPIKES -> getVolumeSpikesUseCase(10)
        SymbolFilterType.MOST_ACTIVE -> getVolumeSpikesUseCase(10)
        SymbolFilterType.WATCHLIST -> repository.getTopSymbols(SymbolFilterType.WATCHLIST)
    }

    private suspend fun fetchConstituentsForIndex(indexName: String): Result<List<IndexConstituent>> {
        val result = when (indexName.uppercase()) {
            "KSE-30", "KSE30" -> getKse30ConstituentsUseCase()
            "KMI-30", "KMI30" -> getKmi30ConstituentsUseCase()
            else -> getKse100ConstituentsUseCase()
        }
        return result.map { it.constituents }
    }

    private fun applyCachedMarketStocks(stocks: List<MarketStock>): List<MarketStock> = stocks.map { stock ->
        liveMarketRepository.getCachedQuote(stock.symbol)?.let { quote -> stock.withQuote(quote) } ?: stock
    }

    private fun applyCachedConstituents(stocks: List<IndexConstituent>): List<IndexConstituent> = stocks.map { stock ->
        liveMarketRepository.getCachedQuote(stock.symbol)?.let { quote -> stock.withQuote(quote) } ?: stock
    }

    private fun MarketStock.withQuote(quote: LiveQuote) = copy(
        price = quote.current ?: price,
        change = quote.change ?: change,
        changePercent = quote.changePct ?: changePercent,
        volume = quote.volume?.toString() ?: volume
    )

    private fun IndexConstituent.withQuote(quote: LiveQuote) = copy(
        current = quote.current ?: current,
        change = quote.change ?: change,
        changePct = quote.changePct ?: changePct,
        volume = quote.volume ?: volume
    )

    private fun accountKeyForCurrentSession(): String? = accountKeyFor(SessionManager.authState.value)

    private fun accountKeyFor(authState: AuthState): String? = (authState as? AuthState.Authenticated)
        ?.let {
            SessionManager.getCurrentUserId()?.takeIf(String::isNotBlank)
                ?: it.userEmail.takeIf(String::isNotBlank)
        }
        ?.lowercase()
}
