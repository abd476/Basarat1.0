package com.fyp.basarattradeinvest.feature.markets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentMarketDashboardBinding
import com.fyp.basarattradeinvest.domain.model.MarketIndex
import com.fyp.basarattradeinvest.domain.model.MarketOverview
import com.fyp.basarattradeinvest.domain.model.MarketSentiment
import com.fyp.basarattradeinvest.domain.model.SymbolFilterType
import com.fyp.basarattradeinvest.feature.markets.adapter.MarketStockAdapter
import com.fyp.basarattradeinvest.feature.markets.adapter.SectorPerformanceAdapter
import kotlinx.coroutines.launch
import java.util.Locale

class MarketDashboardFragment : Fragment() {

    private var _binding: FragmentMarketDashboardBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MarketDashboardViewModel by viewModels()

    private val stockAdapter = MarketStockAdapter { stock ->
        val bundle = Bundle().apply { putString("symbol", stock.symbol) }
        findNavController().navigate(R.id.stockDetailFragment, bundle)
    }

    private val sectorAdapter = SectorPerformanceAdapter { sector ->
        Toast.makeText(requireContext(), "${sector.sectorName}: ${sector.changePercent}%", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarketDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupSwipeRefresh()
        setupTabListeners()
        setupTopBarListeners()
        observeUiState()
    }

    private fun setupRecyclerViews() {
        binding.rvTopSymbols.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = stockAdapter
        }

        binding.rvSectors.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = sectorAdapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(R.color.color_basarat_green)
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.refreshData()
        }
    }

    private fun setupTabListeners() {
        binding.tabGainers.setOnClickListener {
            viewModel.setFilter(SymbolFilterType.GAINERS)
        }
        binding.tabLosers.setOnClickListener {
            viewModel.setFilter(SymbolFilterType.LOSERS)
        }
        binding.tabMostActive.setOnClickListener {
            viewModel.setFilter(SymbolFilterType.MOST_ACTIVE)
        }
        binding.tabVolumeSpikes.setOnClickListener {
            viewModel.setFilter(SymbolFilterType.VOLUME_SPIKES)
        }
        binding.tabWatchlist.setOnClickListener {
            viewModel.setFilter(SymbolFilterType.WATCHLIST)
        }

        binding.ivQuoteBannerClose.setOnClickListener {
            binding.bannerRealtimeQuotes.visibility = View.GONE
        }
    }

    private fun setupTopBarListeners() {
        binding.ivTopMenu.setOnClickListener {
            Toast.makeText(requireContext(), "Menu drawer", Toast.LENGTH_SHORT).show()
        }
        binding.ivTopSearch.setOnClickListener {
            Toast.makeText(requireContext(), "Stock search", Toast.LENGTH_SHORT).show()
        }
        binding.tvSectorsSeeAll.setOnClickListener {
            Toast.makeText(requireContext(), "Sector Heatmap", Toast.LENGTH_SHORT).show()
        }
        binding.chipShariah.setOnClickListener {
            findNavController().navigate(R.id.shariahScreeningFragment)
        }
        binding.chipNews.setOnClickListener {
            findNavController().navigate(R.id.newsEventsFragment)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is MarketDashboardUiState.Loading -> {
                            // Non-blocking loading
                        }
                        is MarketDashboardUiState.Success -> {
                            binding.swipeRefreshLayout.isRefreshing = false
                            binding.tvLastUpdated.text = "• Updated ${state.lastUpdated}"
                            bindIndices(state.indices)
                            bindOverview(state.overview)
                            bindSentiment(state.sentiment)
                            sectorAdapter.submitList(state.sectors)
                            bindFilterTabs(state.selectedFilter)
                            stockAdapter.submitList(state.topSymbols)
                        }
                        is MarketDashboardUiState.Empty -> {
                            binding.swipeRefreshLayout.isRefreshing = false
                            stockAdapter.submitList(emptyList())
                        }
                        is MarketDashboardUiState.Error -> {
                            binding.swipeRefreshLayout.isRefreshing = false
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    private fun bindIndices(indices: List<MarketIndex>) {
        if (indices.size >= 2) {
            val idx1 = indices[0]
            val idx2 = indices[1]

            // Index 1 (e.g. KSE-100)
            binding.tvIndex1Name.text = idx1.name
            binding.tvIndex1Points.text = String.format(Locale.US, "%,.2f", idx1.currentPoints)
            val prefix1 = if (idx1.changePercent >= 0) "+" else ""
            binding.tvIndex1Change.text = String.format(Locale.US, "%.2f  %s%.2f%%", idx1.changePoints, prefix1, idx1.changePercent)
            val color1 = if (idx1.changePercent >= 0) R.color.color_positive else R.color.color_negative
            binding.tvIndex1Change.setTextColor(ContextCompat.getColor(requireContext(), color1))

            // Index 2 (e.g. KMI-30)
            binding.tvIndex2Name.text = idx2.name
            binding.tvIndex2Points.text = String.format(Locale.US, "%,.2f", idx2.currentPoints)
            val prefix2 = if (idx2.changePercent >= 0) "+" else ""
            binding.tvIndex2Change.text = String.format(Locale.US, "%.2f  %s%.2f%%", idx2.changePoints, prefix2, idx2.changePercent)
            val color2 = if (idx2.changePercent >= 0) R.color.color_positive else R.color.color_negative
            binding.tvIndex2Change.setTextColor(ContextCompat.getColor(requireContext(), color2))

            // Feed chart
            binding.chartIntraday.setChartData(idx1.intradayPoints, idx2.intradayPoints)
        }
    }

    private fun bindOverview(overview: MarketOverview) {
        binding.tvOverviewValue.text = overview.tradedValue
        binding.tvOverviewVolume.text = overview.tradedVolume
        binding.tvOverviewTrades.text = overview.totalTrades
        binding.tvTotalStocks.text = "Total ${overview.totalStocks} \u203A"

        // Advance / Decline breadth bar weights
        val total = (overview.upCount + overview.unchangedCount + overview.downCount).toFloat()
        if (total > 0) {
            (binding.barUp.layoutParams as LinearLayout.LayoutParams).weight = overview.upCount.toFloat()
            (binding.barUnchanged.layoutParams as LinearLayout.LayoutParams).weight = overview.unchangedCount.toFloat()
            (binding.barDown.layoutParams as LinearLayout.LayoutParams).weight = overview.downCount.toFloat()
            binding.barUp.requestLayout()
        }

        binding.tvUpCount.text = "\u25B2 Up ${overview.upCount}"
        binding.tvUnchangedCount.text = "Unchanged ${overview.unchangedCount}"
        binding.tvDownCount.text = "\u25BC Down ${overview.downCount}"
    }

    private fun bindSentiment(sentiment: MarketSentiment) {
        binding.tvSentimentScore.text = "${sentiment.overallScore} / 100"
        binding.tvSentimentLabel.text = sentiment.sentimentLabel

        // Adjust sentiment color according to label
        val isBullish = sentiment.sentimentLabel.contains("Bull", ignoreCase = true)
        val isBearish = sentiment.sentimentLabel.contains("Bear", ignoreCase = true)
        val labelColor = when {
            isBullish -> R.color.color_positive
            isBearish -> R.color.color_negative
            else -> R.color.color_neutral
        }
        binding.tvSentimentLabel.setTextColor(ContextCompat.getColor(requireContext(), labelColor))

        // Set 3-segment bar weights
        val total = (sentiment.positivePercent + sentiment.neutralPercent + sentiment.negativePercent).toFloat()
        if (total > 0) {
            (binding.barSentimentPositive.layoutParams as LinearLayout.LayoutParams).weight = sentiment.positivePercent.toFloat()
            (binding.barSentimentNeutral.layoutParams as LinearLayout.LayoutParams).weight = sentiment.neutralPercent.toFloat()
            (binding.barSentimentNegative.layoutParams as LinearLayout.LayoutParams).weight = sentiment.negativePercent.toFloat()
            binding.barSentimentPositive.requestLayout()
        }

        binding.tvSentimentPos.text = "Pos ${sentiment.positivePercent}%"
        binding.tvSentimentNeu.text = "Neu ${sentiment.neutralPercent}%"
        binding.tvSentimentNeg.text = "Neg ${sentiment.negativePercent}%"
       // binding.tvSentimentAiSummary.text = sentiment.aiSummary
    }

    private fun bindFilterTabs(selected: SymbolFilterType) {
        val tabs = listOf(
            binding.tabGainers to SymbolFilterType.GAINERS,
            binding.tabLosers to SymbolFilterType.LOSERS,
            binding.tabMostActive to SymbolFilterType.MOST_ACTIVE,
            binding.tabVolumeSpikes to SymbolFilterType.VOLUME_SPIKES,
            binding.tabWatchlist to SymbolFilterType.WATCHLIST
        )

        val selectedBg = R.drawable.bg_market_tab_selected
        val unselectedBg = R.drawable.bg_market_tab_unselected
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.color_text_primary)

        tabs.forEach { (tabView, type) ->
            if (type == selected) {
                tabView.setBackgroundResource(selectedBg)
                tabView.setTextColor(selectedTextColor)
            } else {
                tabView.setBackgroundResource(unselectedBg)
                tabView.setTextColor(unselectedTextColor)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
