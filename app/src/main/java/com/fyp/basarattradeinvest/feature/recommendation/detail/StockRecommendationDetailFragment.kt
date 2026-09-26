package com.fyp.basarattradeinvest.feature.recommendation.detail

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
import com.fyp.basarattradeinvest.databinding.FragmentStockRecommendationDetailBinding
import com.fyp.basarattradeinvest.domain.model.recommendations.RecommendationDetail
import com.fyp.basarattradeinvest.domain.model.recommendations.RecommendationSignal
import com.fyp.basarattradeinvest.domain.model.recommendations.TargetStop
import kotlinx.coroutines.launch
import java.util.Locale

class StockRecommendationDetailFragment : Fragment() {

    private var _binding: FragmentStockRecommendationDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockRecommendationDetailViewModel by viewModels()
    private var symbol: String = "OGDC"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStockRecommendationDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        symbol = arguments?.getString("symbol") ?: "OGDC"

        setupListeners()
        observeUiState()

        viewModel.ensureLoaded(symbol)
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnViewStock.setOnClickListener {
            val bundle = Bundle().apply { putString("symbol", symbol) }
            findNavController().navigate(R.id.stockDetailFragment, bundle)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderUiState(state)
                }
            }
        }
    }

    private fun renderUiState(state: StockRecommendationDetailUiState) {
        binding.layoutLoading.visibility = View.GONE
        binding.layoutError.visibility = View.GONE
        binding.scrollContent.visibility = View.GONE

        when (state) {
            is StockRecommendationDetailUiState.Loading -> {
                binding.layoutLoading.visibility = View.VISIBLE
            }
            is StockRecommendationDetailUiState.Success -> {
                binding.scrollContent.visibility = View.VISIBLE
                bindData(state.detail, state.targetStop)
            }
            is StockRecommendationDetailUiState.Error -> {
                binding.layoutError.visibility = View.VISIBLE
                binding.tvErrorMessage.text = state.message
            }
        }
    }

    private fun bindData(detail: RecommendationDetail, targetStop: TargetStop?) {
        val context = requireContext()

        binding.tvTitleSymbol.text = "${detail.symbol} Recommendation"
        binding.tvSymbol.text = detail.symbol
        binding.tvSignalBadge.text = detail.signal.label

        when (detail.signal) {
            RecommendationSignal.BUY -> {
                binding.tvSignalBadge.setBackgroundResource(R.drawable.bg_badge_positive)
                binding.tvSignalBadge.setTextColor(ContextCompat.getColor(context, R.color.color_positive))
            }
            RecommendationSignal.SELL -> {
                binding.tvSignalBadge.setBackgroundResource(R.drawable.bg_badge_negative)
                binding.tvSignalBadge.setTextColor(ContextCompat.getColor(context, R.color.color_negative))
            }
            RecommendationSignal.HOLD -> {
                binding.tvSignalBadge.setBackgroundResource(R.drawable.bg_badge_neutral)
                binding.tvSignalBadge.setTextColor(ContextCompat.getColor(context, R.color.color_neutral))
            }
        }

        val sign = if (detail.compositeScore >= 0) "+" else ""
        binding.tvCompositeScore.text = String.format(Locale.US, "Score: %s%.2f", sign, detail.compositeScore)
        binding.tvConfidenceLabel.text = String.format(Locale.US, "Engine Confidence: %d%%", (detail.confidence * 100).toInt())

        if (detail.currentPrice != null) {
            binding.tvCurrentPrice.text = String.format(Locale.US, "Current Market Price: PKR %.2f", detail.currentPrice)
        } else if (targetStop?.currentPrice != null) {
            binding.tvCurrentPrice.text = String.format(Locale.US, "Current Market Price: PKR %.2f", targetStop.currentPrice)
        } else {
            binding.tvCurrentPrice.text = "Current Market Price: N/A"
        }

        // Target / Stop Section
        val target = targetStop?.targetPrice ?: detail.targetPrice
        val stop = targetStop?.stopLoss ?: detail.stopLoss

        if (target != null) {
            binding.tvDetailTargetPrice.text = String.format(Locale.US, "PKR %.2f", target)
        } else {
            binding.tvDetailTargetPrice.text = "N/A"
        }

        if (stop != null) {
            binding.tvDetailStopLoss.text = String.format(Locale.US, "PKR %.2f", stop)
        } else {
            binding.tvDetailStopLoss.text = "N/A"
        }

        if (targetStop?.upsidePct != null) {
            val upsideSign = if (targetStop.upsidePct >= 0) "+" else ""
            binding.tvUpsidePct.text = String.format(Locale.US, "%s%.2f%% upside", upsideSign, targetStop.upsidePct)
            binding.tvUpsidePct.visibility = View.VISIBLE
        } else {
            binding.tvUpsidePct.visibility = View.GONE
        }

        if (targetStop?.downsidePct != null) {
            binding.tvDownsidePct.text = String.format(Locale.US, "%.2f%% downside", targetStop.downsidePct)
            binding.tvDownsidePct.visibility = View.VISIBLE
        } else {
            binding.tvDownsidePct.visibility = View.GONE
        }

        val expectedRange = targetStop?.expectedRange ?: detail.expectedRange
        val atr = targetStop?.atr14 ?: detail.atr14
        if (expectedRange != null) {
            val atrText = if (atr != null) " (ATR-14: ${String.format(Locale.US, "%.2f", atr)})" else ""
            binding.tvExpectedRange.text = String.format(
                Locale.US,
                "Expected Range: PKR %.2f — PKR %.2f%s",
                expectedRange.low,
                expectedRange.high,
                atrText
            )
            binding.tvExpectedRange.visibility = View.VISIBLE
        } else {
            binding.tvExpectedRange.visibility = View.GONE
        }

        // Signal Breakdown
        val ml = detail.individualSignals["ml"] ?: 0.0
        val tech = detail.individualSignals["technical"] ?: 0.0
        val fund = detail.individualSignals["fundamental"] ?: 0.0

        binding.tvSignalMl.text = formatScore(ml)
        binding.tvSignalTech.text = formatScore(tech)
        binding.tvSignalFund.text = formatScore(fund)

        // Model Reasoning
        val reasoningBuilder = StringBuilder()
        detail.reasoning.forEach { (source, map) ->
            val sourceTitle = source.uppercase()
            val entries = map.values.joinToString(", ")
            reasoningBuilder.append("• $sourceTitle: $entries\n")
        }
        val reasoningText = reasoningBuilder.toString().trim()
        binding.tvReasoningSummary.text = if (reasoningText.isNotEmpty()) reasoningText else "Model reasoning generated from historical indicators and ML sequences."
    }

    private fun formatScore(score: Double): String {
        val sign = if (score >= 0) "+" else ""
        return String.format(Locale.US, "%s%.2f", sign, score)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
