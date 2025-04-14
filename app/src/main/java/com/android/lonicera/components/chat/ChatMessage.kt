package com.android.lonicera.components.chat

import com.llmsdk.deepseek.models.Message

// 消息数据类
data class ChatMessage(
    val id: Int,
    val content: Message,
    val timestamp: Long,
    val isSender: Boolean = content.isUser(),
    val avatar: Int, // 本地资源ID
    val timeout: Boolean = false
)