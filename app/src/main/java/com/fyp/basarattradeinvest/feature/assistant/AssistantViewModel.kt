package com.fyp.basarattradeinvest.feature.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fyp.basarattradeinvest.data.repository.AssistantRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.assistant.AssistantConversation
import com.fyp.basarattradeinvest.domain.model.assistant.AssistantMessage
import com.fyp.basarattradeinvest.domain.model.assistant.MessageRole
import com.fyp.basarattradeinvest.domain.repository.AssistantRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel for the AI Investment Assistant module.
 * Manages conversation list, active chat, streaming, and conversation CRUD.
 */
class AssistantViewModel(
    private val repository: AssistantRepository
) : ViewModel() {

    // ─── Conversation List State ───
    private val _conversationListState = MutableStateFlow<ConversationListUiState>(ConversationListUiState.Loading)
    val conversationListState: StateFlow<ConversationListUiState> = _conversationListState.asStateFlow()

    // ─── Chat State ───
    private val _chatState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val chatState: StateFlow<ChatUiState> = _chatState.asStateFlow()

    // Track active streaming job for cancellation
    private var streamingJob: Job? = null

    // Current conversation context
    private var currentConversationId: String? = null

    // ─── Conversation List ───

    fun loadConversations() {
        viewModelScope.launch {
            _conversationListState.value = ConversationListUiState.Loading
            repository.listConversations(limit = 50).fold(
                onSuccess = { conversations ->
                    if (conversations.isEmpty()) {
                        _conversationListState.value = ConversationListUiState.Empty
                    } else {
                        _conversationListState.value = ConversationListUiState.Success(conversations)
                    }
                },
                onFailure = { error ->
                    _conversationListState.value = ConversationListUiState.Error(
                        error.message ?: "Unable to load conversations"
                    )
                }
            )
        }
    }

    fun createNewConversation(title: String? = null) {
        viewModelScope.launch {
            repository.createConversation(title).fold(
                onSuccess = { conversation ->
                    currentConversationId = conversation.id
                    _chatState.value = ChatUiState.Active(
                        conversation = conversation,
                        messages = emptyList()
                    )
                    // Refresh list in background
                    loadConversations()
                },
                onFailure = { error ->
                    _chatState.value = ChatUiState.Error(
                        error.message ?: "Unable to create conversation"
                    )
                }
            )
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId).fold(
                onSuccess = {
                    // Remove from list locally
                    val current = _conversationListState.value
                    if (current is ConversationListUiState.Success) {
                        val updated = current.conversations.filter { it.id != conversationId }
                        _conversationListState.value = if (updated.isEmpty()) {
                            ConversationListUiState.Empty
                        } else {
                            current.copy(conversations = updated)
                        }
                    }
                    // If this was the active conversation, clear chat
                    if (currentConversationId == conversationId) {
                        currentConversationId = null
                        _chatState.value = ChatUiState.Loading
                    }
                },
                onFailure = { /* Ignore delete failure silently, list keeps the item */ }
            )
        }
    }

    fun updateConversationTitle(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateConversationTitle(conversationId, newTitle).fold(
                onSuccess = { updatedConversation ->
                    // Update in list
                    val current = _conversationListState.value
                    if (current is ConversationListUiState.Success) {
                        val updated = current.conversations.map {
                            if (it.id == conversationId) updatedConversation else it
                        }
                        _conversationListState.value = current.copy(conversations = updated)
                    }
                    // Update in active chat
                    val chatCurrent = _chatState.value
                    if (chatCurrent is ChatUiState.Active && chatCurrent.conversation?.id == conversationId) {
                        _chatState.value = chatCurrent.copy(conversation = updatedConversation)
                    }
                },
                onFailure = { /* Title update failed, UI stays as-is */ }
            )
        }
    }

    // ─── Chat ───

    fun loadConversation(conversationId: String) {
        currentConversationId = conversationId
        viewModelScope.launch {
            _chatState.value = ChatUiState.Loading
            repository.getConversation(conversationId).fold(
                onSuccess = { conversationWithMessages ->
                    _chatState.value = ChatUiState.Active(
                        conversation = conversationWithMessages.conversation,
                        messages = conversationWithMessages.messages.map { it.toChatMessageItem() }
                    )
                },
                onFailure = { error ->
                    _chatState.value = ChatUiState.Error(
                        error.message ?: "Unable to load conversation"
                    )
                }
            )
        }
    }

    /**
     * Opens a fresh chat without an existing conversation.
     * The conversation will be created server-side when the first message is sent.
     */
    fun startNewChat() {
        currentConversationId = null
        _chatState.value = ChatUiState.Active(
            conversation = null,
            messages = emptyList()
        )
    }

    /**
     * Send a message using the streaming endpoint.
     * Falls back to standard REST if streaming fails.
     */
    fun sendMessage(userMessage: String) {
        val trimmed = userMessage.trim()
        if (trimmed.isEmpty()) return

        val currentState = _chatState.value
        if (currentState !is ChatUiState.Active) return
        if (currentState.isSending) return // Prevent double-send

        // Add user message optimistically
        val userMsgItem = ChatMessageItem(
            id = "temp-user-${System.currentTimeMillis()}",
            isUser = true,
            content = trimmed,
            timestamp = getCurrentTimestamp()
        )
        val updatedMessages = currentState.messages + userMsgItem

        _chatState.value = currentState.copy(
            messages = updatedMessages,
            isSending = true,
            streamingContent = "",
            errorMessage = null
        )

        // Use streaming endpoint
        streamingJob = viewModelScope.launch {
            var finalContent = ""
            var hasReceivedContent = false

            repository.sendMessageStream(trimmed, currentConversationId)
                .catch { e ->
                    // Stream failed — fallback to standard REST
                    sendMessageRest(trimmed, updatedMessages)
                }
                .collect { result ->
                    result.fold(
                        onSuccess = { accumulatedContent ->
                            hasReceivedContent = true
                            finalContent = accumulatedContent
                            val state = _chatState.value
                            if (state is ChatUiState.Active) {
                                _chatState.value = state.copy(
                                    streamingContent = accumulatedContent
                                )
                            }
                        },
                        onFailure = { error ->
                            if (!hasReceivedContent) {
                                // No content received — fallback to REST
                                sendMessageRest(trimmed, updatedMessages)
                                return@collect
                            }
                            // Error during streaming after partial content
                            val state = _chatState.value
                            if (state is ChatUiState.Active) {
                                _chatState.value = state.copy(
                                    isSending = false,
                                    streamingContent = null,
                                    errorMessage = error.message
                                )
                            }
                        }
                    )
                }

            // Streaming complete — add final assistant message
            if (hasReceivedContent && finalContent.isNotEmpty()) {
                finalizeStreamedMessage(finalContent)
            }
        }
    }

    /**
     * Fallback: send message via standard REST (non-streaming).
     */
    private suspend fun sendMessageRest(userMessage: String, currentMessages: List<ChatMessageItem>) {
        repository.sendMessage(userMessage, currentConversationId).fold(
            onSuccess = { chatResult ->
                // Update conversation ID if this was first message
                if (currentConversationId == null) {
                    currentConversationId = chatResult.conversationId
                }

                val assistantMsgItem = ChatMessageItem(
                    id = "rest-${System.currentTimeMillis()}",
                    isUser = false,
                    content = chatResult.message,
                    timestamp = getCurrentTimestamp()
                )
                val state = _chatState.value
                if (state is ChatUiState.Active) {
                    _chatState.value = state.copy(
                        messages = state.messages + assistantMsgItem,
                        isSending = false,
                        streamingContent = null,
                        errorMessage = null
                    )
                }
            },
            onFailure = { error ->
                val state = _chatState.value
                if (state is ChatUiState.Active) {
                    _chatState.value = state.copy(
                        isSending = false,
                        streamingContent = null,
                        errorMessage = error.message ?: "Failed to send message"
                    )
                }
            }
        )
    }

    private fun finalizeStreamedMessage(content: String) {
        val state = _chatState.value
        if (state is ChatUiState.Active) {
            val assistantMsgItem = ChatMessageItem(
                id = "stream-${System.currentTimeMillis()}",
                isUser = false,
                content = content,
                timestamp = getCurrentTimestamp()
            )
            _chatState.value = state.copy(
                messages = state.messages + assistantMsgItem,
                isSending = false,
                streamingContent = null,
                errorMessage = null
            )
        }
    }

    fun regenerateLastResponse() {
        val convId = currentConversationId ?: return
        val currentState = _chatState.value
        if (currentState !is ChatUiState.Active) return
        if (currentState.isRegenerating) return

        _chatState.value = currentState.copy(isRegenerating = true, errorMessage = null)

        viewModelScope.launch {
            repository.regenerateResponse(convId).fold(
                onSuccess = { chatResult ->
                    val state = _chatState.value
                    if (state is ChatUiState.Active) {
                        // Replace last assistant message if it exists
                        val messages = state.messages.toMutableList()
                        val lastAssistantIdx = messages.indexOfLast { !it.isUser }
                        if (lastAssistantIdx >= 0) {
                            messages[lastAssistantIdx] = messages[lastAssistantIdx].copy(
                                content = chatResult.message,
                                timestamp = getCurrentTimestamp()
                            )
                        } else {
                            // No existing assistant message — add new one
                            messages.add(
                                ChatMessageItem(
                                    id = "regen-${System.currentTimeMillis()}",
                                    isUser = false,
                                    content = chatResult.message,
                                    timestamp = getCurrentTimestamp()
                                )
                            )
                        }
                        _chatState.value = state.copy(
                            messages = messages,
                            isRegenerating = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    val state = _chatState.value
                    if (state is ChatUiState.Active) {
                        _chatState.value = state.copy(
                            isRegenerating = false,
                            errorMessage = error.message ?: "Failed to regenerate response"
                        )
                    }
                }
            )
        }
    }

    fun dismissError() {
        val state = _chatState.value
        if (state is ChatUiState.Active) {
            _chatState.value = state.copy(errorMessage = null)
        }
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
    }

    // ─── Helpers ───

    private fun AssistantMessage.toChatMessageItem(): ChatMessageItem {
        return ChatMessageItem(
            id = id,
            isUser = role == MessageRole.USER,
            content = content,
            timestamp = createdAt
        )
    }

    private fun getCurrentTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        return sdf.format(java.util.Date())
    }

    // ─── Factory ───

    class Factory(
        private val repository: AssistantRepository = AssistantRepositoryImpl()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(AssistantViewModel::class.java)) {
                return AssistantViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
