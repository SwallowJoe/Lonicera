package com.android.lonicera.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.android.lonicera.components.chat.model.ChatUIMessage
import com.llmsdk.deepseek.models.ChatMessage

@Entity(tableName = "tb_chat")
data class ChatEntity(
    @PrimaryKey val createdTimestamp: String,
    val title: String,
    val updateTimestamp: Long,
    val systemPrompt: String,
    val messages: ArrayList<ChatUIMessage>
) {
    fun chatMessages(): List<ChatMessage> {
        return messages.filter { !it.isError }.map { it.message }
    }

    fun filterChatMessages(predicate: (ChatUIMessage) -> Boolean): List<ChatMessage> {
        return messages.filter { predicate(it) }.map { it.message }
    }

    override fun toString(): String {
        return "ChatEntity{title=$title, createdTimestamp=$createdTimestamp, systemPrompt=$systemPrompt, messages=${messages.joinToString()}}"
    }
}