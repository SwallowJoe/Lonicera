package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatChoice(
    val finish_reason: FinishReason,
    val index: Int,
    val message: AssistantMessage,
    val logprobs: Logprobs?
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatChoice) return false
        return index == other.index
                && finish_reason == other.finish_reason
                && message == other.message
                && logprobs == other.logprobs
    }

    override fun hashCode(): Int {
        var result = index
        result = 31 * result + finish_reason.hashCode()
        result = 31 * result + message.hashCode()
        result = 31 * result + logprobs.hashCode()
        return result
    }

    override fun toString(): String {
        return "ChatChoice(index=$index, finish_reason='$finish_reason', message=$message, logprobs=$logprobs)"
    }
}