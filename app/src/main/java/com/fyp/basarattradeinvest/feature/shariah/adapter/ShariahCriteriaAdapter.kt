package com.fyp.basarattradeinvest.feature.shariah.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.ItemShariahCriterionBinding
import com.fyp.basarattradeinvest.domain.model.shariah.ShariahCriterionItem
import java.util.Locale

class ShariahCriteriaAdapter : ListAdapter<ShariahCriterionItem, ShariahCriteriaAdapter.CriterionViewHolder>(CriterionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CriterionViewHolder {
        val binding = ItemShariahCriterionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CriterionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CriterionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CriterionViewHolder(
        private val binding: ItemShariahCriterionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ShariahCriterionItem) {
            val context = binding.root.context

            binding.tvCriterionName.text = item.name
            binding.tvCriterionDescription.text = item.description

            if (item.passed) {
                binding.tvCriterionStatusBadge.text = "PASSED ✓"
                binding.tvCriterionStatusBadge.setBackgroundResource(R.drawable.bg_badge_shariah_compliant)
                binding.tvCriterionStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.color_shariah_teal))
            } else {
                binding.tvCriterionStatusBadge.text = "FAILED ✗"
                binding.tvCriterionStatusBadge.setBackgroundResource(R.drawable.bg_badge_shariah_non_compliant)
                binding.tvCriterionStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.color_negative))
            }

            val measuredValStr = if (item.value != null) {
                if (item.name.contains("Core", ignoreCase = true)) {
                    if (item.passed) "Compliant" else "Non-Compliant"
                } else {
                    String.format(Locale.US, "%.1f%%", item.value)
                }
            } else {
                "N/A"
            }
            binding.tvCriterionMeasuredValue.text = measuredValStr

            val thresholdStr = if (item.name.contains("Core", ignoreCase = true)) {
                "Halal Only"
            } else if (item.threshold > 0.0) {
                String.format(Locale.US, "≤ %.1f%%", item.threshold)
            } else {
                "0.0%"
            }
            binding.tvCriterionThresholdValue.text = thresholdStr
        }
    }

    class CriterionDiffCallback : DiffUtil.ItemCallback<ShariahCriterionItem>() {
        override fun areItemsTheSame(oldItem: ShariahCriterionItem, newItem: ShariahCriterionItem): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: ShariahCriterionItem, newItem: ShariahCriterionItem): Boolean {
            return oldItem == newItem
        }
    }
}
