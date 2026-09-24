package com.fyp.basarattradeinvest.assistant

import com.fyp.basarattradeinvest.data.mapper.AssistantDtoMapper
import com.fyp.basarattradeinvest.data.remote.api.AssistantApiService
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantChatRequestDto
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantChatResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantConversationCreateDto
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantConversationListResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantConversationMessagesResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantConversationResponseDto
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantConversationUpdateDto
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantMessageResponseDto
import com.fyp.basarattradeinvest.data.repository.AssistantRepositoryImpl
import com.fyp.basarattradeinvest.domain.model.assistant.MessageRole
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response

class AssistantModuleUnitTest {

    private class FakeAssistantApiService : AssistantApiService {
        var shouldFail: Boolean = false
        val storedConversations = mutableListOf<AssistantConversationResponseDto>()
        val storedMessages = mutableMapOf<String, MutableList<AssistantMessageResponseDto>>()

        override suspend fun chat(request: AssistantChatRequestDto): Response<AssistantChatResponseDto> {
            if (shouldFail) {
                return Response.error(500, "{\"detail\":\"Internal server error\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val convId = request.conversationId ?: "conv-new-1"
            val chatResponse = AssistantChatResponseDto(
                message = "Analysis for ${request.message}: Market conditions are currently bullish.",
                conversationId = convId
            )
            return Response.success(chatResponse)
        }

        override suspend fun chatStream(request: AssistantChatRequestDto): Response<ResponseBody> {
            if (shouldFail) {
                return Response.error(500, "{\"detail\":\"Stream error\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val sseData = "data: {\"chunk\":\"Analysis: \"}\n\ndata: {\"chunk\":\"Market is bullish.\"}\n\ndata: [DONE]\n\n"
            val body = sseData.toResponseBody("text/event-stream".toMediaTypeOrNull())
            return Response.success(body)
        }

        override suspend fun listConversations(limit: Int?, offset: Int?): Response<AssistantConversationListResponseDto> {
            if (shouldFail) {
                return Response.error(500, "{\"detail\":\"Server error\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            return Response.success(
                AssistantConversationListResponseDto(
                    conversations = storedConversations.toList()
                )
            )
        }

        override suspend fun createConversation(request: AssistantConversationCreateDto): Response<AssistantConversationResponseDto> {
            if (shouldFail) {
                return Response.error(400, "{\"detail\":\"Failed to create conversation\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val newConv = AssistantConversationResponseDto(
                id = "conv-${storedConversations.size + 1}",
                userId = "user-123",
                title = request.title ?: "New Chat",
                createdAt = "2026-09-24T10:00:00Z",
                updatedAt = "2026-09-24T10:00:00Z"
            )
            storedConversations.add(newConv)
            return Response.success(newConv)
        }

        override suspend fun getConversation(conversationId: String): Response<AssistantConversationMessagesResponseDto> {
            if (shouldFail) {
                return Response.error(404, "{\"detail\":\"Conversation not found\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val conv = storedConversations.find { it.id == conversationId }
                ?: AssistantConversationResponseDto(
                    id = conversationId,
                    userId = "user-123",
                    title = "Chat $conversationId",
                    createdAt = "2026-09-24T10:00:00Z",
                    updatedAt = "2026-09-24T10:00:00Z"
                )
            val msgs = storedMessages[conversationId] ?: emptyList()
            return Response.success(
                AssistantConversationMessagesResponseDto(
                    conversation = conv,
                    messages = msgs
                )
            )
        }

        override suspend fun updateConversation(
            conversationId: String,
            request: AssistantConversationUpdateDto
        ): Response<AssistantConversationResponseDto> {
            if (shouldFail) {
                return Response.error(400, "{\"detail\":\"Update failed\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            val index = storedConversations.indexOfFirst { it.id == conversationId }
            val updated = AssistantConversationResponseDto(
                id = conversationId,
                userId = "user-123",
                title = request.title,
                createdAt = "2026-09-24T10:00:00Z",
                updatedAt = "2026-09-24T10:05:00Z"
            )
            if (index >= 0) {
                storedConversations[index] = updated
            } else {
                storedConversations.add(updated)
            }
            return Response.success(updated)
        }

        override suspend fun deleteConversation(conversationId: String): Response<Unit> {
            if (shouldFail) {
                return Response.error(404, "{\"detail\":\"Not found\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            storedConversations.removeAll { it.id == conversationId }
            storedMessages.remove(conversationId)
            return Response.success(Unit)
        }

        override suspend fun regenerateResponse(conversationId: String): Response<AssistantChatResponseDto> {
            if (shouldFail) {
                return Response.error(500, "{\"detail\":\"Regeneration failed\"}".toResponseBody("application/json".toMediaTypeOrNull()))
            }
            return Response.success(
                AssistantChatResponseDto(
                    message = "Regenerated analysis: Momentum indicators point to a strong consolidation phase.",
                    conversationId = conversationId
                )
            )
        }
    }

    private lateinit var fakeApiService: FakeAssistantApiService
    private lateinit var repository: AssistantRepositoryImpl

    @Before
    fun setUp() {
        fakeApiService = FakeAssistantApiService()
        repository = AssistantRepositoryImpl(fakeApiService)
    }

    @Test
    fun testAssistantDtoMapper_mapChatResponseDtoToDomain() {
        val dto = AssistantChatResponseDto(
            message = "Buy recommendation for LUCK",
            conversationId = "conv-101"
        )
        val domain = AssistantDtoMapper.mapChatResponseDtoToDomain(dto)

        assertEquals("Buy recommendation for LUCK", domain.message)
        assertEquals("conv-101", domain.conversationId)
    }

    @Test
    fun testAssistantDtoMapper_mapConversationWithMessagesDtoToDomain() {
        val dto = AssistantConversationMessagesResponseDto(
            conversation = AssistantConversationResponseDto(
                id = "conv-1",
                userId = "user-123",
                title = "Cement Sector Analysis",
                createdAt = "2026-09-24T10:00:00Z",
                updatedAt = "2026-09-24T10:30:00Z"
            ),
            messages = listOf(
                AssistantMessageResponseDto(
                    id = "msg-1",
                    conversationId = "conv-1",
                    role = "user",
                    content = "How is LUCK performing today?",
                    createdAt = "2026-09-24T10:00:00Z"
                ),
                AssistantMessageResponseDto(
                    id = "msg-2",
                    conversationId = "conv-1",
                    role = "assistant",
                    content = "LUCK is trading up +2.4% with solid volume.",
                    createdAt = "2026-09-24T10:00:05Z"
                )
            )
        )
        val domain = AssistantDtoMapper.mapConversationWithMessagesDtoToDomain(dto)

        assertEquals("conv-1", domain.conversation.id)
        assertEquals("user-123", domain.conversation.userId)
        assertEquals("Cement Sector Analysis", domain.conversation.title)
        assertEquals(2, domain.messages.size)
        assertEquals(MessageRole.USER, domain.messages[0].role)
        assertEquals(MessageRole.ASSISTANT, domain.messages[1].role)
    }

    @Test
    fun testRepository_SendMessage_Success() = runBlocking {
        val result = repository.sendMessage("What is the trend for OGDC?", null)
        assertTrue(result.isSuccess)
        val chatResult = result.getOrNull()
        assertNotNull(chatResult)
        assertTrue(chatResult!!.message.contains("bullish", ignoreCase = true))
        assertEquals("conv-new-1", chatResult.conversationId)
    }

    @Test
    fun testRepository_ListAndCreateConversations() = runBlocking {
        // Initially empty
        val listBefore = repository.listConversations().getOrNull()
        assertNotNull(listBefore)
        assertTrue(listBefore!!.isEmpty())

        // Create conversation
        val createResult = repository.createConversation("Tech Sector Inquiry")
        assertTrue(createResult.isSuccess)
        val conv = createResult.getOrNull()
        assertNotNull(conv)
        assertEquals("Tech Sector Inquiry", conv!!.title)

        // List again -> should have 1 item
        val listAfter = repository.listConversations().getOrNull()
        assertNotNull(listAfter)
        assertEquals(1, listAfter!!.size)
        assertEquals("Tech Sector Inquiry", listAfter[0].title)
    }

    @Test
    fun testRepository_UpdateAndDeleteConversation() = runBlocking {
        val conv = repository.createConversation("Old Title").getOrNull()!!

        // Update title
        val updated = repository.updateConversationTitle(conv.id, "New Title").getOrNull()
        assertNotNull(updated)
        assertEquals("New Title", updated!!.title)

        // Delete conversation
        val deleteResult = repository.deleteConversation(conv.id)
        assertTrue(deleteResult.isSuccess)

        // List should be empty
        val list = repository.listConversations().getOrNull()!!
        assertTrue(list.isEmpty())
    }

    @Test
    fun testRepository_RegenerateResponse() = runBlocking {
        val result = repository.regenerateResponse("conv-123")
        assertTrue(result.isSuccess)
        val chatResult = result.getOrNull()
        assertNotNull(chatResult)
        assertTrue(chatResult!!.message.contains("Regenerated", ignoreCase = true))
    }
}
