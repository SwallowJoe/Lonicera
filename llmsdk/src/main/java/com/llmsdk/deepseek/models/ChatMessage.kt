@file:OptIn(ExperimentalSerializationApi::class)

package com.llmsdk.deepseek.models

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic

@Serializable
@JsonClassDiscriminator("role")
sealed interface ChatMessage: java.io.Serializable {
    val content: String?
}

@Serializable
@SerialName("system")
data class SystemMessage(override val content: String, val name: String? = null) : ChatMessage {
    override fun toString(): String {
        return "SystemMessage(content='$content', name=$name)"
    }
}

@Serializable
@SerialName("user")
data class UserMessage(override val content: String, val name: String? = null) : ChatMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserMessage) return false
        return content == other.content && name == other.name
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "UserMessage(content='$content', name=$name)"
}

@Serializable
@SerialName("tool")
data class ToolMessage(override val content: String, val tool_call_id: String? = null): ChatMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ToolMessage) return false
        return content == other.content && tool_call_id == other.tool_call_id
    }

    override fun hashCode(): Int {
        var result = content.hashCode()
        result = 31 * result + (tool_call_id?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ToolMessage(content=$content, tool_call_id=$tool_call_id)"
    }
}

@Serializable
@SerialName("assistant")
data class AssistantMessage(
    override var content: String? = null,
    val name: String? = null,
    val prefix: Boolean? = null,
    var reasoning_content: String? = null,
    var tool_calls: ArrayList<ToolCall>? = null
) : ChatMessage {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssistantMessage) return false
        return content == other.content &&
                name == other.name &&
                prefix == other.prefix &&
                reasoning_content == other.reasoning_content
    }

    override fun hashCode(): Int {
        var result = content.hashCode()  // content 已经是非空了
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (prefix?.hashCode() ?: 0)
        result = 31 * result + (reasoning_content?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "AssistantMessage(content=$content, name=$name, prefix=$prefix, reasoningContent=$reasoning_content, toolCalls=${tool_calls?.joinToString()})"
}

val ChatMessageModule = SerializersModule {
    polymorphic(ChatMessage::class) {
        subclass(SystemMessage::class, SystemMessage.serializer())
        subclass(UserMessage::class, UserMessage.serializer())
        subclass(ToolMessage::class, ToolMessage.serializer())
        subclass(AssistantMessage::class, AssistantMessage.serializer())
        // subclass(ChatCompletionMessage::class, ChatCompletionMessage.serializer())
    }
}