package com.fyp.basarattradeinvest.feature.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.fyp.basarattradeinvest.R
import com.fyp.basarattradeinvest.core.session.AuthState
import com.fyp.basarattradeinvest.core.session.RefreshTokenFailure
import com.fyp.basarattradeinvest.core.session.SessionManager
import com.fyp.basarattradeinvest.data.repository.AuthRepositoryImpl
import com.fyp.basarattradeinvest.databinding.FragmentSplashBinding
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashFragment : Fragment() {

    private var _binding: FragmentSplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        SessionManager.init(requireContext())

        viewLifecycleOwner.lifecycleScope.launch {
            // Run the token refresh concurrently with a short minimum splash delay so
            // cold start costs max(delay, refresh) instead of delay + refresh.
            val refreshJob = async {
                if (SessionManager.shouldRefreshAccessTokenSoon()) {
                    val refreshToken = SessionManager.getRefreshToken()
                    if (refreshToken.isNullOrBlank()) {
                        SessionManager.notifySessionExpired()
                    } else {
                        val refreshResult = AuthRepositoryImpl().refresh(refreshToken)
                        refreshResult.exceptionOrNull()?.let { error ->
                            if ((error as? RefreshTokenFailure)?.isRejected == true) {
                                SessionManager.notifySessionExpired()
                            } else {
                                SessionManager.notifyRefreshUnavailable()
                            }
                        }
                    }
                }
            }

            delay(600)
            refreshJob.await()

            if (isAdded) {
                when (SessionManager.authState.value) {
                    AuthState.SessionExpired -> findNavController().navigate(
                        R.id.loginFragment,
                        null,
                        navOptions { popUpTo(R.id.splashFragment) { inclusive = true } }
                    )
                    is AuthState.Authenticated -> findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                    else -> {
                        if (SessionManager.hasRefreshToken()) {
                            findNavController().navigate(R.id.action_splashFragment_to_homeFragment)
                        } else {
                            findNavController().navigate(R.id.action_splashFragment_to_onboardingFragment)
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
