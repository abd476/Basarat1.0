package com.fyp.basarattradeinvest.core.network

/**
 * DevTokenConfig
 *
 * Dedicated development & testing token configuration.
 * Paste any active Bearer token here so you don't need to re-login every time you run or test the app.
 * AuthInterceptor will automatically fall back to this token if no active session is found in SessionManager.
 */
object DevTokenConfig {
    var DEV_ACCESS_TOKEN: String = ""
}
