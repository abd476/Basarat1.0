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
import com.fyp.basarattradeinvest.domain.model.IndexConstituent
import com.fyp.basarattradeinvest.domain.model.MarketIndex
import com.fyp.basarattradeinvest.domain.model.MarketOverview
import com.fyp.basarattradeinvest.domain.model.MarketSentiment
import com.fyp.basarattradeinvest.domain.model.SymbolFilterType
import com.fyp.basarattradeinvest.feature.markets.adapter.ConstituentAdapter
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

    private val constituentAdapter = ConstituentAdapter { constituent ->
        val bundle = Bundle().apply { putString("symbol", constituent.symbol) }
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

        binding.rvConstituents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = constituentAdapter
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

        binding.tabConstituentKse100.setOnClickListener {
            viewModel.selectConstituentIndex("KSE-100")
        }
        binding.tabConstituentKse30.setOnClickListener {
            viewModel.selectConstituentIndex("KSE-30")
        }
        binding.tabConstituentKmi30.setOnClickListener {
            viewModel.selectConstituentIndex("KMI-30")
        }

        binding.ivQuoteBannerClose.setOnClickListener {
            binding.bannerRealtimeQuotes.visibility = View.GONE
        }
    }

    private fun setupTopBarListeners() {
        binding.ivTopMenu.setOnClickListener {
            Toast.makeText(requireContext(), "Menu drawer", Toast.LENGTH_SHORT).show()
        }
        binding.ivTopNotifications.setOnClickListener {
            findNavController().navigate(R.id.action_marketDashboard_to_notifications)
        }
        binding.ivTopSearch.setOnClickListener {
            findNavController().navigate(R.id.stockSearchFragment)
        }
        binding.tvSectorsSeeAll.setOnClickListener {
            Toast.makeText(requireContext(), "Sector Heatmap", Toast.LENGTH_SHORT).show()
        }
        binding.chipShariah.setOnClickListener {
            findNavController().navigate(R.id.shariahScreeningFragment)
        }
        binding.layoutIndex2.setOnClickListener {
            findNavController().navigate(R.id.shariahScreeningFragment)
        }
        binding.chipCommunity.setOnClickListener {
            findNavController().navigate(R.id.communityProfileFragment)
        }
        binding.chipNews.setOnClickListener {
            findNavController().navigate(R.id.newsEventsFragment)
        }
        binding.chipRecommendations.setOnClickListener {
            findNavController().navigate(R.id.recommendationsFragment)
        }

        binding.fabChatAssistant.setOnClickListener {
            findNavController().navigate(R.id.action_marketDashboard_to_assistant)
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
                            sectorAdapter.submitList(state.sectors.toList())
                            bindFilterTabs(state.selectedFilter)
                            stockAdapter.submitList(state.topSymbols.toList())
                            bindConstituents(state.constituents, state.selectedConstituentIndex, state.isConstituentsLoading)
                        }
                        is MarketDashboardUiState.Empty -> {
                            binding.swipeRefreshLayout.isRefreshing = false
                            stockAdapter.submitList(emptyList())
                            constituentAdapter.submitList(emptyList())
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
        if (indices.isEmpty()) return

        val kse100 = indices.firstOrNull {
            it.name.contains("100", ignoreCase = true) || it.code.contains("100", ignoreCase = true)
        } ?: indices.getOrNull(0)

        val kse30 = indices.firstOrNull {
            (it.name.contains("KSE-30", ignoreCase = true) || it.code.contains("KSE30", ignoreCase = true)) &&
                !it.name.contains("KMI", ignoreCase = true)
        } ?: indices.getOrNull(1)

        val kmi30 = indices.firstOrNull {
            it.name.contains("KMI", ignoreCase = true) || it.code.contains("KMI", ignoreCase = true)
        } ?: indices.getOrNull(2)

        // 1. KSE-100
        kse100?.let { idx ->
            binding.tvIndex1Name.text = idx.name
            binding.tvIndex1Points.text = String.format(Locale.US, "%,.2f", idx.currentPoints)
            val prefix = if (idx.changePercent >= 0) "+" else ""
            binding.tvIndex1Change.text = String.format(Locale.US, "%.2f  %s%.2f%%", idx.changePoints, prefix, idx.changePercent)
            val color = if (idx.changePercent >= 0) R.color.color_positive else R.color.color_negative
            binding.tvIndex1Change.setTextColor(ContextCompat.getColor(requireContext(), color))
        }

        // 2. KSE-30
        kse30?.let { idx ->
            binding.tvIndex3Name.text = idx.name
            binding.tvIndex3Points.text = String.format(Locale.US, "%,.2f", idx.currentPoints)
            val prefix = if (idx.changePercent >= 0) "+" else ""
            binding.tvIndex3Change.text = String.format(Locale.US, "%.2f  %s%.2f%%", idx.changePoints, prefix, idx.changePercent)
            val color = if (idx.changePercent >= 0) R.color.color_positive else R.color.color_negative
            binding.tvIndex3Change.setTextColor(ContextCompat.getColor(requireContext(), color))
        }

        // 3. KMI-30
        kmi30?.let { idx ->
            binding.tvIndex2Name.text = idx.name
            binding.tvIndex2Points.text = String.format(Locale.US, "%,.2f", idx.currentPoints)
            val prefix = if (idx.changePercent >= 0) "+" else ""
            binding.tvIndex2Change.text = String.format(Locale.US, "%.2f  %s%.2f%%", idx.changePoints, prefix, idx.changePercent)
            val color = if (idx.changePercent >= 0) R.color.color_positive else R.color.color_negative
            binding.tvIndex2Change.setTextColor(ContextCompat.getColor(requireContext(), color))
        }

        // Chart Intraday
        val idx1Points = kse100?.intradayPoints ?: emptyList()
        val idx2Points = kmi30?.intradayPoints ?: emptyList()
        if (idx1Points.isNotEmpty() || idx2Points.isNotEmpty()) {
            binding.chartIntraday.setChartData(idx1Points, idx2Points)
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

    private fun bindConstituents(
        constituents: List<IndexConstituent>,
        selectedTab: String,
        isLoading: Boolean
    ) {
        binding.pbConstituentsLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.tvConstituentsCount.text = "${constituents.size} Stocks"

        if (!isLoading && constituents.isEmpty()) {
            binding.tvConstituentsEmpty.visibility = View.VISIBLE
            binding.rvConstituents.visibility = View.GONE
        } else {
            binding.tvConstituentsEmpty.visibility = View.GONE
            binding.rvConstituents.visibility = View.VISIBLE
            constituentAdapter.submitList(constituents)
        }

        val selectedBg = R.drawable.bg_market_tab_selected
        val unselectedBg = R.drawable.bg_market_tab_unselected
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.color_text_primary)

        val isKse100 = selectedTab.contains("100", ignoreCase = true)
        val isKse30 = selectedTab.contains("KSE-30", ignoreCase = true) || selectedTab.equals("KSE30", ignoreCase = true)
        val isKmi30 = selectedTab.contains("KMI", ignoreCase = true)

        binding.tabConstituentKse100.setBackgroundResource(if (isKse100) selectedBg else unselectedBg)
        binding.tabConstituentKse100.setTextColor(if (isKse100) selectedTextColor else unselectedTextColor)

        binding.tabConstituentKse30.setBackgroundResource(if (isKse30) selectedBg else unselectedBg)
        binding.tabConstituentKse30.setTextColor(if (isKse30) selectedTextColor else unselectedTextColor)

        binding.tabConstituentKmi30.setBackgroundResource(if (isKmi30) selectedBg else unselectedBg)
        binding.tabConstituentKmi30.setTextColor(if (isKmi30) selectedTextColor else unselectedTextColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
