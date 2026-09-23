package com.fyp.basarattradeinvest.feature.portfolio.main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentPortfolioMainBinding
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioAllocation
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioOverview
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioPerformance
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioPnL
import com.fyp.basarattradeinvest.domain.model.portfolio.PortfolioTransaction
import com.fyp.basarattradeinvest.feature.portfolio.main.adapter.AllocationBarAdapter
import com.fyp.basarattradeinvest.feature.portfolio.main.adapter.AllocationDisplayItem
import com.fyp.basarattradeinvest.feature.portfolio.main.adapter.HoldingAdapter
import com.fyp.basarattradeinvest.feature.portfolio.transactions.adapter.TransactionAdapter
import com.fyp.basarattradeinvest.feature.portfolio.transactions.dialog.AddEditTransactionDialogFragment
import com.fyp.basarattradeinvest.feature.portfolio.transactions.dialog.RecordCompletedTradeDialogFragment
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class PortfolioMainFragment : Fragment() {

    private var _binding: FragmentPortfolioMainBinding? = null
    private val binding get() = _binding!!

    private val viewModel: PortfolioMainViewModel by viewModels()

    private lateinit var holdingAdapter: HoldingAdapter
    private lateinit var stockAllocationAdapter: AllocationBarAdapter
    private lateinit var sectorAllocationAdapter: AllocationBarAdapter
    private lateinit var transactionAdapter: TransactionAdapter

    private val currencyFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPortfolioMainBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupListeners()
        observeState()
    }

    private fun setupRecyclerViews() {
        holdingAdapter = HoldingAdapter { symbol ->
            val bundle = bundleOf("symbol" to symbol)
            findNavController().navigate(R.id.action_portfolio_to_holdingDetail, bundle)
        }
        binding.rvHoldings.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHoldings.adapter = holdingAdapter

        stockAllocationAdapter = AllocationBarAdapter()
        binding.rvAllocationStock.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllocationStock.adapter = stockAllocationAdapter

        sectorAllocationAdapter = AllocationBarAdapter()
        binding.rvAllocationSector.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllocationSector.adapter = sectorAllocationAdapter

        transactionAdapter = TransactionAdapter { txId ->
            val bundle = bundleOf("transaction_id" to txId)
            findNavController().navigate(R.id.action_portfolio_to_transactionDetail, bundle)
        }
        binding.rvRecentTransactions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRecentTransactions.adapter = transactionAdapter
    }

    private fun setupListeners() {
        binding.btnAddTransaction.setOnClickListener {
            val dialog = AddEditTransactionDialogFragment.newInstanceForCreate()
            dialog.onTransactionSaved = {
                viewModel.loadPortfolio(forceRefresh = true)
            }
            dialog.show(childFragmentManager, "add_transaction")
        }

        binding.btnRecordTrade.setOnClickListener {
            val dialog = RecordCompletedTradeDialogFragment()
            dialog.onTradeRecorded = {
                viewModel.loadPortfolio(forceRefresh = true)
            }
            dialog.show(childFragmentManager, "record_trade")
        }

        binding.btnViewFullHistory.setOnClickListener {
            findNavController().navigate(R.id.action_portfolio_to_transactionHistory)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadPortfolio(forceRefresh = true)
        }

        // Tabs
        binding.tabHoldings.setOnClickListener { viewModel.selectTab(0) }
        binding.tabPnl.setOnClickListener { viewModel.selectTab(1) }
        binding.tabAllocation.setOnClickListener { viewModel.selectTab(2) }
        binding.tabPerformance.setOnClickListener { viewModel.selectTab(3) }
        binding.tabTransactions.setOnClickListener { viewModel.selectTab(4) }

        // Performance Periods
        binding.btnPeriod1w.setOnClickListener { viewModel.setPeriod("1W") }
        binding.btnPeriod1m.setOnClickListener { viewModel.setPeriod("1M") }
        binding.btnPeriod3m.setOnClickListener { viewModel.setPeriod("3M") }
        binding.btnPeriod1y.setOnClickListener { viewModel.setPeriod("1Y") }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        renderState(state)
                    }
                }
                launch {
                    viewModel.selectedTab.collect { tabIndex ->
                        renderTabSelection(tabIndex)
                    }
                }
                launch {
                    viewModel.selectedPeriod.collect { period ->
                        renderPeriodSelection(period)
                    }
                }
            }
        }
    }

    private fun renderState(state: PortfolioMainUiState) {
        when (state) {
            is PortfolioMainUiState.Loading -> {
                binding.layoutLoading.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
            }
            is PortfolioMainUiState.Success -> {
                binding.layoutLoading.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                renderOverview(state.overview)
                state.pnl?.let { renderPnL(it) }
                state.allocation?.let { renderAllocation(it) }
                state.performance?.let { renderPerformance(it) }
                renderRecentTransactions(state.recentTransactions)
            }
            is PortfolioMainUiState.Empty -> {
                binding.layoutLoading.visibility = View.GONE
                binding.layoutError.visibility = View.GONE
                binding.tvTotalCurrentValue.text = "PKR 0.00"
                binding.tvTotalInvested.text = "PKR 0.00"
                binding.tvHoldingCount.text = "0 Stocks"
                binding.tvTotalPnlBadge.text = "PKR 0.00 (0.00%)"
                binding.tvTodayPnl.text = "Today: PKR 0.00"
                binding.layoutEmptyHoldings.visibility = View.VISIBLE
                binding.rvHoldings.visibility = View.GONE
            }
            is PortfolioMainUiState.Error -> {
                binding.layoutLoading.visibility = View.GONE
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMsg.text = state.message
            }
        }
    }

    private fun renderOverview(overview: PortfolioOverview) {
        val s = overview.summary
        binding.tvTotalCurrentValue.text = "PKR ${currencyFormat.format(s.currentValue)}"
        binding.tvTotalInvested.text = "PKR ${currencyFormat.format(s.totalInvested)}"
        binding.tvHoldingCount.text = "${overview.holdings.size} Stocks"

        val prefix = if (s.totalPnl >= 0) "+" else ""
        binding.tvTotalPnlBadge.text = String.format(
            Locale.US,
            "%sPKR %s (%.2f%%)",
            prefix,
            currencyFormat.format(s.totalPnl),
            s.totalPnlPercent
        )
        if (s.totalPnl >= 0) {
            binding.tvTotalPnlBadge.setBackgroundResource(R.drawable.bg_badge_buy)
            binding.tvTotalPnlBadge.setTextColor(Color.parseColor("#18B981"))
        } else {
            binding.tvTotalPnlBadge.setBackgroundResource(R.drawable.bg_badge_sell)
            binding.tvTotalPnlBadge.setTextColor(Color.parseColor("#EF4444"))
        }

        val todayPrefix = if (s.todayPnl >= 0) "+" else ""
        binding.tvTodayPnl.text = "Today: ${todayPrefix}PKR ${currencyFormat.format(s.todayPnl)}"

        if (overview.holdings.isEmpty()) {
            binding.layoutEmptyHoldings.visibility = View.VISIBLE
            binding.rvHoldings.visibility = View.GONE
        } else {
            binding.layoutEmptyHoldings.visibility = View.GONE
            binding.rvHoldings.visibility = View.VISIBLE
            holdingAdapter.submitList(overview.holdings)
        }
    }

    private fun renderPnL(pnl: PortfolioPnL) {
        binding.tvPnlRealized.text = "PKR ${currencyFormat.format(pnl.realizedPnl)}"
        binding.tvPnlUnrealized.text = "PKR ${currencyFormat.format(pnl.unrealizedPnl)}"

        val prefix = if (pnl.totalPnl >= 0) "+" else ""
        binding.tvPnlTotal.text = String.format(
            Locale.US,
            "%sPKR %s (%.2f%%)",
            prefix,
            currencyFormat.format(pnl.totalPnl),
            pnl.totalPnlPercent
        )
        if (pnl.totalPnl >= 0) {
            binding.tvPnlTotal.setTextColor(Color.parseColor("#18B981"))
        } else {
            binding.tvPnlTotal.setTextColor(Color.parseColor("#EF4444"))
        }

        val todayPrefix = if (pnl.todayPnl >= 0) "+" else ""
        binding.tvPnlToday.text = "${todayPrefix}PKR ${currencyFormat.format(pnl.todayPnl)}"
    }

    private fun renderAllocation(allocation: PortfolioAllocation) {
        val stockItems = allocation.byStock.map {
            AllocationDisplayItem(it.symbol, it.percentage, it.marketValue)
        }
        stockAllocationAdapter.submitList(stockItems)

        val sectorItems = allocation.bySector.map {
            AllocationDisplayItem(it.sector, it.percentage, it.marketValue)
        }
        sectorAllocationAdapter.submitList(sectorItems)
    }

    private fun renderPerformance(perf: PortfolioPerformance) {
        binding.tvPerfPointsCount.text = "Showing ${perf.data.size} recorded performance data points (${perf.period})"
    }

    private fun renderRecentTransactions(transactions: List<PortfolioTransaction>) {
        transactionAdapter.submitList(transactions)
    }

    private fun renderTabSelection(tabIndex: Int) {
        // Reset all tabs to unselected
        val tabs = listOf(
            binding.tabHoldings,
            binding.tabPnl,
            binding.tabAllocation,
            binding.tabPerformance,
            binding.tabTransactions
        )
        tabs.forEach {
            it.setBackgroundResource(R.drawable.bg_tab_unselected)
            it.setTextColor(Color.parseColor("#667085"))
        }

        // Highlight selected tab
        if (tabIndex in tabs.indices) {
            tabs[tabIndex].setBackgroundResource(R.drawable.bg_tab_selected)
            tabs[tabIndex].setTextColor(Color.WHITE)
        }

        // Switch visible section
        binding.sectionHoldings.visibility = if (tabIndex == 0) View.VISIBLE else View.GONE
        binding.sectionPnl.visibility = if (tabIndex == 1) View.VISIBLE else View.GONE
        binding.sectionAllocation.visibility = if (tabIndex == 2) View.VISIBLE else View.GONE
        binding.sectionPerformance.visibility = if (tabIndex == 3) View.VISIBLE else View.GONE
        binding.sectionTransactions.visibility = if (tabIndex == 4) View.VISIBLE else View.GONE
    }

    private fun renderPeriodSelection(period: String) {
        val buttons = mapOf(
            "1W" to binding.btnPeriod1w,
            "1M" to binding.btnPeriod1m,
            "3M" to binding.btnPeriod3m,
            "1Y" to binding.btnPeriod1y
        )
        buttons.forEach { (p, btn) ->
            if (p == period) {
                btn.setBackgroundResource(R.drawable.bg_tab_selected)
                btn.setTextColor(Color.WHITE)
            } else {
                btn.setBackgroundResource(R.drawable.bg_tab_unselected)
                btn.setTextColor(Color.parseColor("#667085"))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
