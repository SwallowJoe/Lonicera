package com.android.lonicera.components.chat

import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.UserMessage

// 消息数据类
data class ChatUIMessage(
    val id: Int,
    val content: ChatMessage,
    val timestamp: Long,
    val isSender: Boolean = content is UserMessage,
    val avatar: Int, // 本地资源ID
    val timeout: Boolean = false
)