package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatChoiceChunk(
    val delta: AssistantMessage,
    val index: Int,
    val finish_reason: FinishReason?,
    val logprobs: Logprobs?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatChoiceChunk) return false

        return delta == other.delta && finish_reason == other.finish_reason && index == other.index
    }

    override fun hashCode(): Int {
        var result = delta.hashCode()
        result = 31 * result + (finish_reason?.hashCode() ?: 0)
        result = 31 * result + index.hashCode()
        return result
    }

    override fun toString(): String =
        "ChatChoiceChunk(delta=$delta, finishReason=$finish_reason, index=$index)"
}