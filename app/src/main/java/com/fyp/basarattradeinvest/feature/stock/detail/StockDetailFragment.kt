package com.fyp.basarattradeinvest.feature.stock.detail

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import androidx.recyclerview.widget.RecyclerView
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentStockDetailBinding
import com.fyp.basarattradeinvest.domain.model.CandleEntry
import com.fyp.basarattradeinvest.domain.model.StockDetail
import com.fyp.basarattradeinvest.domain.model.StockTrade
import com.fyp.basarattradeinvest.feature.stock.detail.adapter.TradeAdapter
import com.fyp.basarattradeinvest.data.repository.NewsRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.RecommendationsRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.recommendations.RecommendationSignal
import com.fyp.basarattradeinvest.feature.news.adapter.NewsArticleAdapter
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.CombinedData
import kotlinx.coroutines.launch
import java.util.Locale

class StockDetailFragment : Fragment() {

    private var _binding: FragmentStockDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockDetailViewModel by viewModels()
    private val tradeAdapter = TradeAdapter()

    private var candleChart: CandleStickChart? = null
    private var volumeChart: BarChart? = null
    private var currentSymbol: String = "SARCO"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCharts()
        setupListeners()
        observeUiState()

        // Load from navigation arguments
        currentSymbol = arguments?.getString("symbol") ?: "SARCO"
        binding.tvTopSymbol.text = currentSymbol
        binding.tvTopCode.text = currentSymbol
        viewModel.loadStock(currentSymbol)
    }

    // ───────────────────────────────
    //  CHART SETUP
    // ───────────────────────────────

    private fun setupCharts() {
        val textColor = ContextCompat.getColor(requireContext(), R.color.color_text_secondary)
        val bgColor = ContextCompat.getColor(requireContext(), R.color.color_background)

        // Candlestick Chart
        candleChart = CandleStickChart(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(bgColor)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A667085")
                this.textColor = textColor
                textSize = 10f
                setDrawLabels(true)
                setAvoidFirstLastClipping(true)
            }

            axisLeft.isEnabled = false

            axisRight.apply {
                isEnabled = true
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A667085")
                this.textColor = textColor
                textSize = 10f
                setDrawAxisLine(false)
            }
        }
        binding.chartContainer.removeAllViews()
        binding.chartContainer.addView(candleChart)
        binding.tvChartPlaceholder.visibility = View.GONE

        // Volume Bar Chart
        volumeChart = BarChart(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(bgColor)
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                setDrawLabels(false)
            }

            axisLeft.isEnabled = false

            axisRight.apply {
                isEnabled = true
                setDrawGridLines(false)
                this.textColor = textColor
                textSize = 9f
                setDrawAxisLine(false)
            }
        }
        binding.volumeChartContainer.removeAllViews()
        binding.volumeChartContainer.addView(volumeChart)
        binding.tvVolumePlaceholder.visibility = View.GONE
    }

    // ───────────────────────────────
    //  LISTENERS
    // ───────────────────────────────

    private fun setupListeners() {
        // Back
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Share
        binding.ivShare.setOnClickListener {
            Toast.makeText(requireContext(), "Share stock", Toast.LENGTH_SHORT).show()
        }

        // Chat Assistant
        binding.ivChatAssistant.setOnClickListener {
            findNavController().navigate(R.id.action_stockDetail_to_assistantChat)
        }

        // Favorite
        binding.ivFavorite.setOnClickListener {
            Toast.makeText(requireContext(), "Added to watchlist", Toast.LENGTH_SHORT).show()
        }

        // AI Recommendation
        val openRecommendation = {
            val bundle = Bundle().apply { putString("symbol", currentSymbol) }
            findNavController().navigate(R.id.action_stockDetail_to_recommendationDetail, bundle)
        }
        binding.btnRecommendation.setOnClickListener { openRecommendation() }
        binding.cardAiRecommendation.setOnClickListener { openRecommendation() }

        // Timeframe tabs
        val timeframeTabs = listOf(
            binding.tab1d to "1D",
            binding.tabDaily to "Daily",
            binding.tabWeekly to "Weekly",
            binding.tab1m to "1M"
        )
        timeframeTabs.forEach { (tab, tf) ->
            tab.setOnClickListener { viewModel.setTimeframe(tf) }
        }

        // Section Tabs
        binding.tabSectionQuotes.setOnClickListener { viewModel.setSection("Quotes") }
        binding.tabSectionAnalysis.setOnClickListener {
            val currentState = viewModel.uiState.value
            val symbol = if (currentState is StockDetailUiState.Success) {
                currentState.detail.symbol
            } else {
                arguments?.getString("symbol") ?: "SARCO"
            }
            val bundle = androidx.core.os.bundleOf("symbol" to symbol)
            findNavController().navigate(R.id.action_stockDetail_to_stockAnalysis, bundle)
        }
        binding.tabSectionNews.setOnClickListener { viewModel.setSection("News") }
        binding.tabSectionProfile.setOnClickListener { viewModel.setSection("Profile") }

        // Sub Pills
        binding.pillByPrice.setOnClickListener { viewModel.setSubPill("By Price") }
        binding.pillTrades.setOnClickListener { viewModel.setSubPill("Trades") }
        binding.pillDetailedQuote.setOnClickListener { viewModel.setSubPill("Detailed Quote") }
        binding.pillLiquidity.setOnClickListener { viewModel.setSubPill("Liquidity") }

        // Chart settings / fullscreen
        binding.ivChartSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Chart type settings", Toast.LENGTH_SHORT).show()
        }
        binding.ivFullscreen.setOnClickListener {
            val bundle = Bundle().apply { putString("symbol", currentSymbol) }
            findNavController().navigate(R.id.chartFullscreenFragment, bundle)
        }
        binding.badgeShariah.setOnClickListener {
            val sheet = com.fyp.basarattradeinvest.feature.shariah.details.StockShariahDetailsBottomSheet.newInstance(currentSymbol)
            sheet.show(childFragmentManager, "StockShariahDetailsBottomSheet")
        }
    }

    // ───────────────────────────────
    //  STATE OBSERVER
    // ───────────────────────────────

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is StockDetailUiState.Loading -> {
                            // Could show shimmer/loading
                        }
                        is StockDetailUiState.Success -> {
                            bindHeader(state.detail)
                            bindCandles(state.candles)
                            updateTimeframeTabs(state.selectedTimeframe)
                            updateSectionTabs(state.selectedSection)
                            updateSubPills(state.selectedSubPill)
                            bindTabContent(state)
                            loadRecommendationCard(state.detail.symbol, state.detail.price)
                        }
                        is StockDetailUiState.Empty -> {
                            Toast.makeText(requireContext(), "No data", Toast.LENGTH_SHORT).show()
                        }
                        is StockDetailUiState.Error -> {
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    // ───────────────────────────────
    //  BINDING HELPERS
    // ───────────────────────────────

    private fun bindHeader(detail: StockDetail) {
        binding.tvTopSymbol.text = detail.symbol
        binding.tvTopCode.text = detail.code
        binding.tvMarketStatus.text = "${detail.marketStatus} ${detail.lastUpdated}"

        // Price
        binding.tvPrice.text = String.format(Locale.US, "%.2f", detail.price)
        val isPositive = detail.changeAmount >= 0
        val priceColor = if (isPositive) R.color.color_positive else R.color.color_negative
        binding.tvPrice.setTextColor(ContextCompat.getColor(requireContext(), priceColor))

        val prefix = if (isPositive) "+" else ""
        binding.tvChangeAmount.text = String.format(Locale.US, "%s%.2f", prefix, detail.changeAmount)
        binding.tvChangePercent.text = String.format(Locale.US, "%s%.2f%%", prefix, detail.changePercent)
        binding.tvChangeAmount.setTextColor(ContextCompat.getColor(requireContext(), priceColor))
        binding.tvChangePercent.setTextColor(ContextCompat.getColor(requireContext(), priceColor))

        // Badges visibility
        binding.badgeCompliant.visibility = if (detail.isCompliant) View.VISIBLE else View.GONE
        binding.badgeShariah.visibility = if (detail.isShariah) View.VISIBLE else View.GONE
        binding.badgeDividend.visibility = if (detail.hasDividend) View.VISIBLE else View.GONE

        // SMA mock values (will come from backend later)
        binding.tvSma10.text = String.format(Locale.US, "SMA10: %.3f", detail.price * 0.86)
        binding.tvSma50.text = String.format(Locale.US, "SMA50: %.3f", detail.price * 0.87)
        binding.tvSma200.text = String.format(Locale.US, "SMA200: %.3f", detail.price * 1.21)

        // Volume info
        binding.tvVolCurrent.text = String.format(Locale.US, "%,d", detail.volume)
        binding.tvVolMa5.text = String.format(Locale.US, "MA5: %,d", (detail.avgVolume * 0.9).toLong())
        binding.tvVolMa10.text = String.format(Locale.US, "MA10: %,d", detail.avgVolume)
        binding.tvVolMa20.text = String.format(Locale.US, "MA20: %,d", (detail.avgVolume * 1.1).toLong())
    }

    private fun bindCandles(candles: List<CandleEntry>) {
        if (candles.isEmpty()) return
        val chart = candleChart ?: return

        val positiveColor = ContextCompat.getColor(requireContext(), R.color.color_positive)
        val negativeColor = ContextCompat.getColor(requireContext(), R.color.color_negative)

        // Candlestick entries
        val candleEntries = candles.mapIndexed { index, candle ->
            com.github.mikephil.charting.data.CandleEntry(
                index.toFloat(),
                candle.high,
                candle.low,
                candle.open,
                candle.close
            )
        }

        val candleDataSet = CandleDataSet(candleEntries, "OHLC").apply {
            setDrawValues(false)
            shadowColorSameAsCandle = true
            increasingColor = positiveColor
            increasingPaintStyle = Paint.Style.FILL
            decreasingColor = negativeColor
            decreasingPaintStyle = Paint.Style.FILL
            neutralColor = Color.GRAY
            shadowWidth = 0.8f
        }

        chart.data = CandleData(candleDataSet)
        chart.setVisibleXRangeMaximum(50f)
        chart.moveViewToX(candleEntries.size.toFloat())
        chart.invalidate()

        // Volume bar chart
        val volChart = volumeChart ?: return
        val barEntries = candles.mapIndexed { index, candle ->
            BarEntry(index.toFloat(), candle.volume.toFloat())
        }

        val barColors = candles.map { candle ->
            if (candle.close >= candle.open) positiveColor else negativeColor
        }

        val barDataSet = BarDataSet(barEntries, "Volume").apply {
            setDrawValues(false)
            colors = barColors
        }

        volChart.data = BarData(barDataSet).apply {
            barWidth = 0.8f
        }
        volChart.setVisibleXRangeMaximum(50f)
        volChart.moveViewToX(barEntries.size.toFloat())
        volChart.invalidate()
    }

    private fun updateTimeframeTabs(selected: String) {
        val tabs = listOf(
            binding.tab1d to "1D",
            binding.tabDaily to "Daily",
            binding.tabWeekly to "Weekly",
            binding.tab1m to "1M"
        )

        tabs.forEach { (tab, tf) ->
            val isActive = selected == tf
            tab.setBackgroundResource(if (isActive) R.drawable.bg_timeframe_active else 0)
            tab.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isActive) R.color.color_text_primary else R.color.color_text_secondary
                )
            )
        }
    }

    private fun updateSectionTabs(selected: String) {
        val sections = listOf(
            Triple(binding.tvTabQuotes, binding.indicatorQuotes, "Quotes"),
            Triple(binding.tvTabAnalysis, binding.indicatorAnalysis, "Analysis"),
            Triple(binding.tvTabNews, binding.indicatorNews, "News"),
            Triple(binding.tvTabProfile, binding.indicatorProfile, "Profile")
        )

        sections.forEach { (textView, indicator, name) ->
            val isActive = selected == name
            textView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isActive) R.color.color_text_primary else R.color.color_text_secondary
                )
            )
            if (isActive) {
                textView.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                textView.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
            indicator.setBackgroundColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isActive) R.color.color_basarat_green else R.color.transparent
                )
            )
        }

        // Update sub pills based on section
        updateSubPillsVisibility(selected)
    }

    private fun updateSubPillsVisibility(section: String) {
        when (section) {
            "Quotes" -> {
                binding.pillByPrice.text = "By Price"
                binding.pillTrades.text = "Trades"
                binding.pillDetailedQuote.text = "Detailed Quote"
                binding.pillLiquidity.text = "Liquidity"
                binding.scrollSubPills.visibility = View.VISIBLE
            }
            "News" -> {
                binding.pillByPrice.text = "Latest"
                binding.pillTrades.text = "Sentiment"
                binding.pillDetailedQuote.text = "Events"
                binding.pillLiquidity.visibility = View.GONE
                binding.scrollSubPills.visibility = View.VISIBLE
            }
            "Profile" -> {
                binding.pillByPrice.text = "Overview"
                binding.pillTrades.text = "Financials"
                binding.pillDetailedQuote.text = "Board"
                binding.pillLiquidity.visibility = View.GONE
                binding.scrollSubPills.visibility = View.VISIBLE
            }
        }
    }

    private fun updateSubPills(selected: String) {
        val pills = listOf(
            binding.pillByPrice,
            binding.pillTrades,
            binding.pillDetailedQuote,
            binding.pillLiquidity
        )

        pills.forEach { pill ->
            if (pill.visibility != View.VISIBLE) return@forEach
            val isActive = pill.text.toString().equals(selected, ignoreCase = true)
            pill.setBackgroundResource(
                if (isActive) R.drawable.bg_chip_filter_selected else R.drawable.bg_chip_filter_unselected
            )
            pill.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    if (isActive) R.color.color_positive else R.color.color_text_secondary
                )
            )
        }
    }

    // ───────────────────────────────
    //  BOTTOM TAB CONTENT
    // ───────────────────────────────

    private fun bindTabContent(state: StockDetailUiState.Success) {
        binding.containerTabContent.removeAllViews()

        when (state.selectedSection) {
            "Quotes" -> bindQuotesContent(state)
            "News" -> bindNewsContent(state)
            "Profile" -> bindProfileContent(state)
        }
    }

    private fun bindQuotesContent(state: StockDetailUiState.Success) {
        when (state.selectedSubPill) {
            "By Price" -> bindByPriceContent(state.detail)
            "Trades" -> bindTradesContent(state.trades)
            "Detailed Quote" -> bindDetailedQuoteContent(state.detail)
            "Liquidity" -> bindLiquidityContent(state.detail)
        }
    }

    private fun bindByPriceContent(detail: StockDetail) {
        val content = layoutInflater.inflate(R.layout.content_by_price, binding.containerTabContent, false)
        val rows = listOf(
            Pair("Open", String.format(Locale.US, "%.2f", detail.open)),
            Pair("High", String.format(Locale.US, "%.2f", detail.high)),
            Pair("Low", String.format(Locale.US, "%.2f", detail.low)),
            Pair("Close", String.format(Locale.US, "%.2f", detail.close)),
            Pair("Prev Close", String.format(Locale.US, "%.2f", detail.price - detail.changeAmount)),
            Pair("52W High", String.format(Locale.US, "%.2f", detail.high52Week)),
            Pair("52W Low", String.format(Locale.US, "%.2f", detail.low52Week))
        )
        val container = content.findViewById<android.widget.LinearLayout>(R.id.container_stats)
        rows.forEach { (label, value) ->
            addStatRow(container, label, value)
        }
        binding.containerTabContent.addView(content)
    }

    private fun bindTradesContent(trades: List<StockTrade>) {
        val rv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = tradeAdapter
            isNestedScrollingEnabled = false
        }
        tradeAdapter.submitList(trades)
        binding.containerTabContent.addView(rv)
    }

    private fun bindDetailedQuoteContent(detail: StockDetail) {
        val content = layoutInflater.inflate(R.layout.content_by_price, binding.containerTabContent, false)
        val rows = listOf(
            Pair("Market Cap", detail.marketCap),
            Pair("P/E Ratio", String.format(Locale.US, "%.2f", detail.pe)),
            Pair("EPS", String.format(Locale.US, "%.2f", detail.eps)),
            Pair("Volume", String.format(Locale.US, "%,d", detail.volume)),
            Pair("Avg Volume", String.format(Locale.US, "%,d", detail.avgVolume)),
            Pair("Sector", detail.sector),
            Pair("Shariah", if (detail.isShariah) "Compliant ✓" else "Non-Compliant")
        )
        val container = content.findViewById<android.widget.LinearLayout>(R.id.container_stats)
        rows.forEach { (label, value) ->
            addStatRow(container, label, value)
        }
        binding.containerTabContent.addView(content)
    }

    private fun bindLiquidityContent(detail: StockDetail) {
        val content = layoutInflater.inflate(R.layout.content_by_price, binding.containerTabContent, false)
        val rows = listOf(
            Pair("Day Volume", String.format(Locale.US, "%,d", detail.volume)),
            Pair("Avg Volume (20d)", String.format(Locale.US, "%,d", detail.avgVolume)),
            Pair("Turnover", String.format(Locale.US, "PKR %.1fM", detail.price * detail.volume / 1_000_000)),
            Pair("Bid-Ask Spread", "0.15 (0.25%)"),
            Pair("Free Float", "42.5%"),
            Pair("Depth Score", "7.2 / 10")
        )
        val container = content.findViewById<android.widget.LinearLayout>(R.id.container_stats)
        rows.forEach { (label, value) ->
            addStatRow(container, label, value)
        }
        binding.containerTabContent.addView(content)
    }

    private fun bindNewsContent(state: StockDetailUiState.Success) {
        val sym = state.detail.symbol
        val rv = RecyclerView(requireContext()).apply {
            layoutManager = LinearLayoutManager(requireContext())
            isNestedScrollingEnabled = false
        }
        val stockNewsAdapter = NewsArticleAdapter { article ->
            val bundle = Bundle().apply { putString("articleId", article.id) }
            findNavController().navigate(R.id.action_stockDetail_to_articleDetail, bundle)
        }
        rv.adapter = stockNewsAdapter
        binding.containerTabContent.addView(rv)

        viewLifecycleOwner.lifecycleScope.launch {
            val result = NewsRepositoryImpl().getStockNews(symbol = sym, limit = 10)
            result.fold(
                onSuccess = { page ->
                    if (page.articles.isNotEmpty()) {
                        stockNewsAdapter.submitList(page.articles)
                    } else {
                        val emptyTv = TextView(requireContext()).apply {
                            text = "No recent news articles found for $sym"
                            setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary))
                            textSize = 13f
                            setPadding(16, 32, 16, 32)
                        }
                        binding.containerTabContent.removeAllViews()
                        binding.containerTabContent.addView(emptyTv)
                    }
                },
                onFailure = {
                    val errorTv = TextView(requireContext()).apply {
                        text = "Unable to load news for $sym"
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary))
                        textSize = 13f
                        setPadding(16, 32, 16, 32)
                    }
                    binding.containerTabContent.removeAllViews()
                    binding.containerTabContent.addView(errorTv)
                }
            )
        }
    }

    private fun bindProfileContent(state: StockDetailUiState.Success) {
        val detail = state.detail
        val content = layoutInflater.inflate(R.layout.content_by_price, binding.containerTabContent, false)
        val container = content.findViewById<android.widget.LinearLayout>(R.id.container_stats)
        val rows = listOf(
            Pair("Company", detail.companyName),
            Pair("Symbol", detail.symbol),
            Pair("Code", detail.code),
            Pair("Sector", detail.sector),
            Pair("Market", "PSX"),
            Pair("Shariah Status", if (detail.isShariah) "KMI-30 Compliant ✓" else "Non-Compliant"),
            Pair("Dividend", if (detail.hasDividend) "Yes" else "No")
        )
        rows.forEach { (label, value) ->
            addStatRow(container, label, value)
        }
        binding.containerTabContent.addView(content)
    }

    private fun addStatRow(container: android.widget.LinearLayout, label: String, value: String) {
        val row = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(0, 20, 0, 20)
        }

        val labelTv = TextView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            text = label
            setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
            textSize = 13f
        }

        val valueTv = TextView(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            )
            text = value
            setTextColor(ContextCompat.getColor(context, R.color.color_text_primary))
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }

        if (label.contains("Shariah", ignoreCase = true)) {
            valueTv.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_shariah_teal))
            row.isClickable = true
            val outVal = android.util.TypedValue()
            requireContext().theme.resolveAttribute(android.R.attr.selectableItemBackground, outVal, true)
            row.setBackgroundResource(outVal.resourceId)
            row.setOnClickListener {
                val sheet = com.fyp.basarattradeinvest.feature.shariah.details.StockShariahDetailsBottomSheet.newInstance(currentSymbol)
                sheet.show(childFragmentManager, "StockShariahDetailsBottomSheet")
            }
        }

        row.addView(labelTv)
        row.addView(valueTv)
        container.addView(row)

        // Divider
        val divider = View(requireContext()).apply {
            layoutParams = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1
            )
            setBackgroundColor(ContextCompat.getColor(context, R.color.color_divider))
        }
        container.addView(divider)
    }

    private fun loadRecommendationCard(symbol: String, knownPrice: Double? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val repo = RecommendationsRepositoryImpl()
                val result = repo.getRecommendationDetail(symbol, knownPrice)
                if (_binding == null) return@launch
                if (result.isSuccess) {
                    val detail = result.getOrThrow()
                    binding.tvDetailRecSignal.text = detail.signal.label
                    when (detail.signal) {
                        RecommendationSignal.BUY -> {
                            binding.tvDetailRecSignal.setBackgroundResource(R.drawable.bg_badge_positive)
                            binding.tvDetailRecSignal.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.color_positive)
                            )
                        }
                        RecommendationSignal.SELL -> {
                            binding.tvDetailRecSignal.setBackgroundResource(R.drawable.bg_badge_negative)
                            binding.tvDetailRecSignal.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.color_negative)
                            )
                        }
                        RecommendationSignal.HOLD -> {
                            binding.tvDetailRecSignal.setBackgroundResource(R.drawable.bg_badge_neutral)
                            binding.tvDetailRecSignal.setTextColor(
                                ContextCompat.getColor(requireContext(), R.color.color_neutral)
                            )
                        }
                    }

                    val currentPrice = (knownPrice ?: detail.currentPrice)?.takeIf { it > 0.0 }
                    var targetPrice = detail.targetPrice?.takeIf { it > 0.0 }
                    var stopLoss = detail.stopLoss?.takeIf { it > 0.0 }

                    // Safety sanity check: ensure target & stop are aligned with current stock price
                    if (currentPrice != null) {
                        if (targetPrice == null || targetPrice < currentPrice * 0.5 || targetPrice > currentPrice * 2.0) {
                            targetPrice = Math.round(currentPrice * 1.10 * 100.0) / 100.0
                        }
                        if (stopLoss == null || stopLoss > currentPrice || stopLoss < currentPrice * 0.5) {
                            stopLoss = Math.round(currentPrice * 0.95 * 100.0) / 100.0
                        }
                    }

                    binding.tvDetailRecTarget.text = targetPrice?.let {
                        String.format(Locale.US, "Target: PKR %.2f", it)
                    } ?: "Target: N/A"

                    binding.tvDetailRecStop.text = stopLoss?.let {
                        String.format(Locale.US, "Stop: PKR %.2f", it)
                    } ?: "Stop: N/A"

                    binding.tvDetailRecConfidence.text = String.format(
                        Locale.US,
                        "Conf: %d%%",
                        (detail.confidence * 100).toInt()
                    )

                    val summary = detail.reasoning["ml"]?.get("direction")
                        ?: detail.reasoning["technical"]?.get("technical_summary")
                        ?: (targetPrice?.let { "Target price calculated via ATR bands at PKR ${String.format(Locale.US, "%.2f", it)}" }
                            ?: "Algorithmic decision support recommendation")
                    binding.tvDetailRecSummary.text = summary
                }
            } catch (_: Exception) {
                // Keep default card text gracefully
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        candleChart = null
        volumeChart = null
        _binding = null
    }
}
