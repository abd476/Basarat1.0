package com.fyp.basarattradeinvest.domain.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Domain entity for a user-defined alert rule.
 */
data class AlertRule(
    val id: String,
    val userId: String,
    val stockSymbol: String?,
    val condition: String,
    val threshold: Double,
    val isActive: Boolean,
    val createdAt: String
) {
    val displayTarget: String
        get() = stockSymbol?.uppercase(Locale.ROOT) ?: "General Market"

    val formattedCondition: String
        get() = when (condition.lowercase(Locale.ROOT)) {
            "price_above" -> "Price Rises Above"
            "price_below" -> "Price Drops Below"
            "change_percent_above" -> "Daily Gain Exceeds"
            "change_percent_below" -> "Daily Drop Exceeds"
            "volume_above" -> "Volume Exceeds"
            "rsi_above" -> "RSI Above (Overbought)"
            "rsi_below" -> "RSI Below (Oversold)"
            else -> condition.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }

    val formattedThreshold: String
        get() = when (condition.lowercase(Locale.ROOT)) {
            "price_above", "price_below" -> "PKR %.2f".format(Locale.US, threshold)
            "change_percent_above", "change_percent_below" -> "%+.2f%%".format(Locale.US, threshold)
            "rsi_above", "rsi_below" -> "%.1f".format(Locale.US, threshold)
            "volume_above" -> "%,.0f".format(Locale.US, threshold)
            else -> "%.2f".format(Locale.US, threshold)
        }

    val formattedDate: String
        get() = formatIsoDate(createdAt)
}

/**
 * Domain entity for a triggered alert / notification event.
 */
data class AlertItem(
    val id: String,
    val userId: String,
    val ruleId: String?,
    val title: String,
    val message: String?,
    val isRead: Boolean,
    val createdAt: String
) {
    val formattedTime: String
        get() = formatRelativeTime(createdAt)
}

/**
 * Supported condition metadata for UI selection
 */
enum class AlertCondition(
    val key: String,
    val label: String,
    val unitPrefix: String = "",
    val unitSuffix: String = "",
    val defaultThreshold: Double = 100.0
) {
    PRICE_ABOVE("price_above", "Price Rises Above", "PKR ", "", 100.0),
    PRICE_BELOW("price_below", "Price Drops Below", "PKR ", "", 100.0),
    CHANGE_PERCENT_ABOVE("change_percent_above", "Daily Gain Exceeds", "+", "%", 5.0),
    CHANGE_PERCENT_BELOW("change_percent_below", "Daily Drop Exceeds", "-", "%", 5.0),
    VOLUME_ABOVE("volume_above", "Volume Exceeds", "", " shares", 1000000.0),
    RSI_ABOVE("rsi_above", "RSI Above (Overbought)", "", "", 70.0),
    RSI_BELOW("rsi_below", "RSI Below (Oversold)", "", "", 30.0);

    companion object {
        fun fromKey(key: String): AlertCondition {
            return entries.firstOrNull { it.key.equals(key, ignoreCase = true) } ?: PRICE_ABOVE
        }
    }
}

private fun formatIsoDate(isoString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val cleanIso = if (isoString.contains(".")) {
            isoString.substringBefore(".")
        } else {
            isoString.removeSuffix("Z")
        }
        val date = inputFormat.parse(cleanIso) ?: return isoString
        val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        outputFormat.format(date)
    } catch (_: Exception) {
        isoString.take(10)
    }
}

private fun formatRelativeTime(isoString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val cleanIso = if (isoString.contains(".")) {
            isoString.substringBefore(".")
        } else {
            isoString.removeSuffix("Z")
        }
        val date = inputFormat.parse(cleanIso) ?: return isoString
        val diffMs = Date().time - date.time
        val minutes = diffMs / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24

        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            days < 7 -> "${days}d ago"
            else -> {
                val outputFormat = SimpleDateFormat("MMM dd", Locale.US)
                outputFormat.format(date)
            }
        }
    } catch (_: Exception) {
        isoString.take(10)
    }
}
