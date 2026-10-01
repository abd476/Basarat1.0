package com.fyp.basarattradeinvest.core.notifications

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationEvents {
    private val _foregroundNotifications = MutableSharedFlow<NotificationTarget>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val foregroundNotifications = _foregroundNotifications.asSharedFlow()

    fun publish(target: NotificationTarget) {
        _foregroundNotifications.tryEmit(target)
    }
}

object NotificationBadgeState {
    private val _unreadCount = MutableStateFlow(0)
    val unreadCount = _unreadCount.asStateFlow()
    private var alertUnreadCount = 0
    private var communityUnreadCount = 0

    suspend fun refresh() {
        val alerts = com.fyp.basarattradeinvest.data.repository.NotificationsRepositoryImpl()
            .getNotifications(page = 1, limit = 1, unreadOnly = true)
        val community = com.fyp.basarattradeinvest.data.repository.CommunityRepositoryImpl()
            .getUnreadNotificationCount()
        alerts.getOrNull()?.let { alertUnreadCount = it.total.coerceAtLeast(0) }
        community.getOrNull()?.let { communityUnreadCount = it.unreadCount.coerceAtLeast(0) }
        _unreadCount.value = alertUnreadCount + communityUnreadCount
    }

    fun markRead() {
        alertUnreadCount = (alertUnreadCount - 1).coerceAtLeast(0)
        _unreadCount.value = alertUnreadCount + communityUnreadCount
    }

    fun markCommunityRead() {
        communityUnreadCount = (communityUnreadCount - 1).coerceAtLeast(0)
        _unreadCount.value = alertUnreadCount + communityUnreadCount
    }

    fun reset() {
        alertUnreadCount = 0
        communityUnreadCount = 0
        _unreadCount.value = 0
    }
}
