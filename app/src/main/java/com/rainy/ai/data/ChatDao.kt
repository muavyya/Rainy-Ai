package com.rainy.ai.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChatDao {
    @Insert
    suspend fun insertConversation(c: ConversationEntity): Long

    @Insert
    suspend fun insertMessage(m: MessageEntity): Long

    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC LIMIT 1")
    suspend fun latestConversation(): ConversationEntity?

    @Query("SELECT * FROM messages WHERE conversationId = :id ORDER BY timestamp ASC, id ASC")
    suspend fun messagesFor(id: Long): List<MessageEntity>

    @Query("UPDATE conversations SET updatedAt = :t WHERE id = :id")
    suspend fun touch(id: Long, t: Long)

    @Query("UPDATE conversations SET title = :title WHERE id = :id")
    suspend fun rename(id: Long, title: String)

    @Query("DELETE FROM messages WHERE conversationId = :id")
    suspend fun clearMessages(id: Long)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM conversations")
    suspend fun deleteAll()
}
