package com.fyp.basarattradeinvest.feature.stock.analysis.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.ItemForecastHistoryBinding
import com.fyp.basarattradeinvest.domain.model.ForecastDirection
import com.fyp.basarattradeinvest.domain.model.ForecastHistoryRecord

/**
 * ListAdapter for rendering historical ML forecasts
 */
class ForecastHistoryAdapter : ListAdapter<ForecastHistoryRecord, ForecastHistoryAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemForecastHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemForecastHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ForecastHistoryRecord) {
            val context = itemView.context

            binding.tvTargetDate.text = "Target: ${item.targetDate}"
            binding.tvPredictedDate.text = "Made: ${item.predictedAt.take(10)}"

            // Predicted direction badge
            binding.tvPredictedDirectionBadge.text = item.predictedDirection.label.uppercase()
            when (item.predictedDirection) {
                ForecastDirection.BULLISH -> {
                    binding.tvPredictedDirectionBadge.setBackgroundResource(R.drawable.bg_badge_positive)
                    binding.tvPredictedDirectionBadge.setTextColor(ContextCompat.getColor(context, R.color.color_positive))
                }
                ForecastDirection.BEARISH -> {
                    binding.tvPredictedDirectionBadge.setBackgroundResource(R.drawable.bg_badge_negative)
                    binding.tvPredictedDirectionBadge.setTextColor(ContextCompat.getColor(context, R.color.color_negative))
                }
                ForecastDirection.SIDEWAYS -> {
                    binding.tvPredictedDirectionBadge.setBackgroundResource(R.drawable.bg_badge_neutral)
                    binding.tvPredictedDirectionBadge.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
                }
            }

            // Confidence display
            val confPct = (item.confidence * 100).toInt()
            binding.tvConfidence.text = "Confidence: $confPct%"

            // Actual outcome & correctness badge
            val actual = item.actualOutcome
            if (actual != null) {
                if (actual.wasCorrect) {
                    binding.tvOutcomeBadge.text = "CORRECT"
                    binding.tvOutcomeBadge.setBackgroundResource(R.drawable.bg_badge_strong_buy)
                    binding.tvOutcomeBadge.setTextColor(ContextCompat.getColor(context, R.color.color_basarat_green))
                } else {
                    binding.tvOutcomeBadge.text = "INCORRECT"
                    binding.tvOutcomeBadge.setBackgroundResource(R.drawable.bg_badge_negative)
                    binding.tvOutcomeBadge.setTextColor(ContextCompat.getColor(context, R.color.color_negative))
                }
                binding.tvActualOutcomeDetails.visibility = View.VISIBLE
                binding.tvActualOutcomeDetails.text = "Actual outcome: ${actual.direction.replaceFirstChar { it.uppercase() }}"
            } else {
                binding.tvOutcomeBadge.text = "PENDING"
                binding.tvOutcomeBadge.setBackgroundResource(R.drawable.bg_badge_neutral)
                binding.tvOutcomeBadge.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
                binding.tvActualOutcomeDetails.visibility = View.VISIBLE
                binding.tvActualOutcomeDetails.text = "Actual outcome pending realization"
            }
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ForecastHistoryRecord>() {
        override fun areItemsTheSame(oldItem: ForecastHistoryRecord, newItem: ForecastHistoryRecord): Boolean {
            return oldItem.predictedAt == newItem.predictedAt && oldItem.targetDate == newItem.targetDate
        }

        override fun areContentsTheSame(oldItem: ForecastHistoryRecord, newItem: ForecastHistoryRecord): Boolean {
            return oldItem == newItem
        }
    }
}
