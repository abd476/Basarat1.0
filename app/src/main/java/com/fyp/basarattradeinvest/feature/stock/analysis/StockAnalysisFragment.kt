package com.fyp.basarattradeinvest.feature.stock.analysis

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentStockAnalysisBinding
import com.fyp.basarattradeinvest.domain.model.StockAnalysis
import com.fyp.basarattradeinvest.domain.model.TechnicalSignal
import kotlinx.coroutines.launch

class StockAnalysisFragment : Fragment() {

    private var _binding: FragmentStockAnalysisBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockAnalysisViewModel by viewModels()

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

        setupClickListeners()
        observeUiState()

        viewModel.loadAnalysis(symbol)
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
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is StockAnalysisUiState.Loading -> {
                            // Can show subtle progress if needed
                        }
                        is StockAnalysisUiState.Success -> {
                            renderAnalysis(state.analysis)
                            renderTab(state.selectedTab)
                        }
                        is StockAnalysisUiState.Empty -> {
                            // Handle empty
                        }
                        is StockAnalysisUiState.Error -> {
                            // Handle error
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

    private fun renderTab(tab: AnalysisTab) {
        val activeBg = ContextCompat.getDrawable(requireContext(), R.drawable.bg_segmented_tab_active)
        val primaryText = ContextCompat.getColor(requireContext(), R.color.color_text_primary)
        val secondaryText = ContextCompat.getColor(requireContext(), R.color.color_text_secondary)

        when (tab) {
            AnalysisTab.TECHNICAL -> {
                binding.btnTabTechnical.background = activeBg
                binding.tvTabTechnicalText.setTextColor(primaryText)

                binding.btnTabFundamentals.background = null
                binding.tvTabFundamentalsText.setTextColor(secondaryText)

                binding.layoutTechnicalContent.visibility = View.VISIBLE
                binding.layoutFundamentalsContent.visibility = View.GONE
            }
            AnalysisTab.FUNDAMENTALS -> {
                binding.btnTabFundamentals.background = activeBg
                binding.tvTabFundamentalsText.setTextColor(primaryText)

                binding.btnTabTechnical.background = null
                binding.tvTabTechnicalText.setTextColor(secondaryText)

                binding.layoutTechnicalContent.visibility = View.GONE
                binding.layoutFundamentalsContent.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
