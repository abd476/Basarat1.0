package com.fyp.basarattradeinvest.feature.shariah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.ShariahRepositoryImpl
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.domain.model.shariah.ShariahStock
import com.fyp.basarattradeinvest.domain.model.live.LiveQuote
import com.fyp.basarattradeinvest.domain.repository.LiveMarketRepository
import com.fyp.basarattradeinvest.domain.repository.ShariahRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ShariahScreeningViewModel(
    private val repository: ShariahRepository = ShariahRepositoryImpl(),
    private val liveMarketRepository: LiveMarketRepository = NetworkClient.liveMarketRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ShariahScreeningUiState>(ShariahScreeningUiState.Loading)
    val uiState: StateFlow<ShariahScreeningUiState> = _uiState.asStateFlow()

    private var currentCategory: String = "KMI-30"
    private var searchQuery: String = ""
    private var isLiveUpdatesActive = false
    private val pendingQuotes = mutableMapOf<String, LiveQuote>()
    private var quoteFlushJob: Job? = null

    init {
        // The Shariah screening payload has no prices; they arrive one symbol at a time from the
        // quote stream. Buffer the ticks and publish once per flush window so a 30-symbol wave
        // does not rebuild the whole table 30 times.
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

        loadStocks()
    }

    fun loadStocks(
        category: String = currentCategory,
        query: String = searchQuery
    ) {
        currentCategory = category
        searchQuery = query

        viewModelScope.launch {
            _uiState.value = ShariahScreeningUiState.Loading
            val result = repository.getShariahCompliantStocks(category = currentCategory, query = searchQuery)
            result.fold(
                onSuccess = { list ->
                    if (list.isEmpty()) {
                        _uiState.value = ShariahScreeningUiState.Empty
                    } else {
                        _uiState.value = ShariahScreeningUiState.Success(
                            stocks = list.withQuotes(liveMarketRepository.quoteCache.value),
                            category = currentCategory,
                            totalCount = list.size
                        )
                        updateQuoteSubscriptions(list.map { it.symbol })
                    }
                },
                onFailure = { error ->
                    _uiState.value = ShariahScreeningUiState.Error(
                        error.message ?: "Unable to load PSX Shariah compliant stocks."
                    )
                }
            )
        }
    }

    fun setCategory(category: String) {
        if (currentCategory.equals(category, ignoreCase = true)) return
        loadStocks(category = category, query = searchQuery)
    }

    fun onSearchQueryChanged(query: String) {
        loadStocks(category = currentCategory, query = query)
    }

    fun setLiveUpdatesActive(active: Boolean) {
        if (isLiveUpdatesActive == active) return
        isLiveUpdatesActive = active
        val symbols = (_uiState.value as? ShariahScreeningUiState.Success)?.stocks?.map { it.symbol }.orEmpty()
        updateQuoteSubscriptions(symbols)
    }

    private fun updateQuoteSubscriptions(symbols: Collection<String>) {
        liveMarketRepository.setSubscriptions(
            owner = SUBSCRIPTION_OWNER,
            symbols = if (isLiveUpdatesActive) symbols else emptyList()
        )
    }

    private fun applyPendingQuotes() {
        quoteFlushJob = null
        if (pendingQuotes.isEmpty()) return
        val batch = pendingQuotes.toMap()
        pendingQuotes.clear()
        val current = _uiState.value as? ShariahScreeningUiState.Success ?: return
        _uiState.value = current.copy(stocks = current.stocks.withQuotes(batch))
    }

    private fun List<ShariahStock>.withQuotes(quotes: Map<String, LiveQuote>): List<ShariahStock> =
        map { stock ->
            val quote = quotes[stock.symbol.uppercase()] ?: return@map stock
            stock.copy(
                price = quote.current ?: stock.price,
                changePercent = quote.changePct ?: stock.changePercent
            )
        }

    companion object {
        private const val SUBSCRIPTION_OWNER = "shariah"
        private const val QUOTE_FLUSH_INTERVAL_MS = 750L
    }
}
