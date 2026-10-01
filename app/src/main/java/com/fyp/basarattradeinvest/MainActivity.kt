package com.fyp.basarattradeinvest

import android.Manifest
import android.os.Bundle
import android.os.Build
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import androidx.navigation.ui.setupWithNavController
import com.fyp.basarattradeinvest.core.notifications.DeviceRegistrationScheduler
import com.fyp.basarattradeinvest.core.notifications.NotificationBadgeState
import com.fyp.basarattradeinvest.core.notifications.NotificationEvents
import com.fyp.basarattradeinvest.core.notifications.NotificationTarget
import com.fyp.basarattradeinvest.core.session.AuthState
import com.fyp.basarattradeinvest.core.session.SessionEvent
import com.fyp.basarattradeinvest.core.session.SessionManager
import com.fyp.basarattradeinvest.databinding.ActivityMainBinding
import com.fyp.basarattradeinvest.data.repository.NotificationsRepositoryImpl
import com.fyp.basarattradeinvest.data.repository.CommunityRepositoryImpl
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var pendingLaunchTarget: NotificationTarget? = null
    private val handledReadIds = mutableSetOf<String>()
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* The app remains fully usable when notification permission is denied. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(applicationContext)
        pendingLaunchTarget = NotificationTarget.from(intent)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        observeSession()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        NotificationTarget.from(intent)?.let(::handleNotificationTarget)
    }

    override fun onResume() {
        super.onResume()
        if (SessionManager.authState.value is AuthState.Authenticated) {
            lifecycleScope.launch { NotificationBadgeState.refresh() }
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.navController

        binding.bottomNavigation.setupWithNavController(navController)

        // Show BottomNavigationView only on main app destinations, hide on Splash, Onboarding & Auth screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_home) {
                maybeRequestNotificationPermission()
            }
            pendingLaunchTarget?.let { target ->
                if (SessionManager.authState.value is AuthState.Authenticated &&
                    destination.id != R.id.splashFragment && destination.id != R.id.loginFragment
                ) {
                    pendingLaunchTarget = null
                    handleNotificationTarget(target)
                } else if (SessionManager.authState.value !is AuthState.Authenticated) {
                    pendingLaunchTarget = null
                    rememberPostLoginTarget(target)
                }
            }
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
                        if (state is AuthState.Authenticated) {
                            DeviceRegistrationScheduler.requestCurrentToken(applicationContext)
                            lifecycleScope.launch { NotificationBadgeState.refresh() }
                        }
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

                launch {
                    NotificationEvents.foregroundNotifications.collect { target ->
                        lifecycleScope.launch { NotificationBadgeState.refresh() }
                        Snackbar.make(binding.root, target.body, Snackbar.LENGTH_LONG)
                            .setAction("View") { handleNotificationTarget(target) }
                            .show()
                    }
                }
            }
        }
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED ||
            SessionManager.wasNotificationPermissionAsked()
        ) return

        SessionManager.setNotificationPermissionAsked()
        MaterialAlertDialogBuilder(this)
            .setTitle("Stay up to date")
            .setMessage("Get timely price alerts, portfolio risk updates, and community notifications.")
            .setPositiveButton("Enable") { _, _ ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun rememberPostLoginTarget(target: NotificationTarget) {
        val (destinationId, arguments) = target.destination()
        SessionManager.rememberPostLoginDestination(destinationId, arguments)
    }

    private fun handleNotificationTarget(target: NotificationTarget) {
        if (SessionManager.authState.value !is AuthState.Authenticated) {
            rememberPostLoginTarget(target)
            val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
            val navController = navHost?.navController ?: return
            if (navController.currentDestination?.id != R.id.loginFragment) {
                runCatching { navController.navigate(R.id.loginFragment) }
            }
            return
        }

        val (destinationId, arguments) = target.destination()
        val navController = (supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment)?.navController ?: return
        runCatching { navController.navigate(destinationId, arguments) }
        val notificationId = target.notificationId
        if (!notificationId.isNullOrBlank() && handledReadIds.add(notificationId)) {
            lifecycleScope.launch {
                val result = if (target.screen.equals("COMMUNITY", ignoreCase = true)) {
                    CommunityRepositoryImpl().markNotificationAsRead(notificationId)
                } else {
                    NotificationsRepositoryImpl().markNotificationAsRead(notificationId)
                }
                if (result.isSuccess) {
                    if (target.screen.equals("COMMUNITY", ignoreCase = true)) {
                        NotificationBadgeState.markCommunityRead()
                    } else {
                        NotificationBadgeState.markRead()
                    }
                } else NotificationBadgeState.refresh()
            }
        }
    }

    private fun NotificationTarget.destination(): Pair<Int, Bundle?> = when (screen?.uppercase()) {
        "STOCK" -> if (!symbol.isNullOrBlank()) {
            R.id.stockDetailFragment to Bundle().apply { putString("symbol", symbol) }
        } else R.id.notificationsFragment to null
        "RISK" -> R.id.riskAnalyticsFragment to null
        "COMMUNITY" -> R.id.communityNotificationsFragment to null
        "ALERTS" -> R.id.alertsFragment to null
        "NOTIFICATIONS" -> R.id.notificationsFragment to null
        else -> R.id.notificationsFragment to null
    }
}
