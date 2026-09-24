package com.fyp.basarattradeinvest.feature.alerts.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.ItemAlertRuleBinding
import com.fyp.basarattradeinvest.domain.model.AlertRule

class AlertRuleAdapter(
    private val onToggleActive: (AlertRule, Boolean) -> Unit,
    private val onEditClick: (AlertRule) -> Unit,
    private val onDeleteClick: (AlertRule) -> Unit
) : ListAdapter<AlertRule, AlertRuleAdapter.AlertRuleViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlertRuleViewHolder {
        val binding = ItemAlertRuleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AlertRuleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AlertRuleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AlertRuleViewHolder(
        private val binding: ItemAlertRuleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AlertRule) {
            val context = binding.root.context

            // Target stock symbol or General badge
            binding.tvStockBadge.text = item.displayTarget

            // Active / Inactive status label & styling
            if (item.isActive) {
                binding.tvStatusLabel.text = "Active"
                binding.tvStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.color_positive))
                binding.root.alpha = 1.0f
            } else {
                binding.tvStatusLabel.text = "Paused"
                binding.tvStatusLabel.setTextColor(ContextCompat.getColor(context, R.color.color_text_tertiary))
                binding.root.alpha = 0.65f
            }

            // Switch toggle (detach listener before setting checked, then re-attach)
            binding.switchActive.setOnCheckedChangeListener(null)
            binding.switchActive.isChecked = item.isActive
            binding.switchActive.setOnCheckedChangeListener { _, isChecked ->
                onToggleActive(item, isChecked)
            }

            // Condition & Threshold
            binding.tvConditionTitle.text = item.formattedCondition
            binding.tvThreshold.text = item.formattedThreshold

            // Created date
            binding.tvCreatedDate.text = "Created ${item.formattedDate}"

            // Buttons
            binding.btnEditRule.setOnClickListener {
                onEditClick(item)
            }
            binding.btnDeleteRule.setOnClickListener {
                onDeleteClick(item)
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<AlertRule>() {
            override fun areItemsTheSame(oldItem: AlertRule, newItem: AlertRule): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: AlertRule, newItem: AlertRule): Boolean {
                return oldItem == newItem
            }
        }
    }
}
