package com.fyp.basarattradeinvest.feature.dashboard

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentHomeBinding
import com.fyp.basarattradeinvest.domain.model.MarketIndex
import com.fyp.basarattradeinvest.domain.model.MarketStock
import com.fyp.basarattradeinvest.feature.markets.adapter.MarketStockAdapter
import com.fyp.basarattradeinvest.feature.news.adapter.NewsArticleAdapter
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by activityViewModels()

    private val priceFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val indexFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    private val marketStockAdapter by lazy {
        MarketStockAdapter { stock ->
            val bundle = Bundle().apply { putString("symbol", stock.symbol) }
            findNavController().navigate(R.id.stockDetailFragment, bundle)
        }
    }

    private val newsArticleAdapter by lazy {
        NewsArticleAdapter { article ->
            val bundle = Bundle().apply { putString("articleId", article.id) }
            findNavController().navigate(R.id.newsArticleDetailFragment, bundle)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupListeners()
        setupBenchmarkChart()
        observeUiState()
        viewModel.ensureLoaded()
    }

    private fun setupRecyclerViews() {
        binding.rvMarketMovers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = marketStockAdapter
            isNestedScrollingEnabled = false
        }

        binding.rvMarketNews.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsArticleAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refresh()
        }

        binding.btnRetryHome.setOnClickListener {
            viewModel.refresh()
        }

        // 1. Search Bar -> Stock Search
        binding.btnSearchBar.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_stockSearch)
        }

        // 2. Header Actions
        binding.btnAlerts.setOnClickListener {
            findNavController().navigate(R.id.alertsFragment)
        }

        binding.btnProfile.setOnClickListener {
            findNavController().navigate(R.id.editProfileFragment)
        }

        // 3. Watchlist "View All"
        binding.btnViewWatchlist.setOnClickListener {
            findNavController().navigate(R.id.nav_markets)
        }

        // 4. Movers "View All" -> Markets
        binding.btnViewMarketMovers.setOnClickListener {
            findNavController().navigate(R.id.nav_markets)
        }

        // 5. News "View All" -> News & Events
        binding.btnViewMarketNews.setOnClickListener {
            findNavController().navigate(R.id.newsEventsFragment)
        }

        // 6. AI Assistant Banner -> Chat
        binding.btnAskAi.setOnClickListener {
            findNavController().navigate(R.id.assistantChatFragment)
        }

        // 7. Movers Filter Tabs
        binding.btnTabGainers.setOnClickListener { viewModel.selectMoversTab(MoversTab.GAINERS) }
        binding.btnTabLosers.setOnClickListener { viewModel.selectMoversTab(MoversTab.LOSERS) }
        binding.btnTabActive.setOnClickListener { viewModel.selectMoversTab(MoversTab.MOST_ACTIVE) }

        // 8. Timeframe Selector Pills for Benchmark
        binding.tabTimeframe1d.setOnClickListener { viewModel.selectTimeframe("1D") }
        binding.tabTimeframe1w.setOnClickListener { viewModel.selectTimeframe("1W") }
        binding.tabTimeframe1m.setOnClickListener { viewModel.selectTimeframe("1M") }
        binding.tabTimeframe3m.setOnClickListener { viewModel.selectTimeframe("3M") }
    }

    private fun setupBenchmarkChart() {
        binding.chartMarketTrend.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            xAxis.isEnabled = false
            axisLeft.isEnabled = false
            axisRight.isEnabled = false
            setViewPortOffsets(0f, 0f, 0f, 0f)
            setNoDataText("")
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is HomeUiState.Loading -> {
                            binding.layoutHomeSkeleton.visibility = View.VISIBLE
                            binding.layoutHomeError.visibility = View.GONE
                            binding.scrollContent.visibility = View.GONE
                            binding.swipeRefresh.isRefreshing = false
                        }
                        is HomeUiState.Error -> {
                            binding.layoutHomeSkeleton.visibility = View.GONE
                            binding.layoutHomeError.visibility = View.VISIBLE
                            binding.scrollContent.visibility = View.GONE
                            binding.tvHomeErrorMsg.text = state.message
                            binding.swipeRefresh.isRefreshing = false
                        }
                        is HomeUiState.Success -> {
                            binding.layoutHomeSkeleton.visibility = View.GONE
                            binding.layoutHomeError.visibility = View.GONE
                            binding.scrollContent.visibility = View.VISIBLE
                            binding.swipeRefresh.isRefreshing = false
                            bindSuccessState(state)
                        }
                    }
                }
            }
        }
    }

    private fun bindSuccessState(state: HomeUiState.Success) {
        val context = requireContext()

        // 1. Header Greeting & User Name
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greetingPrefix = when (hour) {
            in 4..11 -> "Good morning,"
            in 12..16 -> "Good afternoon,"
            in 17..22 -> "Good evening,"
            else -> "Welcome back,"
        }
        binding.tvGreeting.text = greetingPrefix
        binding.tvUserName.text = state.userName

        // Market Status
        binding.tvMarketStatus.text = state.marketStatus
        val statusColor = if (state.isMarketOpen) {
            ContextCompat.getColor(context, R.color.color_positive)
        } else {
            ContextCompat.getColor(context, R.color.color_neutral)
        }
        binding.dotMarketStatus.backgroundTintList = ColorStateList.valueOf(statusColor)

        // Notification Badge
        if (state.activeAlertsCount > 0) {
            binding.tvAlertBadge.visibility = View.VISIBLE
            binding.tvAlertBadge.text = if (state.activeAlertsCount > 9) "9+" else state.activeAlertsCount.toString()
        } else {
            binding.tvAlertBadge.visibility = View.GONE
        }

        // 2. Market Pulse Indices
        bindMarketPulse(state.marketIndices)

        // 3. Benchmark Trend Card (KSE-100)
        bindBenchmarkCard(state)

        // 4. Watchlist Preview
        bindWatchlist(state.watchlistStocks)

        // 5. Market Movers
        bindMarketMovers(state)

        // 6. Market News
        newsArticleAdapter.submitList(state.recentNews)
    }

    private fun bindMarketPulse(indices: List<MarketIndex>) {
        val context = requireContext()

        // Index 1 (KSE-100)
        val kse100 = indices.firstOrNull { it.code.contains("100") || it.name.contains("100") } ?: indices.getOrNull(0)
        kse100?.let {
            binding.tvIndexName1.text = it.name
            binding.tvIndexVal1.text = indexFormat.format(it.currentPoints)
            val isPos = it.changePercent >= 0
            val prefix = if (isPos) "+" else ""
            binding.tvIndexChange1.text = String.format(Locale.US, "%s%.2f%%", prefix, it.changePercent)
            val col = ContextCompat.getColor(context, if (isPos) R.color.color_positive else R.color.color_negative)
            binding.tvIndexChange1.setTextColor(col)
        }

        // Index 2 (KSE-30)
        val kse30 = indices.firstOrNull { it.code.contains("30") && !it.code.contains("KMI") } ?: indices.getOrNull(1)
        kse30?.let {
            binding.tvIndexName2.text = it.name
            binding.tvIndexVal2.text = indexFormat.format(it.currentPoints)
            val isPos = it.changePercent >= 0
            val prefix = if (isPos) "+" else ""
            binding.tvIndexChange2.text = String.format(Locale.US, "%s%.2f%%", prefix, it.changePercent)
            val col = ContextCompat.getColor(context, if (isPos) R.color.color_positive else R.color.color_negative)
            binding.tvIndexChange2.setTextColor(col)
        }

        // Index 3 (KMI-30)
        val kmi30 = indices.firstOrNull { it.code.contains("KMI") || it.name.contains("KMI") } ?: indices.getOrNull(2)
        kmi30?.let {
            binding.tvIndexName3.text = it.name
            binding.tvIndexVal3.text = indexFormat.format(it.currentPoints)
            val isPos = it.changePercent >= 0
            val prefix = if (isPos) "+" else ""
            binding.tvIndexChange3.text = String.format(Locale.US, "%s%.2f%%", prefix, it.changePercent)
            val col = ContextCompat.getColor(context, if (isPos) R.color.color_positive else R.color.color_negative)
            binding.tvIndexChange3.setTextColor(col)
        }

        // Index 4 (ALLSHR)
        val allshr = indices.firstOrNull { it.code.contains("ALL", ignoreCase = true) } ?: indices.getOrNull(3)
        allshr?.let {
            binding.tvIndexName4.text = it.name
            binding.tvIndexVal4.text = indexFormat.format(it.currentPoints)
            val isPos = it.changePercent >= 0
            val prefix = if (isPos) "+" else ""
            binding.tvIndexChange4.text = String.format(Locale.US, "%s%.2f%%", prefix, it.changePercent)
            val col = ContextCompat.getColor(context, if (isPos) R.color.color_positive else R.color.color_negative)
            binding.tvIndexChange4.setTextColor(col)
        }
    }

    private fun bindBenchmarkCard(state: HomeUiState.Success) {
        val benchmark = state.selectedBenchmarkIndex
        val context = requireContext()

        if (benchmark != null) {
            binding.tvBenchmarkTitle.text = "${benchmark.name} Benchmark"
            binding.tvBenchmarkPoints.text = indexFormat.format(benchmark.currentPoints)

            val isPos = benchmark.changePercent >= 0
            val prefix = if (isPos) "+" else ""
            val changePtsStr = String.format(Locale.US, "%s%.2f", prefix, benchmark.changePoints)
            val changePctStr = String.format(Locale.US, "%s%.2f%%", prefix, benchmark.changePercent)
            binding.tvBenchmarkChange.text = "$changePtsStr ($changePctStr)"

            val trendColor = ContextCompat.getColor(
                context,
                if (isPos) R.color.color_positive else R.color.color_negative
            )
            binding.tvBenchmarkChange.setTextColor(trendColor)

            if (benchmark.high > 0.0) {
                binding.tvBenchmarkHigh.text = "Day High: ${indexFormat.format(benchmark.high)}"
            } else {
                binding.tvBenchmarkHigh.text = "Day High: --"
            }

            if (benchmark.low > 0.0) {
                binding.tvBenchmarkLow.text = "Day Low: ${indexFormat.format(benchmark.low)}"
            } else {
                binding.tvBenchmarkLow.text = "Day Low: --"
            }

            // Render chart points if intraday or high/low points available
            val points = if (benchmark.intradayPoints.isNotEmpty()) {
                benchmark.intradayPoints.mapIndexed { idx, p -> Entry(idx.toFloat(), p) }
            } else if (benchmark.high > 0.0 && benchmark.low > 0.0) {
                val openApprox = (benchmark.currentPoints - benchmark.changePoints).toFloat()
                listOf(
                    Entry(0f, openApprox),
                    Entry(1f, benchmark.low.toFloat()),
                    Entry(2f, benchmark.high.toFloat()),
                    Entry(3f, benchmark.currentPoints.toFloat())
                )
            } else {
                emptyList()
            }

            if (points.isNotEmpty()) {
                val dataSet = LineDataSet(points, "Benchmark").apply {
                    color = trendColor
                    lineWidth = 2f
                    setDrawCircles(false)
                    setDrawValues(false)
                    mode = LineDataSet.Mode.CUBIC_BEZIER
                    setDrawFilled(true)
                    fillColor = trendColor
                    fillAlpha = 35
                }
                binding.chartMarketTrend.data = LineData(dataSet)
                binding.chartMarketTrend.invalidate()
            }
        }

        // Timeframe selector styles
        val pills = listOf(
            binding.tabTimeframe1d to "1D",
            binding.tabTimeframe1w to "1W",
            binding.tabTimeframe1m to "1M",
            binding.tabTimeframe3m to "3M"
        )
        pills.forEach { (view, tf) ->
            if (tf == state.selectedTimeframe) {
                view.setBackgroundResource(R.drawable.bg_pill_chart_toggle_selected)
                view.setTextColor(ContextCompat.getColor(context, R.color.color_text_primary))
            } else {
                view.setBackgroundColor(Color.TRANSPARENT)
                view.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
            }
        }
    }

    private fun bindWatchlist(stocks: List<MarketStock>) {
        if (stocks.isEmpty()) {
            binding.layoutWatchlistEmpty.visibility = View.VISIBLE
            binding.layoutWatchlistItems.visibility = View.GONE
            return
        }

        binding.layoutWatchlistEmpty.visibility = View.GONE
        binding.layoutWatchlistItems.visibility = View.VISIBLE

        val s1 = stocks.getOrNull(0)
        if (s1 != null) {
            binding.cardWatchlistItem1.visibility = View.VISIBLE
            binding.tvWatchlistSym1.text = s1.symbol
            binding.tvWatchlistName1.text = s1.companyName
            binding.tvWatchlistPrice1.text = priceFormat.format(s1.price)

            val isPos1 = s1.changePercent >= 0
            val prefix1 = if (isPos1) "+" else ""
            binding.tvWatchlistChange1.text = String.format(Locale.US, "%s%.2f%%", prefix1, s1.changePercent)
            val col1 = ContextCompat.getColor(requireContext(), if (isPos1) R.color.color_positive else R.color.color_negative)
            binding.tvWatchlistChange1.setTextColor(col1)
            binding.sparklineWatchlist1.setTrend(isPos1)

            binding.cardWatchlistItem1.setOnClickListener {
                val bundle = Bundle().apply { putString("symbol", s1.symbol) }
                findNavController().navigate(R.id.stockDetailFragment, bundle)
            }
        } else {
            binding.cardWatchlistItem1.visibility = View.GONE
        }

        val s2 = stocks.getOrNull(1)
        if (s2 != null) {
            binding.cardWatchlistItem2.visibility = View.VISIBLE
            binding.tvWatchlistSym2.text = s2.symbol
            binding.tvWatchlistName2.text = s2.companyName
            binding.tvWatchlistPrice2.text = priceFormat.format(s2.price)

            val isPos2 = s2.changePercent >= 0
            val prefix2 = if (isPos2) "+" else ""
            binding.tvWatchlistChange2.text = String.format(Locale.US, "%s%.2f%%", prefix2, s2.changePercent)
            val col2 = ContextCompat.getColor(requireContext(), if (isPos2) R.color.color_positive else R.color.color_negative)
            binding.tvWatchlistChange2.setTextColor(col2)
            binding.sparklineWatchlist2.setTrend(isPos2)

            binding.cardWatchlistItem2.setOnClickListener {
                val bundle = Bundle().apply { putString("symbol", s2.symbol) }
                findNavController().navigate(R.id.stockDetailFragment, bundle)
            }
        } else {
            binding.cardWatchlistItem2.visibility = View.GONE
        }
    }

    private fun bindMarketMovers(state: HomeUiState.Success) {
        val context = requireContext()

        // Tab selection styles
        val moverTabs = listOf(
            Triple(binding.btnTabGainers, MoversTab.GAINERS, "Gainers"),
            Triple(binding.btnTabLosers, MoversTab.LOSERS, "Losers"),
            Triple(binding.btnTabActive, MoversTab.MOST_ACTIVE, "Most Active")
        )

        moverTabs.forEach { (view, tab, _) ->
            if (tab == state.selectedMoversTab) {
                view.setBackgroundResource(R.drawable.bg_pill_chart_toggle_selected)
                view.setTextColor(ContextCompat.getColor(context, R.color.color_text_primary))
            } else {
                view.setBackgroundColor(Color.TRANSPARENT)
                view.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
            }
        }

        // Submit list to RecyclerView
        marketStockAdapter.submitList(state.currentMovers)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
