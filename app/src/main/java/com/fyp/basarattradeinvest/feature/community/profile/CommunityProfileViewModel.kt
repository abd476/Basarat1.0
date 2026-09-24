package com.fyp.basarattradeinvest.feature.community.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.CommunityRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.community.CommunityPost
import com.fyp.basarattradeinvest.domain.model.community.CommunityProfile
import com.fyp.basarattradeinvest.domain.repository.CommunityRepository
import com.fyp.basarattradeinvest.domain.usecase.community.GetMyCommunityProfileUseCase
import com.fyp.basarattradeinvest.domain.usecase.community.GetMyPostsUseCase
import com.fyp.basarattradeinvest.domain.usecase.community.GetUserCommunityProfileUseCase
import com.fyp.basarattradeinvest.domain.usecase.community.GetUserPostsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommunityProfileViewModel(
    private val getMyProfileUseCase: GetMyCommunityProfileUseCase,
    private val getUserProfileUseCase: GetUserCommunityProfileUseCase,
    private val getMyPostsUseCase: GetMyPostsUseCase,
    private val getUserPostsUseCase: GetUserPostsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<CommunityProfileUiState>(CommunityProfileUiState.Loading)
    val uiState: StateFlow<CommunityProfileUiState> = _uiState.asStateFlow()

    private var currentUserId: String? = null
    private var postCursor: String? = null

    fun loadProfile(userId: String? = null, isRefresh: Boolean = false) {
        currentUserId = userId
        val isSelf = userId.isNullOrBlank() || userId.equals("me", ignoreCase = true)

        viewModelScope.launch {
            if (!isRefresh && _uiState.value !is CommunityProfileUiState.Success) {
                _uiState.value = CommunityProfileUiState.Loading
            }

            val profileResult = if (isSelf) {
                getMyProfileUseCase()
            } else {
                getUserProfileUseCase(userId!!)
            }

            val postsResult = if (isSelf) {
                getMyPostsUseCase(cursor = null, limit = 20)
            } else {
                getUserPostsUseCase(userId = userId!!, cursor = null, limit = 20)
            }

            if (profileResult.isSuccess) {
                val profile = profileResult.getOrNull()!!
                val posts = postsResult.getOrNull()?.posts ?: emptyList()
                val hasMore = postsResult.getOrNull()?.hasMore ?: false
                postCursor = postsResult.getOrNull()?.cursor

                _uiState.value = CommunityProfileUiState.Success(
                    profile = profile,
                    posts = posts,
                    hasMorePosts = hasMore,
                    isRefreshing = false
                )
            } else {
                val errorMsg = profileResult.exceptionOrNull()?.message ?: "Unable to load community profile"
                _uiState.value = CommunityProfileUiState.Error(errorMsg)
            }
        }
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
                    getUserPostsUseCase = GetUserPostsUseCase(repository)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
