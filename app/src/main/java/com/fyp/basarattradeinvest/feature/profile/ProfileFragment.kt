package com.fyp.basarattradeinvest.feature.profile

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
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentProfileBinding
import com.fyp.basarattradeinvest.feature.auth.AuthUiState
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeData()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserProfile()
    }

    private fun setupClickListeners() {
        binding.cardChangePassword.setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_changePasswordFragment)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Sign Out")
            .setMessage("Are you sure you want to sign out of your BASARAT account?")
            .setPositiveButton("Sign Out") { _, _ ->
                viewModel.logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userState.collect { user ->
                        if (user != null) {
                            binding.tvUserName.text = user.fullName?.takeIf { it.isNotBlank() } ?: "BASARAT Trader"
                            binding.tvUserEmail.text = user.email
                        } else {
                            binding.tvUserName.text = "BASARAT Trader"
                            binding.tvUserEmail.text = "Active Trader"
                        }
                    }
                }

                launch {
                    viewModel.logoutUiState.collect { state ->
                        when (state) {
                            is AuthUiState.Idle -> {
                                binding.pbLogout.visibility = View.GONE
                                binding.tvLogoutLabel.visibility = View.VISIBLE
                                binding.btnLogout.isEnabled = true
                            }
                            is AuthUiState.Loading -> {
                                binding.pbLogout.visibility = View.VISIBLE
                                binding.tvLogoutLabel.visibility = View.GONE
                                binding.btnLogout.isEnabled = false
                            }
                            is AuthUiState.Success -> {
                                binding.pbLogout.visibility = View.GONE
                                binding.tvLogoutLabel.visibility = View.VISIBLE
                                binding.btnLogout.isEnabled = true
                                Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
                                
                                // Navigate to Login and clear entire back stack to prevent going back to authenticated screens
                                val navOptions = NavOptions.Builder()
                                    .setPopUpTo(R.id.nav_graph, true)
                                    .build()
                                findNavController().navigate(R.id.loginFragment, null, navOptions)
                                viewModel.resetLogoutState()
                            }
                            is AuthUiState.Error -> {
                                binding.pbLogout.visibility = View.GONE
                                binding.tvLogoutLabel.visibility = View.VISIBLE
                                binding.btnLogout.isEnabled = true
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                                viewModel.resetLogoutState()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
