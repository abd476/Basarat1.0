package com.fyp.basarattradeinvest.feature.markets.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.ItemMarketStockBinding
import com.fyp.basarattradeinvest.domain.model.MarketStock
import java.util.Locale

class MarketStockAdapter(
    private val onStockClick: (MarketStock) -> Unit = {}
) : ListAdapter<MarketStock, MarketStockAdapter.StockViewHolder>(DiffCallback) {

    inner class StockViewHolder(
        private val binding: ItemMarketStockBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stock: MarketStock) {
            val context = binding.root.context

            // Rank Badge
            binding.tvStockRank.text = stock.rank.toString()
            if (stock.rank == 1) {
                binding.tvStockRank.setBackgroundResource(R.drawable.bg_rank_first)
            } else {
                binding.tvStockRank.setBackgroundResource(R.drawable.bg_rank_other)
            }

            // Symbol & Sector
            binding.tvStockSymbol.text = stock.symbol
            binding.tvStockSector.text = stock.sectorCode

            // Price & % Change
            binding.tvStockPrice.text = String.format(Locale.US, "%.2f", stock.price)

            val isPositive = stock.changePercent >= 0
            val prefix = if (isPositive) "+" else ""
            binding.tvStockChange.text = String.format(Locale.US, "%s%.2f%%", prefix, stock.changePercent)

            val colorRes = if (isPositive) R.color.color_positive else R.color.color_negative
            binding.tvStockChange.setTextColor(ContextCompat.getColor(context, colorRes))

            binding.root.setOnClickListener {
                onStockClick(stock)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val binding = ItemMarketStockBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StockViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<MarketStock>() {
        override fun areItemsTheSame(oldItem: MarketStock, newItem: MarketStock): Boolean {
            return oldItem.symbol == newItem.symbol
        }

        override fun areContentsTheSame(oldItem: MarketStock, newItem: MarketStock): Boolean {
            return oldItem == newItem
        }
    }
}
