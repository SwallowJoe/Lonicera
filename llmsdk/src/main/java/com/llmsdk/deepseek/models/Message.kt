package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class Message(
    val content: String,
    val role: String,
    val reasoning_content: String? = null,
    val tool_calls: List<ToolCall>? = null,
) {

    companion object {
        const val ROLE_USER = "user"
        const val ROLE_ASSISTANT = "assistant"
        const val ROLE_SYSTEM = "system"
    }

    fun isUser(): Boolean {
        return role == ROLE_USER
    }

    fun isAssistant(): Boolean {
        return role == ROLE_ASSISTANT
    }

    fun isSystem(): Boolean {
        return role == ROLE_SYSTEM
    }
}
