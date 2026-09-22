package com.fyp.basarattradeinvest.feature.auth

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LoginViewModel by viewModels()
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupPasswordVisibilityToggle()
        setupBottomLink()
        setupClickListeners()
        observeUiState()
    }

    private fun setupPasswordVisibilityToggle() {
        binding.ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                binding.etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye_off)
            } else {
                binding.etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
                binding.ivTogglePassword.setImageResource(R.drawable.ic_eye)
            }
            binding.etPassword.setSelection(binding.etPassword.text.length)
        }
    }

    private fun setupBottomLink() {
        val fullText = "${getString(R.string.dont_have_account)} ${getString(R.string.sign_up)}"
        val spannable = SpannableString(fullText)

        val signUpStart = fullText.indexOf(getString(R.string.sign_up))
        if (signUpStart != -1) {
            val signUpEnd = signUpStart + getString(R.string.sign_up).length
            val blackColor = ContextCompat.getColor(requireContext(), R.color.color_text_primary)
            spannable.setSpan(ForegroundColorSpan(blackColor), signUpStart, signUpEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spannable.setSpan(StyleSpan(Typeface.BOLD), signUpStart, signUpEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        binding.tvSignUpLink.text = spannable
        binding.tvSignUpLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_signUpFragment)
        }
    }

    private fun setupClickListeners() {
        binding.btnSignIn.setOnClickListener {
            val email = binding.etEmail.text?.toString().orEmpty().trim()
            val password = binding.etPassword.text?.toString().orEmpty()
            viewModel.signIn(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            Toast.makeText(requireContext(), "Continue with Google", Toast.LENGTH_SHORT).show()
        }

        binding.btnAppleSignIn.setOnClickListener {
            Toast.makeText(requireContext(), "Continue with Apple", Toast.LENGTH_SHORT).show()
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AuthUiState.Idle -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.tvSignInLabel.visibility = View.VISIBLE
                            binding.btnSignIn.isEnabled = true
                        }
                        is AuthUiState.Loading -> {
                            binding.pbLoading.visibility = View.VISIBLE
                            binding.tvSignInLabel.visibility = View.GONE
                            binding.btnSignIn.isEnabled = false
                        }
                        is AuthUiState.Success -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.tvSignInLabel.visibility = View.VISIBLE
                            binding.btnSignIn.isEnabled = true
                            findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                            viewModel.resetState()
                        }
                        is AuthUiState.Error -> {
                            binding.pbLoading.visibility = View.GONE
                            binding.tvSignInLabel.visibility = View.VISIBLE
                            binding.btnSignIn.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
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
