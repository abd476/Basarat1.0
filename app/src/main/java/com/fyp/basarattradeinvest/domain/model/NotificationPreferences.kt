package com.fyp.basarattradeinvest.domain.model

data class NotificationPreferences(
    val channels: List<String>,
    val categories: List<String>
)
