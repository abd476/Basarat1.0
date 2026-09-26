package com.fyp.basarattradeinvest.feature.portfolio.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.PortfolioRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioAllocation
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioOverview
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioPerformance
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioPnL
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioTransaction
import com.fyp.basarattradeinvest.domain.repository.PortfolioRepository
import com.fyp.basarattradeinvest.domain.usecase.portfolio.GetPortfolioAllocationUseCase
import com.fyp.basarattradeinvest.domain.usecase.portfolio.GetPortfolioOverviewUseCase
import com.fyp.basarattradeinvest.domain.usecase.portfolio.GetPortfolioPerformanceUseCase
import com.fyp.basarattradeinvest.domain.usecase.portfolio.GetPortfolioPnLUseCase
import com.fyp.basarattradeinvest.domain.usecase.portfolio.GetTransactionsUseCase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PortfolioMainViewModel(
    private val getOverviewUseCase: GetPortfolioOverviewUseCase,
    private val getPnLUseCase: GetPortfolioPnLUseCase,
    private val getAllocationUseCase: GetPortfolioAllocationUseCase,
    private val getPerformanceUseCase: GetPortfolioPerformanceUseCase,
    private val getTransactionsUseCase: GetTransactionsUseCase
) : ViewModel() {

    constructor() : this(PortfolioRepositoryImpl())

    constructor(repository: PortfolioRepository) : this(
        getOverviewUseCase = GetPortfolioOverviewUseCase(repository),
        getPnLUseCase = GetPortfolioPnLUseCase(repository),
        getAllocationUseCase = GetPortfolioAllocationUseCase(repository),
        getPerformanceUseCase = GetPortfolioPerformanceUseCase(repository),
        getTransactionsUseCase = GetTransactionsUseCase(repository)
    )

    class Factory(
        private val repository: PortfolioRepository = PortfolioRepositoryImpl()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PortfolioMainViewModel::class.java)) {
                return PortfolioMainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    private val _uiState = MutableStateFlow<PortfolioMainUiState>(PortfolioMainUiState.Loading)
    val uiState: StateFlow<PortfolioMainUiState> = _uiState.asStateFlow()

    private val _selectedPeriod = MutableStateFlow("1M")
    val selectedPeriod: StateFlow<String> = _selectedPeriod.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: Holdings, 1: PnL, 2: Allocation, 3: Performance, 4: History
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()
    private var lastSuccessfulFetchAt: Long = 0L
    private val freshnessWindowMs = 5 * 60 * 1000L

    fun ensureLoaded() {
        val state = _uiState.value
        val shouldLoad = state !is PortfolioMainUiState.Success || stateHasExpired()
        if (shouldLoad) {
            loadPortfolio(forceRefresh = false)
        }
    }

    private fun stateHasExpired(): Boolean {
        return System.currentTimeMillis() - lastSuccessfulFetchAt > freshnessWindowMs
    }

    fun selectTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
    }

    fun setPeriod(period: String) {
        if (_selectedPeriod.value == period) return
        _selectedPeriod.value = period
        viewModelScope.launch {
            val perfResult = getPerformanceUseCase(period)
            val currentState = _uiState.value
            if (currentState is PortfolioMainUiState.Success && perfResult.isSuccess) {
                _uiState.value = currentState.copy(performance = perfResult.getOrNull())
            }
        }
    }

    fun loadPortfolio(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentState = _uiState.value
            val hasUsableData = currentState is PortfolioMainUiState.Success
            val shouldSkip = !forceRefresh && hasUsableData && !stateHasExpired()

            if (shouldSkip) return@launch
            if (!forceRefresh && !hasUsableData) {
                _uiState.value = PortfolioMainUiState.Loading
            }

            try {
                coroutineScope {
                    val overviewDeferred = async { getOverviewUseCase() }
                    val pnlDeferred = async { getPnLUseCase() }
                    val allocationDeferred = async { getAllocationUseCase() }
                    val performanceDeferred = async { getPerformanceUseCase(_selectedPeriod.value) }
                    val transactionsDeferred = async { getTransactionsUseCase(page = 1, limit = 5) }

                    val overviewRes = overviewDeferred.await()
                    val pnlRes = pnlDeferred.await()
                    val allocationRes = allocationDeferred.await()
                    val perfRes = performanceDeferred.await()
                    val txRes = transactionsDeferred.await()

                    lastSuccessfulFetchAt = System.currentTimeMillis()

                    if (overviewRes.isSuccess) {
                        val overview = overviewRes.getOrThrow()
                        if (overview.holdings.isEmpty() && overview.summary.totalInvested == 0.0) {
                            _uiState.value = PortfolioMainUiState.Empty
                        } else {
                            _uiState.value = PortfolioMainUiState.Success(
                                overview = overview,
                                pnl = pnlRes.getOrNull(),
                                allocation = allocationRes.getOrNull(),
                                performance = perfRes.getOrNull(),
                                recentTransactions = txRes.getOrNull()?.items ?: emptyList()
                            )
                        }
                    } else {
                        val error = overviewRes.exceptionOrNull()?.message ?: "Unable to load portfolio overview."
                        _uiState.value = PortfolioMainUiState.Error(error)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = PortfolioMainUiState.Error(e.localizedMessage ?: "Unexpected error occurred.")
            }
        }
    }
}
