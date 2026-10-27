package com.rainy.ai.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rainy.ai.ai.GeminiClient
import com.rainy.ai.data.ChatMessage
import com.rainy.ai.data.ChatRepository
import com.rainy.ai.data.SecureSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SecureSettings(app)
    private val repo = ChatRepository(app)

    val messages = mutableStateListOf<ChatMessage>()

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var canRetry by mutableStateOf(false)
        private set
    var historyLoaded by mutableStateOf(false)
        private set
    var apiKey by mutableStateOf(settings.apiKey)
        private set
    var model by mutableStateOf(settings.model)
        private set

    // Settings > Test connection state
    var models by mutableStateOf<List<String>>(emptyList())
        private set
    var testing by mutableStateOf(false)
        private set
    var testStatus by mutableStateOf<String?>(null)
        private set
    var testOk by mutableStateOf<Boolean?>(null)
        private set

    init {
        viewModelScope.launch {
            try {
                val saved = repo.loadLatest()
                messages.addAll(saved)
                canRetry = saved.lastOrNull()?.isUser == true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "History load nahi hui: ${e.message ?: "unknown error"}"
            } finally {
                historyLoaded = true
            }
        }
    }

    fun saveSettings(newKey: String, newModel: String) {
        settings.apiKey = newKey
        settings.model = newModel.ifBlank { SecureSettings.DEFAULT_MODEL }
        apiKey = settings.apiKey
        model = settings.model
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || loading || !historyLoaded) return
        if (apiKey.isBlank()) {
            error = "Settings mein Gemini API key daalo."
            return
        }
        error = null
        canRetry = false
        loading = true
        viewModelScope.launch {
            try {
                messages.add(repo.append(true, t))
                requestReply()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = "Message save nahi hua: ${e.message ?: "unknown error"}"
            } finally {
                loading = false
            }
        }
    }

    fun retry() {
        if (!canRetry || loading || messages.lastOrNull()?.isUser != true) return
        if (apiKey.isBlank()) {
            error = "Settings mein Gemini API key daalo."
            return
        }
        error = null
        canRetry = false
        loading = true
        viewModelScope.launch {
            try {
                requestReply()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
                canRetry = true
            } finally {
                loading = false
            }
        }
    }

    private suspend fun requestReply() {
        val result = GeminiClient.generate(messages.toList(), apiKey, model)
        val reply = result.getOrNull()
        if (reply != null) {
            messages.add(repo.append(false, reply))
            canRetry = false
        } else {
            error = result.exceptionOrNull()?.message ?: "Unknown error"
            canRetry = true
        }
    }

    fun newChat() {
        if (loading) return
        viewModelScope.launch {
            try {
                repo.startNew()
                messages.clear()
                error = null
                canRetry = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            }
        }
    }

    fun clearChat() {
        if (loading) return
        viewModelScope.launch {
            try {
                repo.clearCurrent()
                messages.clear()
                error = null
                canRetry = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            }
        }
    }

    /** Privacy control: deletes every saved conversation. */
    fun deleteAllHistory() {
        if (loading) return
        viewModelScope.launch {
            try {
                repo.deleteAll()
                messages.clear()
                error = null
                canRetry = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            }
        }
    }

    fun exportText(onResult: (String) -> Unit) {
        viewModelScope.launch {
            try {
                onResult(repo.exportText())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e.message ?: "Unknown error"
            }
        }
    }

    /** Fetches the real model list from Gemini using the key typed in Settings. */
    fun loadModels(key: String) {
        if (testing) return
        testing = true
        testOk = null
        testStatus = "Models load ho rahe hain..."
        viewModelScope.launch {
            try {
                val result = GeminiClient.listModels(key.trim())
                val list = result.getOrNull()
                if (list != null) {
                    models = list.map { it.name }.sorted()
                    testOk = true
                    testStatus = "${list.size} models mile jo chat support karte hain."
                } else {
                    testOk = false
                    testStatus = result.exceptionOrNull()?.message ?: "Models load nahi hue."
                }
            } finally {
                testing = false
            }
        }
    }

    /** Sends a real prompt. Success is only reported if the model actually replied. */
    fun testConnection(key: String, modelId: String) {
        if (testing) return
        testing = true
        testOk = null
        testStatus = "Test prompt bheja ja raha hai..."
        viewModelScope.launch {
            try {
                val result = GeminiClient.testGeneration(key.trim(), modelId)
                val reply = result.getOrNull()
                if (reply != null) {
                    testOk = true
                    testStatus = "Kaam kar raha hai. Model ka jawab: ${reply.trim().take(80)}"
                } else {
                    testOk = false
                    testStatus = result.exceptionOrNull()?.message ?: "Test fail ho gaya."
                }
            } finally {
                testing = false
            }
        }
    }
}
