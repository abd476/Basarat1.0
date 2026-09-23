package com.fyp.basarattradeinvest.feature.stock.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentStockAnalysisBinding
import com.fyp.basarattradeinvest.domain.model.ForecastDirection
import com.fyp.basarattradeinvest.domain.model.ForecastHistory
import com.fyp.basarattradeinvest.domain.model.StockAnalysis
import com.fyp.basarattradeinvest.domain.model.StockForecast
import com.fyp.basarattradeinvest.domain.model.TechnicalSignal
import com.fyp.basarattradeinvest.feature.stock.analysis.adapter.ForecastHistoryAdapter
import kotlinx.coroutines.launch

class StockAnalysisFragment : Fragment() {

    private var _binding: FragmentStockAnalysisBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockAnalysisViewModel by viewModels()
    private val forecastHistoryAdapter by lazy { ForecastHistoryAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val symbol = arguments?.getString("symbol") ?: "LUCK"

        setupRecyclerView()
        setupClickListeners()
        observeUiState()

        viewModel.loadAnalysis(symbol)
    }

    private fun setupRecyclerView() {
        binding.rvForecastHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = forecastHistoryAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnTabTechnical.setOnClickListener {
            viewModel.selectTab(AnalysisTab.TECHNICAL)
        }

        binding.btnTabFundamentals.setOnClickListener {
            viewModel.selectTab(AnalysisTab.FUNDAMENTALS)
        }

        binding.btnTabForecast.setOnClickListener {
            viewModel.selectTab(AnalysisTab.FORECAST)
        }

        binding.btnTabSentiment.setOnClickListener {
            // Sentiment navigates to dedicated SentimentFragment
            val symbol = arguments?.getString("symbol") ?: "LUCK"
            val bundle = androidx.core.os.bundleOf("symbol" to symbol)
            findNavController().navigate(R.id.action_stockAnalysis_to_sentiment, bundle)
        }

        // Horizon selection pills
        binding.btnHorizon1d.setOnClickListener {
            viewModel.selectHorizon("1D")
        }

        binding.btnHorizon1w.setOnClickListener {
            viewModel.selectHorizon("1W")
        }

        binding.btnHorizon1m.setOnClickListener {
            viewModel.selectHorizon("1M")
        }

        // Retry buttons
        binding.btnForecastRetry.setOnClickListener {
            viewModel.retryForecast()
        }

        binding.btnHistoryRetry.setOnClickListener {
            viewModel.retryHistory()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is StockAnalysisUiState.Loading -> {
                            // Subtle loading if needed
                        }
                        is StockAnalysisUiState.Success -> {
                            renderAnalysis(state.analysis)
                            renderTab(state.selectedTab)
                            renderForecastSection(state.forecastState, state.selectedHorizon)
                            renderForecastHistorySection(state.historyState)
                        }
                        is StockAnalysisUiState.Empty -> {
                            // Handle empty state
                        }
                        is StockAnalysisUiState.Error -> {
                            // Handle analysis error
                        }
                    }
                }
            }
        }
    }

    private fun renderAnalysis(analysis: StockAnalysis) {
        // Stock Identity
        binding.tvStockSymbol.text = analysis.symbol
        binding.tvCompanyName.text = analysis.companyName
        binding.tvCurrentPrice.text = String.format("PKR %.2f", analysis.price)

        val sign = if (analysis.changeAmount >= 0) "+" else ""
        binding.tvPriceChange.text = String.format("%s%.2f (%s%.2f%%)", sign, analysis.changeAmount, sign, analysis.changePercent)

        val isPositive = analysis.changeAmount >= 0
        binding.tvPriceChange.setTextColor(
            ContextCompat.getColor(
                requireContext(),
                if (isPositive) R.color.color_positive else R.color.color_negative
            )
        )
        binding.tvPriceChange.setBackgroundResource(
            if (isPositive) R.drawable.bg_badge_positive else R.drawable.bg_badge_negative
        )

        // Technical Summary
        val tech = analysis.technical
        binding.tvCountBuy.text = tech.summary.buyCount.toString()
        binding.tvCountNeutral.text = tech.summary.neutralCount.toString()
        binding.tvCountSell.text = tech.summary.sellCount.toString()
        binding.tvProminentSignal.text = tech.summary.overallSignal.label

        // Oscillators
        tech.oscillators.forEach { indicator ->
            when (indicator.name) {
                "RSI (14)" -> {
                    binding.tvValRsi.text = indicator.value
                    binding.tvBadgeRsi.text = indicator.signal.label
                    applyBadgeStyle(binding.tvBadgeRsi, indicator.signal)
                }
                "MACD (12,26,9)" -> {
                    binding.tvValMacd.text = indicator.value
                    binding.tvBadgeMacd.text = indicator.signal.label
                    applyBadgeStyle(binding.tvBadgeMacd, indicator.signal)
                }
                "ADX (14)" -> {
                    binding.tvValAdx.text = indicator.value
                    binding.tvBadgeAdx.text = indicator.signal.label
                    applyBadgeStyle(binding.tvBadgeAdx, indicator.signal)
                }
                "Stochastic (14,3)" -> {
                    binding.tvValStoch.text = indicator.value
                    binding.tvBadgeStoch.text = indicator.signal.label
                    applyBadgeStyle(binding.tvBadgeStoch, indicator.signal)
                }
                "Bollinger Bands" -> {
                    binding.tvValBb.text = indicator.value
                    binding.tvBadgeBb.text = indicator.signal.label
                    applyBadgeStyle(binding.tvBadgeBb, indicator.signal)
                }
            }
        }

        // Fundamentals
        val fund = analysis.fundamentals
        binding.tvValPe.text = String.format("%.2fx", fund.peRatio)
        binding.tvValForwardPe.text = String.format("%.2fx", fund.forwardPe)
        binding.tvValPb.text = String.format("%.2fx", fund.pbRatio)
        binding.tvValEvEbitda.text = String.format("%.2fx", fund.evEbitda)
        binding.tvValPeg.text = String.format("%.2f", fund.pegRatio)

        binding.tvValEps.text = String.format("PKR %.2f", fund.eps)
        binding.tvValRoe.text = String.format("%.1f%%", fund.roe)
        binding.tvValRoa.text = String.format("%.1f%%", fund.roa)
        binding.tvValDivYield.text = String.format("%.2f%%", fund.dividendYield)
        binding.tvValNetMargin.text = String.format("%.1f%%", fund.netProfitMargin)

        binding.tvValDe.text = String.format("%.2f", fund.debtToEquity)
        binding.tvValCurrentRatio.text = String.format("%.2fx", fund.currentRatio)
        binding.tvValQuickRatio.text = String.format("%.2fx", fund.quickRatio)
        binding.tvValInterestCoverage.text = String.format("%.1fx", fund.interestCoverage)

        binding.tvValRevenue.text = String.format("%s (+%.1f%%)", fund.revenueTTM, fund.revenueGrowthYoY)
        binding.tvValNetIncome.text = String.format("%s (+%.1f%%)", fund.netIncomeTTM, fund.netIncomeGrowthYoY)
        binding.tvValFcf.text = fund.freeCashFlowTTM
    }

    private fun renderTab(tab: AnalysisTab) {
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_segmented_tab_active)
        val primaryText = ContextCompat.getColor(requireContext(), R.color.color_text_primary)
        val secondaryText = ContextCompat.getColor(requireContext(), R.color.color_text_secondary)

        // Reset all 3 content tabs (Sentiment is navigation-only, no local content)
        binding.btnTabTechnical.background = null
        binding.tvTabTechnicalText.setTextColor(secondaryText)
        binding.btnTabFundamentals.background = null
        binding.tvTabFundamentalsText.setTextColor(secondaryText)
        binding.btnTabForecast.background = null
        binding.tvTabForecastText.setTextColor(secondaryText)
        binding.btnTabSentiment.background = null
        binding.tvTabSentimentText.setTextColor(secondaryText)

        when (tab) {
            AnalysisTab.TECHNICAL -> {
                binding.btnTabTechnical.background = activeBg
                binding.tvTabTechnicalText.setTextColor(primaryText)

                binding.layoutTechnicalContent.visibility = View.VISIBLE
                binding.layoutFundamentalsContent.visibility = View.GONE
                binding.layoutForecastContent.visibility = View.GONE
            }
            AnalysisTab.FUNDAMENTALS -> {
                binding.btnTabFundamentals.background = activeBg
                binding.tvTabFundamentalsText.setTextColor(primaryText)

                binding.layoutTechnicalContent.visibility = View.GONE
                binding.layoutFundamentalsContent.visibility = View.VISIBLE
                binding.layoutForecastContent.visibility = View.GONE
            }
            AnalysisTab.FORECAST -> {
                binding.btnTabForecast.background = activeBg
                binding.tvTabForecastText.setTextColor(primaryText)

                binding.layoutTechnicalContent.visibility = View.GONE
                binding.layoutFundamentalsContent.visibility = View.GONE
                binding.layoutForecastContent.visibility = View.VISIBLE
            }
            AnalysisTab.SENTIMENT -> {
                // Navigation-only tab — content shown in SentimentFragment
                binding.btnTabSentiment.background = activeBg
                binding.tvTabSentimentText.setTextColor(primaryText)

                binding.layoutTechnicalContent.visibility = View.GONE
                binding.layoutFundamentalsContent.visibility = View.GONE
                binding.layoutForecastContent.visibility = View.GONE
            }
        }
    }

    private fun renderForecastSection(state: ForecastSectionState, selectedHorizon: String) {
        // Update Horizon Selector Buttons
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_segmented_tab_active)
        val primaryText = ContextCompat.getColor(requireContext(), R.color.color_text_primary)
        val secondaryText = ContextCompat.getColor(requireContext(), R.color.color_text_secondary)

        binding.btnHorizon1d.background = if (selectedHorizon == "1D") activeBg else null
        binding.btnHorizon1d.setTextColor(if (selectedHorizon == "1D") primaryText else secondaryText)

        binding.btnHorizon1w.background = if (selectedHorizon == "1W") activeBg else null
        binding.btnHorizon1w.setTextColor(if (selectedHorizon == "1W") primaryText else secondaryText)

        binding.btnHorizon1m.background = if (selectedHorizon == "1M") activeBg else null
        binding.btnHorizon1m.setTextColor(if (selectedHorizon == "1M") primaryText else secondaryText)

        when (state) {
            is ForecastSectionState.Loading -> {
                binding.layoutForecastLoading.visibility = View.VISIBLE
                binding.layoutForecastError.visibility = View.GONE
                binding.layoutForecastSuccess.visibility = View.GONE
            }
            is ForecastSectionState.Error -> {
                binding.layoutForecastLoading.visibility = View.GONE
                binding.layoutForecastError.visibility = View.VISIBLE
                binding.layoutForecastSuccess.visibility = View.GONE
                binding.tvForecastErrorMsg.text = state.message
            }
            is ForecastSectionState.Empty -> {
                binding.layoutForecastLoading.visibility = View.GONE
                binding.layoutForecastError.visibility = View.VISIBLE
                binding.layoutForecastSuccess.visibility = View.GONE
                binding.tvForecastErrorMsg.text = "No forecast data available for this horizon."
            }
            is ForecastSectionState.Success -> {
                binding.layoutForecastLoading.visibility = View.GONE
                binding.layoutForecastError.visibility = View.GONE
                binding.layoutForecastSuccess.visibility = View.VISIBLE
                bindForecastData(state.forecast)
            }
        }
    }

    private fun bindForecastData(forecast: StockForecast) {
        val context = requireContext()

        // Dates
        binding.tvForecastDates.text = "Target: ${forecast.targetDate} (As of ${forecast.asOfDate})"

        // Direction Badge
        binding.tvForecastDirectionBadge.text = forecast.direction.label.uppercase()
        when (forecast.direction) {
            ForecastDirection.BULLISH -> {
                binding.tvForecastDirectionBadge.setBackgroundResource(R.drawable.bg_badge_positive)
                binding.tvForecastDirectionBadge.setTextColor(ContextCompat.getColor(context, R.color.color_positive))
            }
            ForecastDirection.BEARISH -> {
                binding.tvForecastDirectionBadge.setBackgroundResource(R.drawable.bg_badge_negative)
                binding.tvForecastDirectionBadge.setTextColor(ContextCompat.getColor(context, R.color.color_negative))
            }
            ForecastDirection.SIDEWAYS -> {
                binding.tvForecastDirectionBadge.setBackgroundResource(R.drawable.bg_badge_neutral)
                binding.tvForecastDirectionBadge.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
            }
        }

        // Signal Rating
        val signalRating = forecast.signalRating ?: "NEUTRAL"
        binding.tvForecastSignalRating.text = signalRating.uppercase()
        when {
            signalRating.contains("Buy", ignoreCase = true) -> {
                binding.tvForecastSignalRating.setBackgroundResource(R.drawable.bg_badge_strong_buy)
                binding.tvForecastSignalRating.setTextColor(ContextCompat.getColor(context, R.color.color_basarat_green))
            }
            signalRating.contains("Sell", ignoreCase = true) -> {
                binding.tvForecastSignalRating.setBackgroundResource(R.drawable.bg_badge_negative)
                binding.tvForecastSignalRating.setTextColor(ContextCompat.getColor(context, R.color.color_negative))
            }
            else -> {
                binding.tvForecastSignalRating.setBackgroundResource(R.drawable.bg_badge_neutral)
                binding.tvForecastSignalRating.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
            }
        }

        // Confidence
        val confPct = (forecast.confidence * 100).toInt()
        binding.tvForecastConfidence.text = "$confPct% Confidence"

        // Meta (Model version & Gate reason)
        val modelVer = forecast.modelVersion ?: "ensemble"
        val gateReason = forecast.gateReason?.takeIf { it.isNotBlank() } ?: "standard"
        binding.tvForecastMeta.text = "Model: ${modelVer.replaceFirstChar { it.uppercase() }} • Decision: $gateReason"

        // Prices & Targets
        binding.tvForecastTargetPrice.text = forecast.targetPrice?.let { String.format("PKR %.2f", it) } ?: "N/A"
        binding.tvForecastUpsidePct.text = forecast.upsidePct?.let { String.format("%+,.1f%%", it) } ?: "--"

        binding.tvForecastStopLoss.text = forecast.stopLoss?.let { String.format("PKR %.2f", it) } ?: "N/A"
        binding.tvForecastDownsidePct.text = forecast.downsidePct?.let { String.format("%+,.1f%%", it) } ?: "--"

        binding.tvForecastRiskReward.text = forecast.riskRewardRatio?.let { String.format("%.2f", it) } ?: "N/A"
        binding.tvForecastCurrentPrice.text = forecast.currentPrice?.let { String.format("Ref: PKR %.2f", it) } ?: ""

        // Probability Distribution Meter
        val probs = forecast.probabilities
        if (probs != null) {
            binding.cardForecastProbabilities.visibility = View.VISIBLE
            val bullWeight = probs.bullish.coerceAtLeast(1.0).toFloat()
            val sideWeight = probs.sideways.coerceAtLeast(1.0).toFloat()
            val bearWeight = probs.bearish.coerceAtLeast(1.0).toFloat()

            (binding.barProbBullish.layoutParams as LinearLayout.LayoutParams).weight = bullWeight
            (binding.barProbSideways.layoutParams as LinearLayout.LayoutParams).weight = sideWeight
            (binding.barProbBearish.layoutParams as LinearLayout.LayoutParams).weight = bearWeight
            binding.layoutProbMeter.requestLayout()

            binding.tvProbBullishVal.text = String.format("Bullish: %.1f%%", probs.bullish)
            binding.tvProbSidewaysVal.text = String.format("Sideways: %.1f%%", probs.sideways)
            binding.tvProbBearishVal.text = String.format("Bearish: %.1f%%", probs.bearish)
        } else {
            binding.cardForecastProbabilities.visibility = View.GONE
        }

        // Model Ensemble Breakdown (GRU vs XGBoost)
        val gruModel = forecast.models["gru"]
        val xgbModel = forecast.models["xgb"]
        if (gruModel != null || xgbModel != null) {
            binding.cardModelBreakdown.visibility = View.VISIBLE
            if (gruModel != null) {
                binding.tvGruDirection.text = gruModel.direction.uppercase()
                binding.tvGruPcts.text = String.format("%.0f%% Bull • %.0f%% Bear • Gap %.1fpp", gruModel.bullishPct, gruModel.bearishPct, gruModel.gapPp)
            }
            if (xgbModel != null) {
                binding.tvXgbDirection.text = xgbModel.direction.uppercase()
                binding.tvXgbPcts.text = String.format("%.0f%% Bull • %.0f%% Bear • Gap %.1fpp", xgbModel.bullishPct, xgbModel.bearishPct, xgbModel.gapPp)
            }
        } else {
            binding.cardModelBreakdown.visibility = View.GONE
        }

        // Market Context
        val mc = forecast.marketContext
        if (mc != null) {
            binding.cardMarketContext.visibility = View.VISIBLE
            val m5d = mc.marketReturn5d?.let { String.format("%+,.1f%%", it * 100) } ?: "N/A"
            val m20d = mc.marketReturn20d?.let { String.format("%+,.1f%%", it * 100) } ?: "N/A"
            binding.tvContextMarketReturns.text = "$m5d / $m20d"

            val rel20d = mc.stockRelativeReturn20d?.let { String.format("%+,.1f%% relative", it * 100) } ?: "N/A"
            binding.tvContextStockRelative.text = rel20d
        } else {
            binding.cardMarketContext.visibility = View.GONE
        }
    }

    private fun renderForecastHistorySection(state: ForecastHistorySectionState) {
        when (state) {
            is ForecastHistorySectionState.Loading -> {
                binding.historyProgress.visibility = View.VISIBLE
                binding.layoutHistoryError.visibility = View.GONE
                binding.tvHistoryEmpty.visibility = View.GONE
                binding.rvForecastHistory.visibility = View.GONE
            }
            is ForecastHistorySectionState.Error -> {
                binding.historyProgress.visibility = View.GONE
                binding.layoutHistoryError.visibility = View.VISIBLE
                binding.tvHistoryEmpty.visibility = View.GONE
                binding.rvForecastHistory.visibility = View.GONE
                binding.tvHistoryErrorMsg.text = state.message
            }
            is ForecastHistorySectionState.Empty -> {
                binding.historyProgress.visibility = View.GONE
                binding.layoutHistoryError.visibility = View.GONE
                binding.tvHistoryEmpty.visibility = View.VISIBLE
                binding.rvForecastHistory.visibility = View.GONE
                binding.tvHistoryAccuracyVal.text = "N/A"
                binding.tvHistoryCountVal.text = "0 Records"
            }
            is ForecastHistorySectionState.Success -> {
                binding.historyProgress.visibility = View.GONE
                binding.layoutHistoryError.visibility = View.GONE
                binding.tvHistoryEmpty.visibility = View.GONE
                binding.rvForecastHistory.visibility = View.VISIBLE
                bindForecastHistoryData(state.history)
            }
        }
    }

    private fun bindForecastHistoryData(history: ForecastHistory) {
        // Overall accuracy from API
        val acc = history.accuracy
        if (acc != null) {
            val accPct = if (acc <= 1.0) acc * 100 else acc
            binding.tvHistoryAccuracyVal.text = String.format("%.1f%%", accPct)
        } else {
            binding.tvHistoryAccuracyVal.text = "N/A"
        }

        binding.tvHistoryCountVal.text = "${history.count} Records"
        forecastHistoryAdapter.submitList(history.history)
    }

    private fun applyBadgeStyle(textView: android.widget.TextView, signal: TechnicalSignal) {
        when (signal) {
            TechnicalSignal.STRONG_BUY -> {
                textView.setBackgroundResource(R.drawable.bg_badge_strong_buy)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_basarat_green))
            }
            TechnicalSignal.BUY -> {
                textView.setBackgroundResource(R.drawable.bg_badge_positive)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_positive))
            }
            TechnicalSignal.NEUTRAL -> {
                textView.setBackgroundResource(R.drawable.bg_badge_neutral)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary))
            }
            TechnicalSignal.SELL, TechnicalSignal.STRONG_SELL -> {
                textView.setBackgroundResource(R.drawable.bg_badge_negative)
                textView.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_negative))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
