package com.fyp.basarattradeinvest.feature.news.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.NewsRepositoryImpl
import com.fyp.basarattradeinvest.domain.usecase.GetNewsAndEventsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NewsArticleDetailViewModel(
    private val getNewsAndEventsUseCase: GetNewsAndEventsUseCase = GetNewsAndEventsUseCase(NewsRepositoryImpl())
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsArticleDetailUiState>(NewsArticleDetailUiState.Loading)
    val uiState: StateFlow<NewsArticleDetailUiState> = _uiState.asStateFlow()

    private var currentArticleId: String = ""

    fun loadArticle(articleId: String) {
        currentArticleId = articleId
        viewModelScope.launch {
            _uiState.value = NewsArticleDetailUiState.Loading
            val result = getNewsAndEventsUseCase.getNewsArticle(articleId)
            result.fold(
                onSuccess = { article ->
                    _uiState.value = NewsArticleDetailUiState.Success(article)
                },
                onFailure = { error ->
                    _uiState.value = NewsArticleDetailUiState.Error(
                        error.message ?: "Failed to load article detail"
                    )
                }
            )
        }
    }

    fun retry() {
        if (currentArticleId.isNotBlank()) {
            loadArticle(currentArticleId)
        }
    }
}
