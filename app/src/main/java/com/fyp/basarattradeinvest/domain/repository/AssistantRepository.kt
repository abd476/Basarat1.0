package com.fyp.basarattradeinvest.domain.repository

import com.fyp.basarattradeinvest.domain.model.assistant.AssistantChatResult
import com.fyp.basarattradeinvest.domain.model.assistant.AssistantConversation
import com.fyp.basarattradeinvest.domain.model.assistant.AssistantConversationWithMessages
import kotlinx.coroutines.flow.Flow

/**
 * Repository contract for the AI Investment Assistant module.
 */
interface AssistantRepository {

    /** Send a chat message (standard REST). */
    suspend fun sendMessage(message: String, conversationId: String? = null): Result<AssistantChatResult>

    /**
     * Send a chat message with streaming (SSE).
     * Emits accumulated content as chunks arrive.
     * The final emission is the complete assistant response.
     */
    fun sendMessageStream(message: String, conversationId: String? = null): Flow<Result<String>>

    /** List all user conversations. */
    suspend fun listConversations(limit: Int? = null, offset: Int? = null): Result<List<AssistantConversation>>

    /** Create a new conversation. */
    suspend fun createConversation(title: String? = null): Result<AssistantConversation>

    /** Get a conversation with its full message history. */
    suspend fun getConversation(conversationId: String): Result<AssistantConversationWithMessages>

    /** Update a conversation's title. */
    suspend fun updateConversationTitle(conversationId: String, title: String?): Result<AssistantConversation>

    /** Delete a conversation. */
    suspend fun deleteConversation(conversationId: String): Result<Unit>

    /** Regenerate the last assistant response. */
    suspend fun regenerateResponse(conversationId: String): Result<AssistantChatResult>
}
