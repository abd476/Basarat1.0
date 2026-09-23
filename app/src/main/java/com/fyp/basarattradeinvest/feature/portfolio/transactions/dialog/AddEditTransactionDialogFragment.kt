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
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.data.repository.PortfolioRepositoryImpl
import com.fyp.basarattradeinvest.databinding.DialogAddEditTransactionBinding
import com.fyp.basarattradeinvest.domain.usecase.portfolio.CreateTransactionUseCase
import com.fyp.basarattradeinvest.domain.usecase.portfolio.UpdateTransactionUseCase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddEditTransactionDialogFragment : DialogFragment() {

    private var _binding: DialogAddEditTransactionBinding? = null
    private val binding get() = _binding!!

    private var isEditMode: Boolean = false
    private var transactionId: String? = null
    private var prefilledSymbol: String? = null
    private var selectedType: String = "BUY"

    private val createUseCase = CreateTransactionUseCase(PortfolioRepositoryImpl())
    private val updateUseCase = UpdateTransactionUseCase(PortfolioRepositoryImpl())

    var onTransactionSaved: (() -> Unit)? = null

    companion object {
        private const val ARG_IS_EDIT = "arg_is_edit"
        private const val ARG_TX_ID = "arg_tx_id"
        private const val ARG_SYMBOL = "arg_symbol"
        private const val ARG_QTY = "arg_qty"
        private const val ARG_PRICE = "arg_price"
        private const val ARG_FEE = "arg_fee"
        private const val ARG_DATE = "arg_date"
        private const val ARG_TYPE = "arg_type"

        fun newInstanceForCreate(prefilledSymbol: String? = null): AddEditTransactionDialogFragment {
            return AddEditTransactionDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_EDIT, false)
                    putString(ARG_SYMBOL, prefilledSymbol)
                }
            }
        }

        fun newInstanceForEdit(
            txId: String,
            symbol: String,
            type: String,
            qty: Double,
            price: Double,
            fee: Double,
            date: String
        ): AddEditTransactionDialogFragment {
            return AddEditTransactionDialogFragment().apply {
                arguments = Bundle().apply {
                    putBoolean(ARG_IS_EDIT, true)
                    putString(ARG_TX_ID, txId)
                    putString(ARG_SYMBOL, symbol)
                    putString(ARG_TYPE, type)
                    putDouble(ARG_QTY, qty)
                    putDouble(ARG_PRICE, price)
                    putDouble(ARG_FEE, fee)
                    putString(ARG_DATE, date)
                }
            }
        }
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
        _binding = DialogAddEditTransactionBinding.inflate(inflater, container, false)
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

        isEditMode = arguments?.getBoolean(ARG_IS_EDIT) ?: false
        transactionId = arguments?.getString(ARG_TX_ID)
        prefilledSymbol = arguments?.getString(ARG_SYMBOL)
        selectedType = arguments?.getString(ARG_TYPE) ?: "BUY"

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        if (isEditMode) {
            binding.tvDialogTitle.text = "Edit Transaction"
            binding.tvDialogSubtitle.text = "Update quantity, price, fee, or execution date."
            binding.layoutTypeToggle.visibility = View.GONE
            binding.tilSymbol.isEnabled = false

            binding.etSymbol.setText(prefilledSymbol ?: "")
            binding.etQuantity.setText(arguments?.getDouble(ARG_QTY)?.toString() ?: "")
            binding.etPrice.setText(arguments?.getDouble(ARG_PRICE)?.toString() ?: "")
            binding.etFee.setText(arguments?.getDouble(ARG_FEE)?.toString() ?: "0.0")
            binding.etDate.setText(arguments?.getString(ARG_DATE) ?: "")
        } else {
            binding.tvDialogTitle.text = "Record Transaction"
            binding.tvDialogSubtitle.text = "Add a BUY or SELL order to your portfolio."
            binding.layoutTypeToggle.visibility = View.VISIBLE
            binding.tilSymbol.isEnabled = true

            if (!prefilledSymbol.isNullOrBlank()) {
                binding.etSymbol.setText(prefilledSymbol)
            }

            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            binding.etDate.setText(today)
            updateTypeToggleUI()
        }
    }

    private fun updateTypeToggleUI() {
        if (selectedType == "BUY") {
            binding.btnSelectBuy.setBackgroundColor(Color.parseColor("#18B981"))
            binding.btnSelectBuy.setTextColor(Color.WHITE)
            binding.btnSelectSell.setBackgroundColor(Color.TRANSPARENT)
            binding.btnSelectSell.setTextColor(Color.parseColor("#667085"))
        } else {
            binding.btnSelectSell.setBackgroundColor(Color.parseColor("#EF4444"))
            binding.btnSelectSell.setTextColor(Color.WHITE)
            binding.btnSelectBuy.setBackgroundColor(Color.TRANSPARENT)
            binding.btnSelectBuy.setTextColor(Color.parseColor("#667085"))
        }
    }

    private fun setupListeners() {
        binding.btnSelectBuy.setOnClickListener {
            selectedType = "BUY"
            updateTypeToggleUI()
        }

        binding.btnSelectSell.setOnClickListener {
            selectedType = "SELL"
            updateTypeToggleUI()
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSubmit.setOnClickListener {
            submitForm()
        }
    }

    private fun submitForm() {
        binding.tvFormError.visibility = View.GONE

        val symbol = binding.etSymbol.text?.toString()?.trim()?.uppercase().orEmpty()
        val qty = binding.etQuantity.text?.toString()?.toDoubleOrNull()
        val price = binding.etPrice.text?.toString()?.toDoubleOrNull()
        val fee = binding.etFee.text?.toString()?.toDoubleOrNull() ?: 0.0
        val date = binding.etDate.text?.toString()?.trim().orEmpty()

        if (symbol.isBlank()) {
            showError("Stock symbol is required.")
            return
        }
        if (qty == null || qty <= 0) {
            showError("Please enter a valid positive quantity.")
            return
        }
        if (price == null || price <= 0) {
            showError("Please enter a valid positive execution price.")
            return
        }
        if (date.isBlank() || !date.matches(Regex("^\\d{4}-\\d{2}-\\d{2}$"))) {
            showError("Please enter date in format YYYY-MM-DD.")
            return
        }

        binding.btnSubmit.isEnabled = false
        binding.btnCancel.isEnabled = false

        lifecycleScope.launch {
            if (isEditMode) {
                val txId = transactionId.orEmpty()
                val result = updateUseCase(
                    transactionId = txId,
                    quantity = qty,
                    price = price,
                    fee = fee,
                    date = date
                )
                if (result.isSuccess) {
                    onTransactionSaved?.invoke()
                    dismiss()
                } else {
                    binding.btnSubmit.isEnabled = true
                    binding.btnCancel.isEnabled = true
                    showError(result.exceptionOrNull()?.message ?: "Failed to update transaction.")
                }
            } else {
                val result = createUseCase(
                    symbol = symbol,
                    type = selectedType,
                    quantity = qty,
                    price = price,
                    fee = fee,
                    date = date
                )
                if (result.isSuccess) {
                    onTransactionSaved?.invoke()
                    dismiss()
                } else {
                    binding.btnSubmit.isEnabled = true
                    binding.btnCancel.isEnabled = true
                    showError(result.exceptionOrNull()?.message ?: "Failed to create transaction.")
                }
            }
        }
    }

    private fun showError(msg: String) {
        binding.tvFormError.text = msg
        binding.tvFormError.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
