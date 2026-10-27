package com.rainy.ai.ai

import com.rainy.ai.data.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class Part(val text: String? = null)

@Serializable
data class Content(val role: String? = null, val parts: List<Part> = emptyList())

@Serializable
data class GenConfig(val temperature: Double)

@Serializable
data class GenRequest(
    val systemInstruction: Content? = null,
    val contents: List<Content>,
    val generationConfig: GenConfig? = null
)

@Serializable
data class Candidate(val content: Content? = null, val finishReason: String? = null)

@Serializable
data class ApiError(val message: String? = null)

@Serializable
data class GenResponse(val candidates: List<Candidate>? = null, val error: ApiError? = null)

object GeminiClient {

    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun generate(
        history: List<ChatMessage>,
        apiKey: String,
        model: String,
        systemPrompt: String = Persona.SYSTEM_PROMPT
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            require(apiKey.isNotBlank()) { "Settings mein Gemini API key daalo." }

            val request = GenRequest(
                systemInstruction = Content(parts = listOf(Part(systemPrompt))),
                contents = history.map {
                    Content(role = if (it.isUser) "user" else "model", parts = listOf(Part(it.text)))
                },
                generationConfig = GenConfig(temperature = 0.9)
            )
            val body = json.encodeToString(GenRequest.serializer(), request)
                .toRequestBody("application/json".toMediaType())

            val httpRequest = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
                .header("x-goog-api-key", apiKey)
                .post(body)
                .build()

            http.newCall(httpRequest).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                val parsed = runCatching {
                    json.decodeFromString(GenResponse.serializer(), raw)
                }.getOrNull()

                if (!resp.isSuccessful) {
                    error("Gemini error ${resp.code}: ${parsed?.error?.message ?: raw.take(200)}")
                }
                val text = parsed?.candidates?.firstOrNull()?.content?.parts
                    ?.mapNotNull { it.text }?.joinToString("")
                if (text.isNullOrBlank()) error("Gemini ne khaali jawab diya.") else text
            }
        }
    }
}
