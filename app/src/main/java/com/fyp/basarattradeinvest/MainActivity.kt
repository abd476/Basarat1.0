package com.fyp.basarattradeinvest

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.fyp.basarattradeinvest.core.session.AuthState
import com.fyp.basarattradeinvest.core.session.SessionEvent
import com.fyp.basarattradeinvest.core.session.SessionManager
import com.fyp.basarattradeinvest.databinding.ActivityMainBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        observeSession()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Show BottomNavigationView only on main app destinations, hide on Splash, Onboarding & Auth screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.splashFragment,
                R.id.onboardingFragment,
                R.id.loginFragment,
                R.id.signUpFragment,
                R.id.emailVerificationFragment,
                R.id.forgotPasswordFragment,
                R.id.verifyResetCodeFragment,
                R.id.resetPasswordFragment,
                R.id.changePasswordFragment,
                R.id.editProfileFragment,
                R.id.riskProfileFragment,
                R.id.notificationPreferencesFragment,
                R.id.alertsFragment,
                R.id.notificationsFragment,
                R.id.chartFullscreenFragment,
                R.id.communityFollowersFragment,
                R.id.communityNotificationsFragment,
                R.id.assistantConversationsFragment,
                R.id.assistantChatFragment,
                R.id.adminCommunityHubFragment,
                R.id.adminReportDetailFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun observeSession() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.navController

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    SessionManager.authState.collect { state ->
                        if (state != AuthState.SessionExpired) return@collect
                        val currentDestination = navController.currentDestination ?: return@collect
                        if (currentDestination.id == R.id.loginFragment ||
                            currentDestination.id == R.id.splashFragment
                        ) return@collect

                        SessionManager.rememberPostLoginDestination(
                            destinationId = currentDestination.id,
                            arguments = navController.currentBackStackEntry?.arguments
                        )
                        Toast.makeText(
                            this@MainActivity,
                            "Your session expired. Please sign in again.",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.navigate(
                            R.id.loginFragment,
                            null,
                            navOptions {
                                popUpTo(navController.graph.id) { inclusive = false }
                                launchSingleTop = true
                            }
                        )
                    }
                }

                launch {
                    SessionManager.sessionEvents.collect { event ->
                        if (event == SessionEvent.RefreshTemporarilyUnavailable) {
                            Toast.makeText(
                                this@MainActivity,
                                "Couldn't verify your session. Check your connection and try again.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }
    }
}
