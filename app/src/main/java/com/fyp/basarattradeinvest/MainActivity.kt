package com.fyp.basarattradeinvest

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.fyp.basarattradeinvest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
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
                R.id.forgotPasswordFragment,
                R.id.resetPasswordFragment,
                R.id.changePasswordFragment,
                R.id.editProfileFragment,
                R.id.riskProfileFragment,
                R.id.notificationPreferencesFragment,
                R.id.alertsFragment,
                R.id.chartFullscreenFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }
    }
}