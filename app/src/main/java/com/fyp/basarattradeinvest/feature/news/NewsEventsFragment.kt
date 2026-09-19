package com.fyp.basarattradeinvest.feature.news

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentNewsEventsBinding
import com.fyp.basarattradeinvest.domain.model.NewsTab
import com.fyp.basarattradeinvest.feature.news.adapter.CorporateEventAdapter
import com.fyp.basarattradeinvest.feature.news.adapter.NewsArticleAdapter
import kotlinx.coroutines.launch

class NewsEventsFragment : Fragment() {

    private var _binding: FragmentNewsEventsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: NewsEventsViewModel by viewModels()

    private val newsAdapter by lazy {
        NewsArticleAdapter { article ->
            val bundle = Bundle().apply { putString("symbol", article.symbol) }
            findNavController().navigate(R.id.stockDetailFragment, bundle)
        }
    }

    private val corporateEventAdapter by lazy {
        CorporateEventAdapter { event ->
            val bundle = Bundle().apply { putString("symbol", event.symbol) }
            findNavController().navigate(R.id.stockDetailFragment, bundle)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewsEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupListeners()
        observeUiState()
    }

    private fun setupRecyclerView() {
        binding.rvNewsEvents.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = newsAdapter
        }
    }

    private fun setupListeners() {
        // Top Back
        binding.ivBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // Help
        binding.ivHelp.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Real-time PSX intelligence, corporate announcements, and FinBERT sentiment scoring.",
                Toast.LENGTH_LONG
            ).show()
        }

        // Search Bar Toggle
        binding.ivSearch.setOnClickListener {
            if (binding.layoutSearchBar.visibility == View.VISIBLE) {
                binding.layoutSearchBar.visibility = View.GONE
                binding.etSearch.setText("")
            } else {
                binding.layoutSearchBar.visibility = View.VISIBLE
                binding.etSearch.requestFocus()
            }
        }

        binding.ivSearchClear.setOnClickListener {
            binding.etSearch.setText("")
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.onSearchQueryChanged(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Segmented Switcher [ Market News ] [ Corporate Events ]
        binding.btnTabMarketNews.setOnClickListener {
            viewModel.setTab(NewsTab.MARKET_NEWS)
        }

        binding.btnTabCorporateEvents.setOnClickListener {
            viewModel.setTab(NewsTab.CORPORATE_EVENTS)
        }

        // Filter Pills
        val filterMap = mapOf(
            binding.chipAll to "All",
            binding.chipKse100 to "KSE-100",
            binding.chipEarnings to "Earnings",
            binding.chipDividends to "Dividends",
            binding.chipBanks to "Banks",
            binding.chipCement to "Cement",
            binding.chipTech to "Technology"
        )

        filterMap.forEach { (view, categoryName) ->
            view.setOnClickListener {
                viewModel.setCategory(categoryName)
            }
        }

        // Retry
        binding.btnRetry.setOnClickListener {
            viewModel.retry()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is NewsEventsUiState.Loading -> {
                            binding.progressLoading.visibility = View.VISIBLE
                            binding.rvNewsEvents.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.layoutErrorState.visibility = View.GONE
                        }
                        is NewsEventsUiState.Success -> {
                            binding.progressLoading.visibility = View.GONE
                            binding.rvNewsEvents.visibility = View.VISIBLE
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.layoutErrorState.visibility = View.GONE

                            updateTabsUI(state.activeTab)
                            updateFilterPills(state.activeCategory)

                            when (state.activeTab) {
                                NewsTab.MARKET_NEWS -> {
                                    if (binding.rvNewsEvents.adapter != newsAdapter) {
                                        binding.rvNewsEvents.adapter = newsAdapter
                                    }
                                    newsAdapter.submitList(state.newsArticles)
                                }
                                NewsTab.CORPORATE_EVENTS -> {
                                    if (binding.rvNewsEvents.adapter != corporateEventAdapter) {
                                        binding.rvNewsEvents.adapter = corporateEventAdapter
                                    }
                                    corporateEventAdapter.submitList(state.corporateEvents)
                                }
                            }
                        }
                        is NewsEventsUiState.Empty -> {
                            binding.progressLoading.visibility = View.GONE
                            binding.rvNewsEvents.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.VISIBLE
                            binding.layoutErrorState.visibility = View.GONE
                        }
                        is NewsEventsUiState.Error -> {
                            binding.progressLoading.visibility = View.GONE
                            binding.rvNewsEvents.visibility = View.GONE
                            binding.layoutEmptyState.visibility = View.GONE
                            binding.layoutErrorState.visibility = View.VISIBLE
                            binding.tvErrorMessage.text = state.message
                        }
                    }
                }
            }
        }
    }

    private fun updateTabsUI(activeTab: NewsTab) {
        val context = requireContext()
        when (activeTab) {
            NewsTab.MARKET_NEWS -> {
                binding.btnTabMarketNews.setBackgroundResource(R.drawable.bg_segmented_tab_active)
                binding.tvTabMarketNews.setTextColor(ContextCompat.getColor(context, R.color.color_text_primary))

                binding.btnTabCorporateEvents.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
                binding.tvTabCorporateEvents.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
            }
            NewsTab.CORPORATE_EVENTS -> {
                binding.btnTabCorporateEvents.setBackgroundResource(R.drawable.bg_segmented_tab_active)
                binding.tvTabCorporateEvents.setTextColor(ContextCompat.getColor(context, R.color.color_text_primary))

                binding.btnTabMarketNews.setBackgroundColor(ContextCompat.getColor(context, R.color.transparent))
                binding.tvTabMarketNews.setTextColor(ContextCompat.getColor(context, R.color.color_text_secondary))
            }
        }
    }

    private fun updateFilterPills(selectedCategory: String) {
        val pills = listOf(
            binding.chipAll to "All",
            binding.chipKse100 to "KSE-100",
            binding.chipEarnings to "Earnings",
            binding.chipDividends to "Dividends",
            binding.chipBanks to "Banks",
            binding.chipCement to "Cement",
            binding.chipTech to "Technology"
        )

        pills.forEach { (view: TextView, categoryName: String) ->
            val isSelected = selectedCategory.equals(categoryName, ignoreCase = true)
            if (isSelected) {
                view.setBackgroundResource(R.drawable.bg_chip_filter_selected)
                view.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_positive))
            } else {
                view.setBackgroundResource(R.drawable.bg_chip_filter_unselected)
                view.setTextColor(ContextCompat.getColor(requireContext(), R.color.color_text_secondary))
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
