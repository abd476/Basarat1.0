package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.community.CommunityFollowList
import com.fyp.basarattradeinvest.domain.model.community.CommunityNotificationList
import com.fyp.basarattradeinvest.domain.model.community.CommunityPostList
import com.fyp.basarattradeinvest.domain.model.community.CommunityProfile
import com.fyp.basarattradeinvest.domain.model.community.CommunityUnreadCount

/**
 * Repository interface for Community operations.
 */
interface CommunityRepository {

    suspend fun getMyProfile(): Result<CommunityProfile>

    suspend fun getUserProfile(userId: String): Result<CommunityProfile>

    suspend fun getMyPosts(cursor: String? = null, limit: Int? = 20): Result<CommunityPostList>

    suspend fun getUserPosts(userId: String, cursor: String? = null, limit: Int? = 20): Result<CommunityPostList>

    suspend fun getFollowers(userId: String, cursor: String? = null, limit: Int? = 20): Result<CommunityFollowList>

    suspend fun getFollowing(userId: String, cursor: String? = null, limit: Int? = 20): Result<CommunityFollowList>

    suspend fun getCommunityNotifications(
        page: Int? = 1,
        limit: Int? = 20,
        unreadOnly: Boolean? = false
    ): Result<CommunityNotificationList>

    suspend fun getUnreadNotificationCount(): Result<CommunityUnreadCount>

    suspend fun markNotificationAsRead(notificationId: String): Result<Unit>

    suspend fun markAllNotificationsAsRead(): Result<Unit>
}
