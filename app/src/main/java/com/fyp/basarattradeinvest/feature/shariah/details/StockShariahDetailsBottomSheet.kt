package com.fyp.basarattradeinvest.feature.shariah.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.BottomSheetStockShariahDetailsBinding
import com.fyp.basarattradeinvest.domain.model.shariah.DividendPurificationResult
import com.fyp.basarattradeinvest.domain.model.shariah.StockShariahCompliance
import com.fyp.basarattradeinvest.domain.model.shariah.StockShariahCriteria
import com.fyp.basarattradeinvest.feature.shariah.adapter.ShariahCriteriaAdapter
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.util.Locale

class StockShariahDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetStockShariahDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StockShariahDetailsViewModel by viewModels()
    private val criteriaAdapter by lazy { ShariahCriteriaAdapter() }

    private var stockSymbol: String = "LUCK"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        stockSymbol = arguments?.getString(ARG_SYMBOL) ?: "LUCK"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetStockShariahDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadStockShariah(stockSymbol)
    }

    private fun setupRecyclerView() {
        binding.rvShariahCriteria.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = criteriaAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupListeners() {
        binding.ivCloseSheet.setOnClickListener {
            dismiss()
        }

        binding.tabCriteria.setOnClickListener {
            showCriteriaSection()
        }

        binding.tabPurification.setOnClickListener {
            showPurificationSection()
        }

        binding.btnCalculatePurification.setOnClickListener {
            val qtyStr = binding.etPurificationQty.text?.toString()?.trim()
            val valStr = binding.etPurificationValue.text?.toString()?.trim()

            val qty = qtyStr?.toIntOrNull()
            val value = valStr?.toDoubleOrNull()

            if (qty == null || qty <= 0) {
                Toast.makeText(requireContext(), "Please enter a valid holding quantity (greater than 0)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (value == null || value <= 0.0) {
                Toast.makeText(requireContext(), "Please enter a valid total holding value (greater than 0)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.calculatePurification(stockSymbol, qty, value)
        }
    }

    private fun showCriteriaSection() {
        binding.tabCriteria.setBackgroundResource(R.drawable.bg_chip_filter_selected)
        binding.tabCriteria.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_positive))

        binding.tabPurification.setBackgroundResource(R.drawable.bg_chip_filter_unselected)
        binding.tabPurification.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary))

        binding.containerCriteriaSection.visibility = View.VISIBLE
        binding.containerPurificationSection.visibility = View.GONE
    }

    private fun showPurificationSection() {
        binding.tabPurification.setBackgroundResource(R.drawable.bg_chip_filter_selected)
        binding.tabPurification.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_positive))

        binding.tabCriteria.setBackgroundResource(R.drawable.bg_chip_filter_unselected)
        binding.tabCriteria.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary))

        binding.containerCriteriaSection.visibility = View.GONE
        binding.containerPurificationSection.visibility = View.VISIBLE
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.complianceState.collect { state ->
                        when (state) {
                            is StockShariahComplianceUiState.Loading -> {
                                // Keep existing content or show light loading
                            }
                            is StockShariahComplianceUiState.Success -> {
                                bindComplianceHeader(state.compliance)
                            }
                            is StockShariahComplianceUiState.Error -> {
                                binding.tvComplianceSummary.text = state.message
                            }
                        }
                    }
                }

                launch {
                    viewModel.criteriaState.collect { state ->
                        when (state) {
                            is StockShariahCriteriaUiState.Loading -> {
                                binding.progressCriteria.visibility = View.VISIBLE
                            }
                            is StockShariahCriteriaUiState.Success -> {
                                binding.progressCriteria.visibility = View.GONE
                                bindCriteriaList(state.criteria)
                            }
                            is StockShariahCriteriaUiState.Error -> {
                                binding.progressCriteria.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }

                launch {
                    viewModel.purificationState.collect { state ->
                        when (state) {
                            is PurificationCalculationUiState.Idle -> {
                                binding.progressPurification.visibility = View.GONE
                                binding.cardPurificationResult.visibility = View.GONE
                            }
                            is PurificationCalculationUiState.Calculating -> {
                                binding.progressPurification.visibility = View.VISIBLE
                                binding.cardPurificationResult.visibility = View.GONE
                            }
                            is PurificationCalculationUiState.Success -> {
                                binding.progressPurification.visibility = View.GONE
                                bindPurificationResult(state.result)
                            }
                            is PurificationCalculationUiState.Error -> {
                                binding.progressPurification.visibility = View.GONE
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindComplianceHeader(item: StockShariahCompliance) {
        val context = requireContext()
        binding.tvShariahSymbol.text = item.symbol
        binding.tvShariahSector.text = "${item.sector} • PSX"
        binding.tvShariahMethod.text = item.screeningMethod

        val scoreText = if (item.overallScore != null) {
            String.format(Locale.US, "%.1f / 100", item.overallScore)
        } else {
            "N/A"
        }
        binding.tvShariahScore.text = scoreText

        if (item.isCompliant) {
            binding.tvShariahBadge.text = "KMI-30 Compliant ✓"
            binding.tvShariahBadge.setBackgroundResource(R.drawable.bg_badge_shariah_compliant)
            binding.tvShariahBadge.setTextColor(ContextCompat.getColor(context, R.color.color_shariah_teal))
        } else {
            binding.tvShariahBadge.text = "Non-Compliant ✗"
            binding.tvShariahBadge.setBackgroundResource(R.drawable.bg_badge_shariah_non_compliant)
            binding.tvShariahBadge.setTextColor(ContextCompat.getColor(context, R.color.color_negative))
        }

        binding.tvComplianceSummary.text = item.complianceSummary
    }

    private fun bindCriteriaList(item: StockShariahCriteria) {
        criteriaAdapter.submitList(item.criteria)
    }

    private fun bindPurificationResult(result: DividendPurificationResult) {
        binding.cardPurificationResult.visibility = View.VISIBLE
        binding.tvPurificationAmount.text = String.format(Locale.US, "PKR %,.2f", result.purificationAmount)
        binding.tvPurificationRate.text = String.format(Locale.US, "%.4f%%", result.purificationRate * 100)

        if (!result.notes.isNullOrBlank()) {
            binding.tvPurificationNotes.visibility = View.VISIBLE
            binding.tvPurificationNotes.text = result.notes
        } else {
            binding.tvPurificationNotes.visibility = View.VISIBLE
            binding.tvPurificationNotes.text = "Deduct this purification amount from your total dividend income and disburse to charity."
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_SYMBOL = "arg_symbol"

        fun newInstance(symbol: String): StockShariahDetailsBottomSheet {
            return StockShariahDetailsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_SYMBOL, symbol)
                }
            }
        }
    }
}
