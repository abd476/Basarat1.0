package com.fyp.basarattradeinvest.domain.model.community

/**
 * Domain representation of a Community User Profile.
 */
data class CommunityProfile(
    val id: String,
    val username: String,
    val fullName: String?,
    val avatarUrl: String?,
    val followersCount: Int,
    val followingCount: Int,
    val publishedPostCount: Int,
    val isFollowing: Boolean,
    val isOwnProfile: Boolean
) {
    val displayName: String
        get() = if (!fullName.isNullOrBlank()) fullName else "@$username"
}

/**
 * Domain representation of a Community User in a list.
 */
data class CommunityUserSummary(
    val id: String,
    val username: String,
    val fullName: String?,
    val avatarUrl: String?
) {
    val displayName: String
        get() = if (!fullName.isNullOrBlank()) fullName else "@$username"
}

/**
 * Paginated follow list domain model.
 */
data class CommunityFollowList(
    val users: List<CommunityUserSummary>,
    val cursor: String?,
    val hasMore: Boolean
)

/**
 * Domain representation of a Community Post.
 */
data class CommunityPost(
    val id: String,
    val authorId: String,
    val postType: String,
    val stockSymbol: String?,
    val content: String,
    val imageUrl: String?,
    val likeCount: Int,
    val commentCount: Int,
    val reportCount: Int,
    val status: String,
    val removedReason: String?,
    val likedByMe: Boolean,
    val createdAt: String,
    val updatedAt: String
) {
    val isStockPost: Boolean
        get() = !stockSymbol.isNullOrBlank() || postType.equals("STOCK", ignoreCase = true)
}

/**
 * Paginated post list domain model.
 */
data class CommunityPostList(
    val posts: List<CommunityPost>,
    val cursor: String?,
    val hasMore: Boolean
)

/**
 * Domain representation of a Community Notification.
 */
data class CommunityNotification(
    val id: String,
    val recipientId: String,
    val actorId: String?,
    val actorUsername: String?,
    val postId: String?,
    val commentId: String?,
    val type: String,
    val title: String,
    val message: String,
    val isRead: Boolean,
    val createdAt: String
)

/**
 * Paginated notification list domain model.
 */
data class CommunityNotificationList(
    val notifications: List<CommunityNotification>,
    val total: Int,
    val page: Int,
    val limit: Int,
    val hasMore: Boolean
)

/**
 * Unread count domain model.
 */
data class CommunityUnreadCount(
    val unreadCount: Int
)
