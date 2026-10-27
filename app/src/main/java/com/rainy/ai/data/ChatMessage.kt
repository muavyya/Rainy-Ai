package com.rainy.ai.data

data class ChatMessage(
    val id: Long,
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = 0L
)
