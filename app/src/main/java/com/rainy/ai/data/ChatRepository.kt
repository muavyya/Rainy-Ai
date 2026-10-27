package com.rainy.ai.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatRepository(context: Context) {

    private val dao: ChatDao = AppDatabase.get(context).chatDao()
    private val lock = Mutex()
    private var currentId: Long? = null

    private fun MessageEntity.toChat() = ChatMessage(id, isUser, text, timestamp)

    /** Loads the most recent conversation (or an empty list) and makes it current. */
    suspend fun loadLatest(): List<ChatMessage> = lock.withLock {
        val latest = dao.latestConversation()
        currentId = latest?.id
        if (latest == null) emptyList() else dao.messagesFor(latest.id).map { it.toChat() }
    }

    /** Starts a fresh chat. The row is created lazily on the first message. */
    suspend fun startNew() = lock.withLock { currentId = null }

    /** Saves one message and returns it with its real id and timestamp. */
    suspend fun append(isUser: Boolean, text: String): ChatMessage = lock.withLock {
        val now = System.currentTimeMillis()
        val convId = currentId ?: dao.insertConversation(
            ConversationEntity(
                title = if (isUser) text.trim().take(40).ifBlank { "New chat" } else "New chat",
                updatedAt = now
            )
        ).also { currentId = it }
        val id = dao.insertMessage(
            MessageEntity(
                conversationId = convId,
                isUser = isUser,
                text = text,
                timestamp = now
            )
        )
        dao.touch(convId, now)
        ChatMessage(id, isUser, text, now)
    }

    /** Removes all messages of the current chat. */
    suspend fun clearCurrent() = lock.withLock {
        currentId?.let { dao.clearMessages(it) }
        Unit
    }

    suspend fun deleteMessage(id: Long) = lock.withLock { dao.deleteMessage(id) }

    /** Privacy control: wipes every conversation and message. */
    suspend fun deleteAll() = lock.withLock {
        dao.deleteAll()
        currentId = null
    }

    /** Plain-text export of the current chat. */
    suspend fun exportText(): String = lock.withLock {
        val id = currentId ?: return@withLock ""
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        dao.messagesFor(id).joinToString("\n\n") {
            val who = if (it.isUser) "You" else "Rainy"
            "[${fmt.format(Date(it.timestamp))}] $who:\n${it.text}"
        }
    }
}
