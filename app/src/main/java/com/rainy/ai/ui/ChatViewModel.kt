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
import com.rainy.ai.data.SecureSettings
import kotlinx.coroutines.launch

class ChatViewModel(app: Application) : AndroidViewModel(app) {

    private val settings = SecureSettings(app)

    val messages = mutableStateListOf<ChatMessage>()

    var loading by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var apiKey by mutableStateOf(settings.apiKey)
        private set
    var model by mutableStateOf(settings.model)
        private set

    private var nextId = 0L

    fun saveSettings(newKey: String, newModel: String) {
        settings.apiKey = newKey
        settings.model = newModel.ifBlank { SecureSettings.DEFAULT_MODEL }
        apiKey = settings.apiKey
        model = settings.model
    }

    fun send(text: String) {
        if (text.isBlank() || loading) return
        if (apiKey.isBlank()) {
            error = "Settings mein Gemini API key daalo."
            return
        }
        error = null
        messages.add(ChatMessage(nextId++, true, text.trim()))
        loading = true
        val snapshot = messages.toList()
        viewModelScope.launch {
            GeminiClient.generate(snapshot, apiKey, model)
                .onSuccess { messages.add(ChatMessage(nextId++, false, it)) }
                .onFailure { error = it.message ?: "Unknown error" }
            loading = false
        }
    }

    fun clearChat() {
        messages.clear()
        error = null
    }
}
