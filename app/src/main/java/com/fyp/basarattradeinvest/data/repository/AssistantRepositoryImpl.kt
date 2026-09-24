package com.fyp.basarattradeinvest.data.repository

import com.fyp.basarattradeinvest.data.mapper.AssistantDtoMapper
import com.fyp.basarattradeinvest.data.remote.NetworkClient
import com.fyp.basarattradeinvest.data.remote.api.AssistantApiService
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantChatRequestDto
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantConversationCreateDto
import com.fyp.basarattradeinvest.data.remote.dto.assistant.AssistantConversationUpdateDto
import com.fyp.basarattradeinvest.domain.model.assistant.AssistantChatResult
import com.fyp.basarattradeinvest.domain.model.assistant.AssistantConversation
import com.fyp.basarattradeinvest.domain.model.assistant.AssistantConversationWithMessages
import com.fyp.basarattradeinvest.domain.repository.AssistantRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Repository implementation for the AI Investment Assistant module.
 * Consumes live backend endpoints via Retrofit + OkHttp.
 * SSE streaming uses raw ResponseBody with @Streaming annotation.
 */
class AssistantRepositoryImpl(
    private val apiService: AssistantApiService = NetworkClient.assistantApiService
) : AssistantRepository {

    override suspend fun sendMessage(
        message: String,
        conversationId: String?
    ): Result<AssistantChatResult> = withContext(Dispatchers.IO) {
        try {
            val request = AssistantChatRequestDto(
                message = message,
                conversationId = conversationId
            )
            val response = apiService.chat(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(AssistantDtoMapper.mapChatResponseDtoToDomain(response.body()!!))
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(response.code(), errorBody)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(getNetworkErrorMessage(e)))
        }
    }

    override fun sendMessageStream(
        message: String,
        conversationId: String?
    ): Flow<Result<String>> = flow {
        try {
            val request = AssistantChatRequestDto(
                message = message,
                conversationId = conversationId
            )
            val response = apiService.chatStream(request)

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string()
                emit(Result.failure(Exception(parseErrorMessage(response.code(), errorBody))))
                return@flow
            }

            val body = response.body()
            if (body == null) {
                emit(Result.failure(Exception("Empty response from server")))
                return@flow
            }

            val reader = BufferedReader(InputStreamReader(body.byteStream()))
            val accumulated = StringBuilder()

            try {
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line ?: continue

                    // SSE protocol: lines starting with "data: " contain the payload
                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.removePrefix("data: ").trim()

                        // "[DONE]" signals end of stream (common FastAPI SSE pattern)
                        if (data == "[DONE]") {
                            break
                        }

                        // Attempt to parse as JSON chunk or treat as plain text
                        val chunk = parseStreamChunk(data)
                        if (chunk.isNotEmpty()) {
                            accumulated.append(chunk)
                            emit(Result.success(accumulated.toString()))
                        }
                    }
                    // Ignore empty lines (SSE keepalive) and comment lines (starting with ':')
                }

                // Final emission with the complete accumulated content
                if (accumulated.isNotEmpty()) {
                    emit(Result.success(accumulated.toString()))
                }
            } finally {
                reader.close()
                body.close()
            }
        } catch (e: Exception) {
            emit(Result.failure(Exception(getNetworkErrorMessage(e))))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun listConversations(
        limit: Int?,
        offset: Int?
    ): Result<List<AssistantConversation>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.listConversations(limit, offset)
            if (response.isSuccessful && response.body() != null) {
                Result.success(AssistantDtoMapper.mapConversationListDtoToDomain(response.body()!!))
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(response.code(), errorBody)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(getNetworkErrorMessage(e)))
        }
    }

    override suspend fun createConversation(
        title: String?
    ): Result<AssistantConversation> = withContext(Dispatchers.IO) {
        try {
            val request = AssistantConversationCreateDto(title = title)
            val response = apiService.createConversation(request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(AssistantDtoMapper.mapConversationDtoToDomain(response.body()!!))
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(response.code(), errorBody)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(getNetworkErrorMessage(e)))
        }
    }

    override suspend fun getConversation(
        conversationId: String
    ): Result<AssistantConversationWithMessages> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getConversation(conversationId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(AssistantDtoMapper.mapConversationWithMessagesDtoToDomain(response.body()!!))
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(response.code(), errorBody)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(getNetworkErrorMessage(e)))
        }
    }

    override suspend fun updateConversationTitle(
        conversationId: String,
        title: String?
    ): Result<AssistantConversation> = withContext(Dispatchers.IO) {
        try {
            val request = AssistantConversationUpdateDto(title = title)
            val response = apiService.updateConversation(conversationId, request)
            if (response.isSuccessful && response.body() != null) {
                Result.success(AssistantDtoMapper.mapConversationDtoToDomain(response.body()!!))
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(response.code(), errorBody)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(getNetworkErrorMessage(e)))
        }
    }

    override suspend fun deleteConversation(
        conversationId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteConversation(conversationId)
            if (response.isSuccessful || response.code() == 204) {
                Result.success(Unit)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(response.code(), errorBody)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(getNetworkErrorMessage(e)))
        }
    }

    override suspend fun regenerateResponse(
        conversationId: String
    ): Result<AssistantChatResult> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.regenerateResponse(conversationId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(AssistantDtoMapper.mapChatResponseDtoToDomain(response.body()!!))
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception(parseErrorMessage(response.code(), errorBody)))
            }
        } catch (e: Exception) {
            Result.failure(Exception(getNetworkErrorMessage(e)))
        }
    }

    // ─── SSE Parsing ───

    /**
     * Parses a single SSE data chunk.
     * Attempts JSON parsing first; falls back to plain text.
     */
    private fun parseStreamChunk(data: String): String {
        // Try to extract "content" or "message" from JSON
        return try {
            val json = com.google.gson.JsonParser.parseString(data).asJsonObject
            when {
                json.has("content") -> json.get("content").asString
                json.has("message") -> json.get("message").asString
                json.has("text") -> json.get("text").asString
                json.has("delta") -> {
                    val delta = json.getAsJsonObject("delta")
                    delta?.get("content")?.asString ?: ""
                }
                else -> data // Treat entire data as text chunk
            }
        } catch (e: Exception) {
            // Not JSON — treat the raw data as a text chunk
            data
        }
    }

    // ─── Error Handling ───

    private fun parseErrorMessage(statusCode: Int, errorBody: String?): String {
        return when (statusCode) {
            401 -> "Session expired. Please sign in again."
            403 -> "You don't have access to this feature."
            404 -> "Conversation not found."
            422 -> parseValidationError(errorBody)
            429 -> "Too many requests. Please wait a moment."
            in 500..599 -> "Unable to reach the assistant. Please try again later."
            else -> "Something went wrong (Error $statusCode)."
        }
    }

    private fun parseValidationError(errorBody: String?): String {
        if (errorBody.isNullOrBlank()) return "Invalid request."
        return try {
            val json = com.google.gson.JsonParser.parseString(errorBody).asJsonObject
            val detail = json.getAsJsonArray("detail")
            if (detail != null && detail.size() > 0) {
                val first = detail.get(0).asJsonObject
                first.get("msg")?.asString ?: "Validation error."
            } else {
                json.get("detail")?.asString ?: "Invalid request."
            }
        } catch (e: Exception) {
            "Invalid request."
        }
    }

    private fun getNetworkErrorMessage(e: Exception): String {
        return when (e) {
            is java.net.UnknownHostException -> "No internet connection."
            is java.net.SocketTimeoutException -> "Request timed out. Please try again."
            is java.net.ConnectException -> "Unable to connect to server."
            is javax.net.ssl.SSLException -> "Secure connection failed."
            else -> e.message ?: "An unexpected error occurred."
        }
    }
}
