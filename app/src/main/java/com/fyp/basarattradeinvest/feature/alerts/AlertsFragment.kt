package com.fyp.basarattradeinvest.feature.alerts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentAlertsBinding
import com.fyp.basarattradeinvest.domain.model.AlertRule
import com.fyp.basarattradeinvest.feature.alerts.adapter.AlertHistoryAdapter
import com.fyp.basarattradeinvest.feature.alerts.adapter.AlertRuleAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.launch

class AlertsFragment : Fragment() {

    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AlertsViewModel by viewModels()

    private lateinit var rulesAdapter: AlertRuleAdapter
    private lateinit var historyAdapter: AlertHistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerViews()
        setupListeners()
        observeData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerViews() {
        rulesAdapter = AlertRuleAdapter(
            onToggleActive = { rule, isChecked ->
                viewModel.toggleRuleActive(rule, isChecked)
            },
            onEditClick = { rule ->
                showEditRuleDialog(rule)
            },
            onDeleteClick = { rule ->
                showDeleteConfirmationDialog(rule)
            }
        )
        binding.rvAlertRules.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = rulesAdapter
        }

        historyAdapter = AlertHistoryAdapter(
            onAlertClick = { alert ->
                if (!alert.isRead) {
                    viewModel.markAlertAsRead(alert.id)
                }
            }
        )
        binding.rvAlertHistory.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = historyAdapter
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnHeaderRefresh.setOnClickListener {
            viewModel.loadData(isSwipeRefresh = false)
        }

        binding.swipeRefresh.setColorSchemeResources(R.color.color_brand_primary)
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadData(isSwipeRefresh = true)
        }

        binding.tabLayoutAlerts.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> viewModel.switchTab(AlertsTab.RULES)
                    1 -> viewModel.switchTab(AlertsTab.HISTORY)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.chipGroupHistoryFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val unreadOnly = checkedIds.contains(R.id.chip_filter_unread)
            viewModel.toggleUnreadFilter(unreadOnly)
        }

        binding.fabCreateRule.setOnClickListener {
            showCreateRuleDialog()
        }

        binding.btnEmptyCreateRule.setOnClickListener {
            showCreateRuleDialog()
        }

        binding.btnRetry.setOnClickListener {
            viewModel.loadData(isSwipeRefresh = false)
        }
    }

    private fun showCreateRuleDialog() {
        val sheet = CreateAlertRuleBottomSheet.newInstance()
        sheet.setViewModel(viewModel)
        sheet.show(childFragmentManager, CreateAlertRuleBottomSheet.TAG)
    }

    private fun showEditRuleDialog(rule: AlertRule) {
        val sheet = CreateAlertRuleBottomSheet.newInstance(editRule = rule)
        sheet.setViewModel(viewModel)
        sheet.show(childFragmentManager, CreateAlertRuleBottomSheet.TAG)
    }

    private fun showDeleteConfirmationDialog(rule: AlertRule) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Alert Rule")
            .setMessage("Are you sure you want to delete this alert rule for ${rule.displayTarget}?")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteRule(rule.id)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.swipeRefresh.isRefreshing = false

                    when (state) {
                        is AlertsUiState.Loading -> {
                            binding.layoutLoading.visibility = View.VISIBLE
                            binding.rvAlertRules.visibility = View.GONE
                            binding.rvAlertHistory.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                        }

                        is AlertsUiState.Success -> {
                            binding.layoutLoading.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE

                            // Update Tab Badges
                            val rulesTab = binding.tabLayoutAlerts.getTabAt(0)
                            val historyTab = binding.tabLayoutAlerts.getTabAt(1)

                            rulesTab?.text = if (state.rules.isNotEmpty()) {
                                "My Rules (${state.activeRulesCount})"
                            } else {
                                "My Rules"
                            }

                            historyTab?.text = if (state.unreadCount > 0) {
                                "History (${state.unreadCount})"
                            } else {
                                "History"
                            }

                            // Switch Views based on selected Tab
                            if (state.selectedTab == AlertsTab.RULES) {
                                binding.rvAlertRules.visibility = View.VISIBLE
                                binding.rvAlertHistory.visibility = View.GONE
                                binding.layoutHistoryFilters.visibility = View.GONE
                                binding.fabCreateRule.show()
                                rulesAdapter.submitList(state.rules)
                            } else {
                                binding.rvAlertRules.visibility = View.GONE
                                binding.rvAlertHistory.visibility = View.VISIBLE
                                binding.layoutHistoryFilters.visibility = View.VISIBLE
                                binding.fabCreateRule.hide()
                                historyAdapter.submitList(state.filteredAlerts)
                            }

                            // Show transient toast if action occurred
                            state.actionMessage?.let { msg ->
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                                viewModel.clearActionMessage()
                            }
                        }

                        is AlertsUiState.Empty -> {
                            binding.layoutLoading.visibility = View.GONE
                            binding.rvAlertRules.visibility = View.GONE
                            binding.rvAlertHistory.visibility = View.GONE
                            binding.layoutError.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.VISIBLE

                            if (state.tab == AlertsTab.RULES) {
                                binding.layoutHistoryFilters.visibility = View.GONE
                                binding.tvEmptyTitle.text = "No Alert Rules Yet"
                                binding.tvEmptyDescription.text = state.message
                                binding.btnEmptyCreateRule.visibility = View.VISIBLE
                                binding.fabCreateRule.show()
                            } else {
                                binding.layoutHistoryFilters.visibility = View.VISIBLE
                                binding.tvEmptyTitle.text = "No Notifications"
                                binding.tvEmptyDescription.text = state.message
                                binding.btnEmptyCreateRule.visibility = View.GONE
                                binding.fabCreateRule.hide()
                            }
                        }

                        is AlertsUiState.Error -> {
                            binding.layoutLoading.visibility = View.GONE
                            binding.rvAlertRules.visibility = View.GONE
                            binding.rvAlertHistory.visibility = View.GONE
                            binding.layoutEmpty.visibility = View.GONE
                            binding.layoutError.visibility = View.VISIBLE
                            binding.tvErrorMessage.text = state.message
                        }
                    }
                }
            }
        }
    }
}
