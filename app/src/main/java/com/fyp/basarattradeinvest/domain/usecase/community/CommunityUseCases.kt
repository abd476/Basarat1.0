package com.fyp.basarattradeinvest.domain.usecase.community

import com.fyp.basarattradeinvest.domain.model.community.CommunityFollowList
import com.fyp.basarattradeinvest.domain.model.community.CommunityNotificationList
import com.fyp.basarattradeinvest.domain.model.community.CommunityPostList
import com.fyp.basarattradeinvest.domain.model.community.CommunityProfile
import com.fyp.basarattradeinvest.domain.model.community.CommunityUnreadCount
import com.fyp.basarattradeinvest.domain.repository.CommunityRepository

class GetMyCommunityProfileUseCase(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(): Result<CommunityProfile> = repository.getMyProfile()
}

class GetUserCommunityProfileUseCase(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(userId: String): Result<CommunityProfile> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return repository.getUserProfile(userId)
    }
}

class GetMyPostsUseCase(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(cursor: String? = null, limit: Int? = 20): Result<CommunityPostList> =
        repository.getMyPosts(cursor, limit)
}

class GetUserPostsUseCase(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(userId: String, cursor: String? = null, limit: Int? = 20): Result<CommunityPostList> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return repository.getUserPosts(userId, cursor, limit)
    }
}

class GetFollowersUseCase(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(userId: String, cursor: String? = null, limit: Int? = 20): Result<CommunityFollowList> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return repository.getFollowers(userId, cursor, limit)
    }
}

class GetFollowingUseCase(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(userId: String, cursor: String? = null, limit: Int? = 20): Result<CommunityFollowList> {
        require(userId.isNotBlank()) { "User ID must not be blank" }
        return repository.getFollowing(userId, cursor, limit)
    }
}

class GetCommunityNotificationsUseCase(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(
        page: Int? = 1,
        limit: Int? = 20,
        unreadOnly: Boolean? = false
    ): Result<CommunityNotificationList> =
        repository.getCommunityNotifications(page, limit, unreadOnly)
}

class GetUnreadCommunityNotificationCountUseCase(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(): Result<CommunityUnreadCount> =
        repository.getUnreadNotificationCount()
}

class MarkCommunityNotificationReadUseCase(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(notificationId: String): Result<Unit> {
        require(notificationId.isNotBlank()) { "Notification ID must not be blank" }
        return repository.markNotificationAsRead(notificationId)
    }
}

class MarkAllCommunityNotificationsReadUseCase(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(): Result<Unit> =
        repository.markAllNotificationsAsRead()
}
