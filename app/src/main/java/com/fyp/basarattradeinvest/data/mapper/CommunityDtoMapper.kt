package com.fyp.basarattradeinvest.data.mapper

import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityFollowListResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityNotificationResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityNotificationsListResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityPostListResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityPostResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityProfileResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityUnreadCountResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.community.CommunityUserSummaryDto
import com.fyp.basarattradeinvest.domain.model.community.CommunityFollowList
import com.fyp.basarattradeinvest.domain.model.community.CommunityNotification
import com.fyp.basarattradeinvest.domain.model.community.CommunityNotificationList
import com.fyp.basarattradeinvest.domain.model.community.CommunityPost
import com.fyp.basarattradeinvest.domain.model.community.CommunityPostList
import com.fyp.basarattradeinvest.domain.model.community.CommunityProfile
import com.fyp.basarattradeinvest.domain.model.community.CommunityUnreadCount
import com.fyp.basarattradeinvest.domain.model.community.CommunityUserSummary

/**
 * Mapper for converting Community DTOs to Domain Models.
 */
object CommunityDtoMapper {

    fun mapProfileDtoToDomain(dto: CommunityProfileResponseDto): CommunityProfile {
        return CommunityProfile(
            id = dto.id,
            username = dto.username,
            fullName = dto.fullName,
            avatarUrl = dto.avatarUrl,
            followersCount = dto.followersCount,
            followingCount = dto.followingCount,
            publishedPostCount = dto.publishedPostCount,
            isFollowing = dto.isFollowing,
            isOwnProfile = dto.isOwnProfile
        )
    }

    fun mapUserSummaryDtoToDomain(dto: CommunityUserSummaryDto): CommunityUserSummary {
        return CommunityUserSummary(
            id = dto.id,
            username = dto.username,
            fullName = dto.fullName,
            avatarUrl = dto.avatarUrl
        )
    }

    fun mapFollowListDtoToDomain(dto: CommunityFollowListResponseDto): CommunityFollowList {
        return CommunityFollowList(
            users = dto.users.map { mapUserSummaryDtoToDomain(it) },
            cursor = dto.cursor,
            hasMore = dto.hasMore
        )
    }

    fun mapPostDtoToDomain(dto: CommunityPostResponseDto): CommunityPost {
        return CommunityPost(
            id = dto.id,
            authorId = dto.authorId,
            postType = dto.postType,
            stockSymbol = dto.stockSymbol,
            content = dto.content,
            imageUrl = dto.imageUrl,
            likeCount = dto.likeCount,
            commentCount = dto.commentCount,
            reportCount = dto.reportCount,
            status = dto.status,
            removedReason = dto.removedReason,
            likedByMe = dto.likedByMe,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt
        )
    }

    fun mapPostListDtoToDomain(dto: CommunityPostListResponseDto): CommunityPostList {
        return CommunityPostList(
            posts = dto.posts.map { mapPostDtoToDomain(it) },
            cursor = dto.cursor,
            hasMore = dto.hasMore
        )
    }

    fun mapNotificationDtoToDomain(dto: CommunityNotificationResponseDto): CommunityNotification {
        return CommunityNotification(
            id = dto.id,
            recipientId = dto.recipientId,
            actorId = dto.actorId,
            actorUsername = dto.actorUsername,
            postId = dto.postId,
            commentId = dto.commentId,
            type = dto.type,
            title = dto.title,
            message = dto.message,
            isRead = dto.isRead,
            createdAt = dto.createdAt
        )
    }

    fun mapNotificationListDtoToDomain(dto: CommunityNotificationsListResponseDto): CommunityNotificationList {
        return CommunityNotificationList(
            notifications = dto.notifications.map { mapNotificationDtoToDomain(it) },
            total = dto.total,
            page = dto.page,
            limit = dto.limit,
            hasMore = dto.hasMore
        )
    }

    fun mapUnreadCountDtoToDomain(dto: CommunityUnreadCountResponseDto): CommunityUnreadCount {
        return CommunityUnreadCount(
            unreadCount = dto.totalUnread
        )
    }
}
