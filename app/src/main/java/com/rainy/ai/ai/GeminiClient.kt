package com.rainy.ai.ai

import com.rainy.ai.data.ChatMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class AiException(val kind: Kind, message: String) : Exception(message) {
    enum class Kind {
        NO_KEY, INVALID_KEY, RATE_LIMIT, MODEL_NOT_FOUND,
        NETWORK, TIMEOUT, BAD_RESPONSE, BLOCKED, SERVER, OTHER
    }
}

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
data class PromptFeedback(val blockReason: String? = null)

@Serializable
data class GenResponse(
    val candidates: List<Candidate>? = null,
    val promptFeedback: PromptFeedback? = null,
    val error: ApiError? = null
)

@Serializable
data class ErrorEnvelope(val error: ApiError? = null)

@Serializable
data class ModelInfo(
    val name: String = "",
    val displayName: String? = null,
    val supportedGenerationMethods: List<String> = emptyList()
)

@Serializable
data class ModelListResponse(
    val models: List<ModelInfo>? = null,
    val nextPageToken: String? = null
)

object GeminiClient {

    private const val BASE = "https://generativelanguage.googleapis.com/v1beta"

    private val json = Json { ignoreUnknownKeys = true }

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    fun cleanModel(model: String): String = model.trim().removePrefix("models/")

    private fun mapHttp(code: Int, msg: String?): AiException {
        val m = msg?.takeIf { it.isNotBlank() } ?: "HTTP $code"
        return when {
            code == 400 && m.contains("API key", ignoreCase = true) ->
                AiException(AiException.Kind.INVALID_KEY, "API key galat hai ya expire ho gayi hai. ($m)")
            code == 401 || code == 403 ->
                AiException(AiException.Kind.INVALID_KEY, "API key reject hui ya is model ki permission nahi. ($m)")
            code == 404 ->
                AiException(AiException.Kind.MODEL_NOT_FOUND, "Model nahi mila. Settings mein Test connection se sahi model chunein. ($m)")
            code == 429 ->
                AiException(AiException.Kind.RATE_LIMIT, "Rate limit ya quota khatam. Thori der baad try karein. ($m)")
            code >= 500 ->
                AiException(AiException.Kind.SERVER, "Gemini server error $code. Baad mein try karein. ($m)")
            else ->
                AiException(AiException.Kind.OTHER, "Gemini error $code: $m")
        }
    }

    private fun <T> guarded(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: AiException) {
        Result.failure(e)
    } catch (e: SocketTimeoutException) {
        Result.failure(AiException(AiException.Kind.TIMEOUT, "Request time out ho gayi. Internet check karein."))
    } catch (e: IOException) {
        Result.failure(AiException(AiException.Kind.NETWORK, "Network error. Internet check karein."))
    } catch (e: Exception) {
        Result.failure(AiException(AiException.Kind.OTHER, e.message ?: "Unknown error"))
    }

    private fun call(request: Request): String =
        http.newCall(request).execute().use { resp ->
            val raw = resp.body?.string().orEmpty()
            if (!resp.isSuccessful) {
                val msg = runCatching {
                    json.decodeFromString(ErrorEnvelope.serializer(), raw).error?.message
                }.getOrNull()
                throw mapHttp(resp.code, msg ?: raw.take(200))
            }
            raw
        }

    suspend fun generate(
        history: List<ChatMessage>,
        apiKey: String,
        model: String,
        systemPrompt: String = Persona.SYSTEM_PROMPT
    ): Result<String> = withContext(Dispatchers.IO) {
        guarded {
            if (apiKey.isBlank()) {
                throw AiException(AiException.Kind.NO_KEY, "Settings mein Gemini API key daalo.")
            }
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
                .url("$BASE/models/${cleanModel(model)}:generateContent")
                .header("x-goog-api-key", apiKey)
                .post(body)
                .build()

            val raw = call(httpRequest)
            val parsed = try {
                json.decodeFromString(GenResponse.serializer(), raw)
            } catch (e: Exception) {
                throw AiException(AiException.Kind.BAD_RESPONSE, "Gemini ka jawab samajh nahi aaya.")
            }
            parsed.promptFeedback?.blockReason?.let {
                throw AiException(AiException.Kind.BLOCKED, "Request block ho gayi: $it")
            }
            val candidate = parsed.candidates?.firstOrNull()
            val text: String = candidate?.content?.parts?.mapNotNull { it.text }?.joinToString("").orEmpty()
            if (text.isBlank()) {
                throw AiException(
                    AiException.Kind.BAD_RESPONSE,
                    "Gemini ne khaali jawab diya (${candidate?.finishReason ?: "no reason"})."
                )
            }
            text
        }
    }

    /** Lists models that support generateContent, using the real API response. */
    suspend fun listModels(apiKey: String): Result<List<ModelInfo>> = withContext(Dispatchers.IO) {
        guarded {
            if (apiKey.isBlank()) {
                throw AiException(AiException.Kind.NO_KEY, "Pehle API key daalo.")
            }
            val all = mutableListOf<ModelInfo>()
            var pageToken: String? = null
            var pages = 0
            do {
                val builder = "$BASE/models".toHttpUrl().newBuilder()
                    .addQueryParameter("pageSize", "100")
                val token = pageToken
                if (!token.isNullOrBlank()) builder.addQueryParameter("pageToken", token)
                val req = Request.Builder()
                    .url(builder.build())
                    .header("x-goog-api-key", apiKey)
                    .get()
                    .build()
                val raw = call(req)
                val parsed = try {
                    json.decodeFromString(ModelListResponse.serializer(), raw)
                } catch (e: Exception) {
                    throw AiException(AiException.Kind.BAD_RESPONSE, "Model list samajh nahi aayi.")
                }
                parsed.models?.let { all.addAll(it) }
                pageToken = parsed.nextPageToken
                pages++
            } while (!pageToken.isNullOrBlank() && pages < 5)

            all.filter { "generateContent" in it.supportedGenerationMethods }
                .map { it.copy(name = cleanModel(it.name)) }
        }
    }

    /** Sends a real minimal prompt. Success means the model actually replied. */
    suspend fun testGeneration(apiKey: String, model: String): Result<String> =
        generate(
            history = listOf(ChatMessage(0, true, "Reply with just the word: pong")),
            apiKey = apiKey,
            model = model,
            systemPrompt = "You are a connectivity test. Reply very briefly."
        )
}
