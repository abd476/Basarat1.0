package com.fyp.basarattradeinvest.feature.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fyp.basarattradeinvest.databinding.BottomSheetCreateAlertRuleBinding
import com.fyp.basarattradeinvest.domain.model.AlertCondition
import com.fyp.basarattradeinvest.domain.model.AlertRule
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.util.Locale

class CreateAlertRuleBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetCreateAlertRuleBinding? = null
    private val binding get() = _binding!!

    // Shared or parent viewModel
    private var alertsViewModel: AlertsViewModel? = null

    // For Edit Mode or Pre-fill
    private var editingRule: AlertRule? = null
    private var prefilledSymbol: String? = null

    private var selectedCondition: AlertCondition = AlertCondition.PRICE_ABOVE

    fun setViewModel(viewModel: AlertsViewModel) {
        this.alertsViewModel = viewModel
    }

    fun setPrefilledSymbol(symbol: String?) {
        this.prefilledSymbol = symbol
    }

    fun setEditRule(rule: AlertRule) {
        this.editingRule = rule
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetCreateAlertRuleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupConditionsDropdown()
        setupSymbolChips()
        populateInitialData()
        setupClickListeners()
        observeMutationState()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupConditionsDropdown() {
        val conditions = AlertCondition.entries
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            conditions.map { it.label }
        )
        binding.actvCondition.setAdapter(adapter)

        binding.actvCondition.setOnItemClickListener { _, _, position, _ ->
            selectedCondition = conditions[position]
            updateThresholdLabels(selectedCondition)
        }
    }

    private fun updateThresholdLabels(condition: AlertCondition) {
        binding.tvThresholdLabel.text = when (condition) {
            AlertCondition.PRICE_ABOVE, AlertCondition.PRICE_BELOW -> "Threshold Price (PKR)"
            AlertCondition.CHANGE_PERCENT_ABOVE, AlertCondition.CHANGE_PERCENT_BELOW -> "Threshold Change (%)"
            AlertCondition.VOLUME_ABOVE -> "Threshold Volume (Shares)"
            AlertCondition.RSI_ABOVE, AlertCondition.RSI_BELOW -> "Threshold RSI (0 - 100)"
        }
        binding.etThreshold.hint = when (condition) {
            AlertCondition.PRICE_ABOVE, AlertCondition.PRICE_BELOW -> "e.g. 150.00"
            AlertCondition.CHANGE_PERCENT_ABOVE, AlertCondition.CHANGE_PERCENT_BELOW -> "e.g. 5.0"
            AlertCondition.VOLUME_ABOVE -> "e.g. 1000000"
            AlertCondition.RSI_ABOVE -> "e.g. 70.0"
            AlertCondition.RSI_BELOW -> "e.g. 30.0"
        }
    }

    private fun setupSymbolChips() {
        binding.chipSymLuck.setOnClickListener { binding.etStockSymbol.setText("LUCK") }
        binding.chipSymOgdc.setOnClickListener { binding.etStockSymbol.setText("OGDC") }
        binding.chipSymEngro.setOnClickListener { binding.etStockSymbol.setText("ENGRO") }
        binding.chipSymSys.setOnClickListener { binding.etStockSymbol.setText("SYS") }
        binding.chipSymHbl.setOnClickListener { binding.etStockSymbol.setText("HBL") }
    }

    private fun populateInitialData() {
        if (editingRule != null) {
            val rule = editingRule!!
            binding.tvSheetTitle.text = "Edit Alert Rule"
            binding.btnSaveRule.text = "Update Alert Rule"
            binding.etStockSymbol.setText(rule.stockSymbol ?: "")

            val conditionEnum = AlertCondition.fromKey(rule.condition)
            selectedCondition = conditionEnum
            binding.actvCondition.setText(conditionEnum.label, false)
            updateThresholdLabels(conditionEnum)

            binding.etThreshold.setText(rule.threshold.toString())

            binding.layoutEditActiveToggle.visibility = View.VISIBLE
            binding.switchRuleEnabled.isChecked = rule.isActive
        } else {
            binding.tvSheetTitle.text = "Create Alert Rule"
            binding.btnSaveRule.text = "Create Alert Rule"
            if (!prefilledSymbol.isNullOrBlank()) {
                binding.etStockSymbol.setText(prefilledSymbol!!.uppercase(Locale.ROOT))
            }
            binding.actvCondition.setText(AlertCondition.PRICE_ABOVE.label, false)
            updateThresholdLabels(AlertCondition.PRICE_ABOVE)
            binding.layoutEditActiveToggle.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnCloseSheet.setOnClickListener {
            dismiss()
        }

        binding.btnSaveRule.setOnClickListener {
            val symbol = binding.etStockSymbol.text?.toString()?.trim()
            val thresholdStr = binding.etThreshold.text?.toString()?.trim()

            if (thresholdStr.isNullOrBlank()) {
                binding.tilThreshold.error = "Please enter a valid threshold value"
                return@setOnClickListener
            }

            val threshold = thresholdStr.toDoubleOrNull()
            if (threshold == null || threshold <= 0.0) {
                binding.tilThreshold.error = "Threshold must be a positive number"
                return@setOnClickListener
            }
            binding.tilThreshold.error = null

            val vm = alertsViewModel ?: return@setOnClickListener

            if (editingRule != null) {
                val rule = editingRule!!
                vm.updateRule(
                    ruleId = rule.id,
                    stockId = symbol?.ifBlank { null },
                    condition = selectedCondition.key,
                    threshold = threshold,
                    isActive = binding.switchRuleEnabled.isChecked
                )
            } else {
                vm.createRule(
                    stockId = symbol?.ifBlank { null },
                    condition = selectedCondition.key,
                    threshold = threshold
                )
            }
        }
    }

    private fun observeMutationState() {
        val vm = alertsViewModel ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.mutationState.collect { state ->
                    when (state) {
                        is AlertMutationState.Idle -> {
                            binding.btnSaveRule.isEnabled = true
                            binding.progressSaving.visibility = View.GONE
                        }
                        is AlertMutationState.Loading -> {
                            binding.btnSaveRule.isEnabled = false
                            binding.progressSaving.visibility = View.VISIBLE
                        }
                        is AlertMutationState.Success -> {
                            binding.progressSaving.visibility = View.GONE
                            binding.btnSaveRule.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            vm.resetMutationState()
                            dismiss()
                        }
                        is AlertMutationState.Error -> {
                            binding.progressSaving.visibility = View.GONE
                            binding.btnSaveRule.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            vm.resetMutationState()
                        }
                    }
                }
            }
        }
    }

    companion object {
        const val TAG = "CreateAlertRuleBottomSheet"

        fun newInstance(
            symbol: String? = null,
            editRule: AlertRule? = null
        ): CreateAlertRuleBottomSheet {
            return CreateAlertRuleBottomSheet().apply {
                setPrefilledSymbol(symbol)
                if (editRule != null) {
                    setEditRule(editRule)
                }
            }
        }
    }
}
