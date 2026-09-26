package com.fyp.basarattradeinvest.feature.community.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.CommunityRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.community.CommunityPost
import com.fyp.basarattradeinvest.domain.model.community.CommunityProfile
import com.fyp.basarattradeinvest.domain.repository.CommunityRepository
import com.fyp.basarattradeinvest.domain.usecase.community.GetCommunityFeedUseCase
import com.fyp.basarattradeinvest.domain.usecase.community.GetMyCommunityProfileUseCase
import com.fyp.basarattradeinvest.domain.usecase.community.GetMyPostsUseCase
import com.fyp.basarattradeinvest.domain.usecase.community.GetUserCommunityProfileUseCase
import com.fyp.basarattradeinvest.domain.usecase.community.GetUserPostsUseCase
import com.fyp.basarattradeinvest.domain.usecase.community.LikeCommunityPostUseCase
import com.fyp.basarattradeinvest.domain.usecase.community.UnlikeCommunityPostUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommunityProfileViewModel(
    private val getMyProfileUseCase: GetMyCommunityProfileUseCase,
    private val getUserProfileUseCase: GetUserCommunityProfileUseCase,
    private val getMyPostsUseCase: GetMyPostsUseCase,
    private val getUserPostsUseCase: GetUserPostsUseCase,
    private val getCommunityFeedUseCase: GetCommunityFeedUseCase,
    private val likePostUseCase: LikeCommunityPostUseCase,
    private val unlikePostUseCase: UnlikeCommunityPostUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommunityProfileUiState>(CommunityProfileUiState.Loading)
    val uiState: StateFlow<CommunityProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var currentFeedMode: CommunityFeedMode = CommunityFeedMode.COMMUNITY_FEED
    private var postCursor: String? = null
    private var lastSuccessfulFetchAt: Long = 0L
    private val freshnessWindowMs = 5 * 60 * 1000L

    fun ensureLoaded(userId: String? = null, isRefresh: Boolean = false) {
        val state = _uiState.value
        val isSameProfile = currentUserId == userId && state is CommunityProfileUiState.Success
        val shouldLoad = isRefresh || !isSameProfile || System.currentTimeMillis() - lastSuccessfulFetchAt > freshnessWindowMs
        if (shouldLoad) {
            loadProfile(userId = userId, isRefresh = isRefresh)
        }
    }

    fun loadProfile(userId: String? = null, isRefresh: Boolean = false) {
        currentUserId = userId
        val isSelf = userId.isNullOrBlank() || userId.equals("me", ignoreCase = true)

        viewModelScope.launch {
            val currentState = _uiState.value
            val isSameProfile = currentUserId == userId && currentState is CommunityProfileUiState.Success
            if (!isRefresh && isSameProfile && System.currentTimeMillis() - lastSuccessfulFetchAt <= freshnessWindowMs) {
                return@launch
            }
            if (!isRefresh && currentState !is CommunityProfileUiState.Success) {
                _uiState.value = CommunityProfileUiState.Loading
            }

            val profileResult = if (isSelf) {
                getMyProfileUseCase()
            } else {
                getUserProfileUseCase(userId!!)
            }

            val postsResult = if (isSelf) {
                if (currentFeedMode == CommunityFeedMode.COMMUNITY_FEED) {
                    getCommunityFeedUseCase(mine = false, cursor = null, limit = 20)
                } else {
                    getMyPostsUseCase(cursor = null, limit = 20)
                }
            } else {
                getUserPostsUseCase(userId = userId!!, cursor = null, limit = 20)
            }

            if (profileResult.isSuccess) {
                val profile = profileResult.getOrNull()!!
                val rawPosts = postsResult.getOrNull()?.posts ?: emptyList()
                val posts = rawPosts.distinctBy { it.id }.distinctBy { "${it.authorDisplayName}_${it.content.trim()}" }
                val hasMore = postsResult.getOrNull()?.hasMore ?: false
                postCursor = postsResult.getOrNull()?.cursor

                lastSuccessfulFetchAt = System.currentTimeMillis()
                _uiState.value = CommunityProfileUiState.Success(
                    profile = profile,
                    posts = posts,
                    hasMorePosts = hasMore,
                    isRefreshing = false,
                    feedMode = currentFeedMode
                )
            } else {
                val errorMsg = profileResult.exceptionOrNull()?.message ?: "Unable to load community profile"
                _uiState.value = CommunityProfileUiState.Error(errorMsg)
            }
        }
    }

    fun setFeedMode(mode: CommunityFeedMode) {
        if (currentFeedMode == mode && _uiState.value is CommunityProfileUiState.Success) {
            return
        }
        currentFeedMode = mode
        loadProfile(userId = currentUserId, isRefresh = true)
    }

    fun toggleLike(post: CommunityPost) {
        val currentState = _uiState.value as? CommunityProfileUiState.Success ?: return

        val newLiked = !post.likedByMe
        val newLikeCount = if (newLiked) post.likeCount + 1 else maxOf(0, post.likeCount - 1)

        val updatedPosts = currentState.posts.map { item ->
            if (item.id == post.id) {
                item.copy(likedByMe = newLiked, likeCount = newLikeCount)
            } else {
                item
            }
        }

        // Optimistic UI update
        _uiState.value = currentState.copy(posts = updatedPosts)

        viewModelScope.launch {
            if (newLiked) {
                likePostUseCase(post.id)
            } else {
                unlikePostUseCase(post.id)
            }
        }
    }

    fun updateCommentCount(postId: String, newCount: Int) {
        val currentState = _uiState.value as? CommunityProfileUiState.Success ?: return

        val updatedPosts = currentState.posts.map { item ->
            if (item.id == postId) {
                item.copy(commentCount = newCount)
            } else {
                item
            }
        }

        _uiState.value = currentState.copy(posts = updatedPosts)
    }

    class Factory(
        private val repository: CommunityRepository = CommunityRepositoryImpl()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(CommunityProfileViewModel::class.java)) {
                return CommunityProfileViewModel(
                    getMyProfileUseCase = GetMyCommunityProfileUseCase(repository),
                    getUserProfileUseCase = GetUserCommunityProfileUseCase(repository),
                    getMyPostsUseCase = GetMyPostsUseCase(repository),
                    getUserPostsUseCase = GetUserPostsUseCase(repository),
                    getCommunityFeedUseCase = GetCommunityFeedUseCase(repository),
                    likePostUseCase = LikeCommunityPostUseCase(repository),
                    unlikePostUseCase = UnlikeCommunityPostUseCase(repository)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
