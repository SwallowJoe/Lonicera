package com.android.lonicera.components.chat.model

import com.android.lonicera.R
import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.UserMessage
import kotlinx.serialization.Serializable
import java.util.UUID

// 消息数据类
@Serializable
data class ChatUIMessage(
    val uuid: String = UUID.randomUUID().toString(),
    val message: ChatMessage,
    val avatar: Int = if (message !is UserMessage) R.drawable.ai_bot_48px else R.drawable.user, // 本地资源ID
    val isError: Boolean = false,
    var isToolCall: Boolean = false,
    var isToolResponse: Boolean = false,
    val timestamp: Long,
    var completion_tokens: Int = 0,
    var prompt_hit_tokens: Int = 0,
    var prompt_miss_tokens: Int = 0,
    val reasoning_tokens: Int = 0,
): java.io.Serializable {
    fun fromUser(): Boolean {
        return message is UserMessage
    }

    override fun toString(): String {
        return message.toString()
    }
}