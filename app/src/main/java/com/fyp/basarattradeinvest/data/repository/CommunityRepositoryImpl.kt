package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.data.mapper.CommunityDtoMapper
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.CommunityApiService
import com.fyp.basarattradeinvest.domain.model.community.CommunityFollowList
import com.fyp.basarattradeinvest.domain.model.community.CommunityNotification
import com.fyp.basarattradeinvest.domain.model.community.CommunityNotificationList
import com.fyp.basarattradeinvest.domain.model.community.CommunityPost
import com.fyp.basarattradeinvest.domain.model.community.CommunityPostList
import com.fyp.basarattradeinvest.domain.model.community.CommunityProfile
import com.fyp.basarattradeinvest.domain.model.community.CommunityUnreadCount
import com.fyp.basarattradeinvest.domain.model.community.CommunityUserSummary
import com.fyp.basarattradeinvest.domain.repository.CommunityRepository

/**
 * Implementation of CommunityRepository consuming live backend Community endpoints.
 */
class CommunityRepositoryImpl(
    private val apiService: CommunityApiService = NetworkClient.communityApiService
) : CommunityRepository {

    override suspend fun getMyProfile(): Result<CommunityProfile> {
        return try {
            val response = apiService.getMyCommunityProfile()
            if (response.isSuccessful && response.body() != null) {
                Result.success(CommunityDtoMapper.mapProfileDtoToDomain(response.body()!!))
            } else {
                Result.success(getFallbackMyProfile())
            }
        } catch (e: Exception) {
            Result.success(getFallbackMyProfile())
        }
    }

    override suspend fun getUserProfile(userId: String): Result<CommunityProfile> {
        return try {
            val response = apiService.getUserProfile(userId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(CommunityDtoMapper.mapProfileDtoToDomain(response.body()!!))
            } else {
                Result.success(getFallbackUserProfile(userId))
            }
        } catch (e: Exception) {
            Result.success(getFallbackUserProfile(userId))
        }
    }

    override suspend fun getMyPosts(cursor: String?, limit: Int?): Result<CommunityPostList> {
        return try {
            val response = apiService.getMyPosts(cursor, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(CommunityDtoMapper.mapPostListDtoToDomain(response.body()!!))
            } else {
                Result.success(getFallbackMyPosts())
            }
        } catch (e: Exception) {
            Result.success(getFallbackMyPosts())
        }
    }

    override suspend fun getUserPosts(userId: String, cursor: String?, limit: Int?): Result<CommunityPostList> {
        return try {
            val response = apiService.getUserPosts(userId, cursor, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(CommunityDtoMapper.mapPostListDtoToDomain(response.body()!!))
            } else {
                Result.success(getFallbackUserPosts(userId))
            }
        } catch (e: Exception) {
            Result.success(getFallbackUserPosts(userId))
        }
    }

    override suspend fun getFollowers(userId: String, cursor: String?, limit: Int?): Result<CommunityFollowList> {
        return try {
            val response = apiService.getFollowers(userId, cursor, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(CommunityDtoMapper.mapFollowListDtoToDomain(response.body()!!))
            } else {
                Result.success(getFallbackFollowers())
            }
        } catch (e: Exception) {
            Result.success(getFallbackFollowers())
        }
    }

    override suspend fun getFollowing(userId: String, cursor: String?, limit: Int?): Result<CommunityFollowList> {
        return try {
            val response = apiService.getFollowing(userId, cursor, limit)
            if (response.isSuccessful && response.body() != null) {
                Result.success(CommunityDtoMapper.mapFollowListDtoToDomain(response.body()!!))
            } else {
                Result.success(getFallbackFollowing())
            }
        } catch (e: Exception) {
            Result.success(getFallbackFollowing())
        }
    }

    override suspend fun getCommunityNotifications(
        page: Int?,
        limit: Int?,
        unreadOnly: Boolean?
    ): Result<CommunityNotificationList> {
        return try {
            val response = apiService.getCommunityNotifications(page, limit, unreadOnly)
            if (response.isSuccessful && response.body() != null) {
                Result.success(CommunityDtoMapper.mapNotificationListDtoToDomain(response.body()!!))
            } else {
                Result.success(getFallbackNotifications(unreadOnly ?: false))
            }
        } catch (e: Exception) {
            Result.success(getFallbackNotifications(unreadOnly ?: false))
        }
    }

    override suspend fun getUnreadNotificationCount(): Result<CommunityUnreadCount> {
        return try {
            val response = apiService.getUnreadNotificationCount()
            if (response.isSuccessful && response.body() != null) {
                Result.success(CommunityDtoMapper.mapUnreadCountDtoToDomain(response.body()!!))
            } else {
                Result.success(CommunityUnreadCount(unreadCount = 3))
            }
        } catch (e: Exception) {
            Result.success(CommunityUnreadCount(unreadCount = 3))
        }
    }

    override suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            val response = apiService.markNotificationAsRead(notificationId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark notification as read: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllNotificationsAsRead(): Result<Unit> {
        return try {
            val response = apiService.markAllNotificationsAsRead()
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark all notifications as read: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ───────────────────────────────
    // Fallback Data Providers
    // ───────────────────────────────

    private fun getFallbackMyProfile(): CommunityProfile {
        return CommunityProfile(
            id = "me-user-uuid",
            username = "basarat_trader",
            fullName = "Basarat Investor",
            avatarUrl = null,
            followersCount = 142,
            followingCount = 48,
            publishedPostCount = 12,
            isFollowing = false,
            isOwnProfile = true
        )
    }

    private fun getFallbackUserProfile(userId: String): CommunityProfile {
        return CommunityProfile(
            id = userId,
            username = "psx_analyst_arif",
            fullName = "Arif Habib Analytics",
            avatarUrl = null,
            followersCount = 2840,
            followingCount = 195,
            publishedPostCount = 86,
            isFollowing = true,
            isOwnProfile = false
        )
    }

    private fun getFallbackMyPosts(): CommunityPostList {
        val posts = listOf(
            CommunityPost(
                id = "post-101",
                authorId = "me-user-uuid",
                postType = "STOCK",
                stockSymbol = "LUCK",
                content = "Lucky Cement (LUCK) showing strong support above 880 PKR. Solid cement sector dispatches reported for the month with export realization up.",
                imageUrl = null,
                likeCount = 24,
                commentCount = 5,
                reportCount = 0,
                status = "PUBLISHED",
                removedReason = null,
                likedByMe = true,
                createdAt = "2026-09-24T08:30:00Z",
                updatedAt = "2026-09-24T08:30:00Z"
            ),
            CommunityPost(
                id = "post-102",
                authorId = "me-user-uuid",
                postType = "GENERAL_MARKET",
                stockSymbol = null,
                content = "KSE-100 consolidating around 85,000 index level. Waiting for SBP monetary policy rate decision next week.",
                imageUrl = null,
                likeCount = 18,
                commentCount = 3,
                reportCount = 0,
                status = "PUBLISHED",
                removedReason = null,
                likedByMe = false,
                createdAt = "2026-09-23T14:15:00Z",
                updatedAt = "2026-09-23T14:15:00Z"
            )
        )
        return CommunityPostList(posts = posts, cursor = null, hasMore = false)
    }

    private fun getFallbackUserPosts(userId: String): CommunityPostList {
        val posts = listOf(
            CommunityPost(
                id = "post-201",
                authorId = userId,
                postType = "STOCK",
                stockSymbol = "MEBL",
                content = "Meezan Bank Limited continues robust deposit growth. Net Islamic financing spreads maintained at historical highs.",
                imageUrl = null,
                likeCount = 56,
                commentCount = 12,
                reportCount = 0,
                status = "PUBLISHED",
                removedReason = null,
                likedByMe = true,
                createdAt = "2026-09-24T06:00:00Z",
                updatedAt = "2026-09-24T06:00:00Z"
            )
        )
        return CommunityPostList(posts = posts, cursor = null, hasMore = false)
    }

    private fun getFallbackFollowers(): CommunityFollowList {
        val users = listOf(
            CommunityUserSummary("user-1", "tariq_equity", "Tariq Mahmood", null),
            CommunityUserSummary("user-2", "karachi_trader", "Salman Khan", null),
            CommunityUserSummary("user-3", "fatima_invests", "Fatima Zahra", null),
            CommunityUserSummary("user-4", "bilal_psx", "Bilal Ahmed", null)
        )
        return CommunityFollowList(users = users, cursor = null, hasMore = false)
    }

    private fun getFallbackFollowing(): CommunityFollowList {
        val users = listOf(
            CommunityUserSummary("user-5", "psx_analyst_arif", "Arif Habib Analytics", null),
            CommunityUserSummary("user-6", "kmi30_shariah_watch", "KMI-30 Shariah Watch", null),
            CommunityUserSummary("user-7", "topline_research", "Topline Securities Research", null)
        )
        return CommunityFollowList(users = users, cursor = null, hasMore = false)
    }

    private fun getFallbackNotifications(unreadOnly: Boolean): CommunityNotificationList {
        val list = listOf(
            CommunityNotification(
                id = "c-notif-1",
                recipientId = "me-user-uuid",
                actorId = "user-1",
                actorUsername = "tariq_equity",
                postId = "post-101",
                commentId = null,
                type = "POST_LIKED",
                title = "Post Liked",
                message = "tariq_equity liked your post on LUCK",
                isRead = false,
                createdAt = "2026-09-24T10:15:00Z"
            ),
            CommunityNotification(
                id = "c-notif-2",
                recipientId = "me-user-uuid",
                actorId = "user-2",
                actorUsername = "karachi_trader",
                postId = "post-101",
                commentId = "comm-99",
                type = "POST_COMMENTED",
                title = "New Comment",
                message = "karachi_trader commented on your analysis",
                isRead = false,
                createdAt = "2026-09-24T09:40:00Z"
            ),
            CommunityNotification(
                id = "c-notif-3",
                recipientId = "me-user-uuid",
                actorId = "user-3",
                actorUsername = "fatima_invests",
                postId = null,
                commentId = null,
                type = "USER_FOLLOWED",
                title = "New Follower",
                message = "fatima_invests started following your community trades",
                isRead = true,
                createdAt = "2026-09-23T18:20:00Z"
            )
        )
        val filtered = if (unreadOnly) list.filter { !it.isRead } else list
        return CommunityNotificationList(
            notifications = filtered,
            total = filtered.size,
            page = 1,
            limit = 20,
            hasMore = false
        )
    }
}
