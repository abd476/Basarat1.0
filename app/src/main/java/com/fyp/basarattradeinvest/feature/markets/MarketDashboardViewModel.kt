package com.fyp.basarattradeinvest.feature.markets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.MarketRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.IndexConstituent
import com.fyp.basarattradeinvest.domain.model.MarketOverview
import com.fyp.basarattradeinvest.domain.model.SymbolFilterType
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
    private val getSectorPerformancesUseCase: GetSectorPerformancesUseCase = GetSectorPerformancesUseCase(repository)
) : ViewModel() {

    private val _uiState = MutableStateFlow<MarketDashboardUiState>(MarketDashboardUiState.Loading)
    val uiState: StateFlow<MarketDashboardUiState> = _uiState.asStateFlow()

    private var currentFilter = SymbolFilterType.GAINERS
    private var currentConstituentIndex = "KSE-100"

    init {
        loadMarketData()
    }

    fun loadMarketData(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (!isRefresh && _uiState.value !is MarketDashboardUiState.Success) {
                _uiState.value = MarketDashboardUiState.Loading
            }

            val indicesDeferred = async { getMarketIndicesUseCase() }
            val sentimentDeferred = async { getMarketSentimentOverviewUseCase() }
            val topSymbolsDeferred = async { fetchSymbolsForFilter(currentFilter) }
            val sectorsDeferred = async { getSectorPerformancesUseCase() }
            val constituentsDeferred = async { fetchConstituentsForIndex(currentConstituentIndex) }

            val indicesResult = indicesDeferred.await()
            val sentimentResult = sentimentDeferred.await()
            val topSymbolsResult = topSymbolsDeferred.await()
            val sectorsResult = sectorsDeferred.await()
            val constituentsResult = constituentsDeferred.await()

            if (indicesResult.isSuccess || sentimentResult.isSuccess) {
                val indices = indicesResult.getOrDefault(emptyList())
                val sentiment = sentimentResult.getOrNull()
                val topSymbols = topSymbolsResult.getOrDefault(emptyList())
                val sectors = sectorsResult.getOrDefault(emptyList())
                val constituents = constituentsResult.getOrDefault(emptyList())

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

                if (sentiment != null) {
                    _uiState.value = MarketDashboardUiState.Success(
                        indices = indices,
                        overview = overview,
                        topSymbols = topSymbols,
                        selectedFilter = currentFilter,
                        sentiment = sentiment,
                        sectors = sectors,
                        selectedConstituentIndex = currentConstituentIndex,
                        constituents = constituents,
                        lastUpdated = currentTime
                    )
                } else {
                    _uiState.value = MarketDashboardUiState.Empty
                }
            } else {
                val errorMsg = indicesResult.exceptionOrNull()?.message
                    ?: sentimentResult.exceptionOrNull()?.message
                    ?: "Unable to load market data. Please check connection."
                _uiState.value = MarketDashboardUiState.Error(errorMsg)
            }
        }
    }

    fun refreshData() {
        loadMarketData(isRefresh = true)
    }

    fun setFilter(filterType: SymbolFilterType) {
        if (currentFilter == filterType && _uiState.value is MarketDashboardUiState.Success) return
        currentFilter = filterType

        val currentState = _uiState.value
        viewModelScope.launch {
            val topSymbolsResult = fetchSymbolsForFilter(filterType)
            if (topSymbolsResult.isSuccess && currentState is MarketDashboardUiState.Success) {
                _uiState.value = currentState.copy(
                    topSymbols = topSymbolsResult.getOrDefault(emptyList()),
                    selectedFilter = filterType
                )
            }
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
            val result = fetchConstituentsForIndex(indexName)
            val updatedState = _uiState.value
            if (updatedState is MarketDashboardUiState.Success) {
                _uiState.value = updatedState.copy(
                    selectedConstituentIndex = indexName,
                    constituents = result.getOrDefault(emptyList()),
                    isConstituentsLoading = false
                )
            }
        }
    }

    private suspend fun fetchSymbolsForFilter(filterType: SymbolFilterType) = when (filterType) {
        SymbolFilterType.GAINERS -> getTopGainersUseCase(10)
        SymbolFilterType.LOSERS -> getTopLosersUseCase(10)
        SymbolFilterType.VOLUME_SPIKES -> getVolumeSpikesUseCase(10)
        SymbolFilterType.MOST_ACTIVE -> repository.getTopSymbols(SymbolFilterType.MOST_ACTIVE)
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
}
