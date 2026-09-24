package com.fyp.basarattradeinvest.data.remote.api

import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityFollowListResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityNotificationsListResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityPostListResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityProfileResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityUnreadCountResponseDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit service for Community endpoints.
 * Source of truth: live OpenAPI at /api/v1/openapi.json
 */
interface CommunityApiService {

    /**
     * Get user's followers
     */
    @GET("api/v1/community/users/{user_id}/followers")
    suspend fun getFollowers(
        @Path("user_id") userId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = 20
    ): Response<CommunityFollowListResponseDto>

    /**
     * Get users that user is following
     */
    @GET("api/v1/community/users/{user_id}/following")
    suspend fun getFollowing(
        @Path("user_id") userId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = 20
    ): Response<CommunityFollowListResponseDto>

    /**
     * Get current user's community profile
     */
    @GET("api/v1/community/me")
    suspend fun getMyCommunityProfile(): Response<CommunityProfileResponseDto>

    /**
     * Get current user's posts
     */
    @GET("api/v1/community/me/posts")
    suspend fun getMyPosts(
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = 20
    ): Response<CommunityPostListResponseDto>

    /**
     * Get another user's public community profile
     */
    @GET("api/v1/community/users/{user_id}")
    suspend fun getUserProfile(
        @Path("user_id") userId: String
    ): Response<CommunityProfileResponseDto>

    /**
     * Get another user's public posts
     */
    @GET("api/v1/community/users/{user_id}/posts")
    suspend fun getUserPosts(
        @Path("user_id") userId: String,
        @Query("cursor") cursor: String? = null,
        @Query("limit") limit: Int? = 20
    ): Response<CommunityPostListResponseDto>

    /**
     * Get community notifications
     */
    @GET("api/v1/community/notifications")
    suspend fun getCommunityNotifications(
        @Query("page") page: Int? = 1,
        @Query("limit") limit: Int? = 20,
        @Query("unread_only") unreadOnly: Boolean? = false
    ): Response<CommunityNotificationsListResponseDto>

    /**
     * Get unread notification count
     */
    @GET("api/v1/community/notifications/unread-count")
    suspend fun getUnreadNotificationCount(): Response<CommunityUnreadCountResponseDto>

    /**
     * Mark notification as read
     */
    @POST("api/v1/community/notifications/{notification_id}/read")
    suspend fun markNotificationAsRead(
        @Path("notification_id") notificationId: String
    ): Response<Unit>

    /**
     * Mark all notifications as read
     */
    @POST("api/v1/community/notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<Unit>
}
