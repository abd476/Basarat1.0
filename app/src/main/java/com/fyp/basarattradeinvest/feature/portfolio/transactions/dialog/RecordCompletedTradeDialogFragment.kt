package com.fyp.basarattradeinvest.feature.portfolio.transactions.dialog

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.fyp.basarattradeinvest.data.repository.PortfolioRepositoryImpl
import com.fyp.basarattradeinvest.databinding.DialogRecordCompletedTradeBinding
import com.fyp.basarattradeinvest.domain.usecase.portfolio.RecordCompletedTradeUseCase
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordCompletedTradeDialogFragment : DialogFragment() {

    private var _binding: DialogRecordCompletedTradeBinding? = null
    private val binding get() = _binding!!

    private val recordTradeUseCase = RecordCompletedTradeUseCase(PortfolioRepositoryImpl())

    var onTradeRecorded: (() -> Unit)? = null

    private val currencyFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRecordCompletedTradeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        binding.etBuyDate.setText(today)
        binding.etSellDate.setText(today)

        binding.btnCancelTrade.setOnClickListener {
            dismiss()
        }

        binding.btnSubmitTrade.setOnClickListener {
            submitTrade()
        }
    }

    private fun submitTrade() {
        binding.tvTradeError.visibility = View.GONE

        val symbol = binding.etTradeSymbol.text?.toString()?.trim()?.uppercase().orEmpty()
        val qty = binding.etTradeQuantity.text?.toString()?.toDoubleOrNull()
        val buyPrice = binding.etBuyPrice.text?.toString()?.toDoubleOrNull()
        val buyDate = binding.etBuyDate.text?.toString()?.trim().orEmpty()
        val buyFee = binding.etBuyFee.text?.toString()?.toDoubleOrNull() ?: 0.0
        val sellPrice = binding.etSellPrice.text?.toString()?.toDoubleOrNull()
        val sellDate = binding.etSellDate.text?.toString()?.trim().orEmpty()
        val sellFee = binding.etSellFee.text?.toString()?.toDoubleOrNull() ?: 0.0

        if (symbol.isBlank()) {
            showError("Stock symbol is required.")
            return
        }
        if (qty == null || qty <= 0) {
            showError("Please enter valid traded shares.")
            return
        }
        if (buyPrice == null || buyPrice <= 0) {
            showError("Please enter a valid buy price.")
            return
        }
        if (buyDate.isBlank() || !buyDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            showError("Buy date must be in YYYY-MM-DD format.")
            return
        }
        if (sellPrice == null || sellPrice <= 0) {
            showError("Please enter a valid sell price.")
            return
        }
        if (sellDate.isBlank() || !sellDate.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            showError("Sell date must be in YYYY-MM-DD format.")
            return
        }

        binding.btnSubmitTrade.isEnabled = false
        binding.btnCancelTrade.isEnabled = false

        lifecycleScope.launch {
            val result = recordTradeUseCase(
                symbol = symbol,
                quantity = qty,
                buyPrice = buyPrice,
                buyDate = buyDate,
                buyFee = buyFee,
                sellPrice = sellPrice,
                sellDate = sellDate,
                sellFee = sellFee
            )

            if (result.isSuccess) {
                val trade = result.getOrThrow()
                onTradeRecorded?.invoke()

                binding.cardTradeResult.visibility = View.VISIBLE
                val prefix = if (trade.realizedPnl >= 0) "+" else ""
                binding.tvResultPnl.text = String.format(
                    Locale.US,
                    "Realized P&L: %sPKR %s (%.2f%%)",
                    prefix,
                    currencyFormat.format(trade.realizedPnl),
                    trade.realizedPnlPercent
                )
                if (trade.realizedPnl >= 0) {
                    binding.tvResultPnl.setTextColor(Color.parseColor("#18B981"))
                } else {
                    binding.tvResultPnl.setTextColor(Color.parseColor("#EF4444"))
                }

                binding.tvResultDetails.text = String.format(
                    Locale.US,
                    "Held for %d days • Invested: PKR %s • Proceeds: PKR %s",
                    trade.holdingPeriodDays,
                    currencyFormat.format(trade.totalInvested),
                    currencyFormat.format(trade.totalProceeds)
                )

                binding.btnSubmitTrade.text = "Done"
                binding.btnSubmitTrade.isEnabled = true
                binding.btnSubmitTrade.setOnClickListener {
                    dismiss()
                }
            } else {
                binding.btnSubmitTrade.isEnabled = true
                binding.btnCancelTrade.isEnabled = true
                showError(result.exceptionOrNull()?.message ?: "Failed to record completed trade.")
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvTradeError.text = msg
        binding.tvTradeError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
