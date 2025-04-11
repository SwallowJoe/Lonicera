package com.android.lonicera.components.chat

// 消息数据类
data class ChatMessage(
    val id: Int,
    val content: String,
    val timestamp: Long,
    val isSender: Boolean,
    val avatar: Int, // 本地资源ID
    val timeout: Boolean = false
)