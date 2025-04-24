package com.android.lonicera.components.chat.model

import com.android.lonicera.R
import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.UserMessage

// 消息数据类
data class ChatUIMessage(
    val content: ChatMessage,
    val timestamp: Long,
    val isSender: Boolean = content is UserMessage,
    val avatar: Int = if (content !is UserMessage) R.drawable.ai_bot else 0, // 本地资源ID
    val timeout: Boolean = false,
    val completion_tokens: Int = 0,
    val prompt_hit_tokens: Int = 0,
    val prompt_miss_tokens: Int = 0,
    val reasoning_tokens: Int = 0,
)