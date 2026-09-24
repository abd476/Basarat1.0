package com.fyp.basarattradeinvest.feature.markets.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.ItemConstituentBinding
import com.fyp.basarattradeinvest.domain.model.IndexConstituent
import java.util.Locale

class ConstituentAdapter(
    private val onConstituentClick: (IndexConstituent) -> Unit = {}
) : ListAdapter<IndexConstituent, ConstituentAdapter.ViewHolder>(DiffCallback) {

    inner class ViewHolder(
        private val binding: ItemConstituentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: IndexConstituent, position: Int) {
            val context = binding.root.context
            val rank = position + 1
            binding.tvConstituentRank.text = rank.toString()

            if (rank == 1) {
                binding.tvConstituentRank.setBackgroundResource(R.drawable.bg_rank_first)
            } else {
                binding.tvConstituentRank.setBackgroundResource(R.drawable.bg_rank_other)
            }

            binding.tvConstituentSymbol.text = item.symbol
            val meta = if (item.weightPct > 0) {
                "${item.name} • Wt: ${String.format(Locale.US, "%.2f", item.weightPct)}%"
            } else {
                item.name
            }
            binding.tvConstituentMeta.text = meta
            binding.tvConstituentPrice.text = String.format(Locale.US, "%.2f", item.current)

            val isPositive = item.changePct >= 0
            val prefix = if (isPositive) "+" else ""
            binding.tvConstituentChange.text = String.format(Locale.US, "%s%.2f%%", prefix, item.changePct)

            val colorRes = if (isPositive) R.color.color_positive else R.color.color_negative
            binding.tvConstituentChange.setTextColor(ContextCompat.getColor(context, colorRes))

            binding.root.setOnClickListener {
                onConstituentClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemConstituentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), position)
    }

    companion object DiffCallback : DiffUtil.ItemCallback<IndexConstituent>() {
        override fun areItemsTheSame(oldItem: IndexConstituent, newItem: IndexConstituent): Boolean {
            return oldItem.symbol == newItem.symbol
        }

        override fun areContentsTheSame(oldItem: IndexConstituent, newItem: IndexConstituent): Boolean {
            return oldItem == newItem
        }
    }
}
