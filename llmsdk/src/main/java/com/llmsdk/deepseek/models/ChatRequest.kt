package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatRequest(
    val messages: List<Message>,
    val model: String,
    @FrequencyPenaltyRange val frequency_penalty: Double = 0.0,
    val max_tokens: Int = 2048,
    @PresencePenaltyRange val presence_penalty: Double = 0.0,
    val response_format: ResponseFormat = ResponseFormat(type = ResponseFormat.TEXT),
    val stop: List<String>? = null,
    val stream: Boolean = false,
    val stream_options: StreamOptions? = null,
    val temperature: Double = 1.0,
    val top_p: Double = 1.0,
    val tools: List<ToolCall>? = null,
    val tool_choice: String = "none",
    val logprobs: Boolean = false,
    val top_logprobs: Int? = null
) {
    companion object {

        @Retention(AnnotationRetention.SOURCE)
        @Target(AnnotationTarget.VALUE_PARAMETER)
        annotation class FrequencyPenaltyRange(
            val min: Double = -2.0,
            val max: Double = 2.0
        )

        @Retention(AnnotationRetention.SOURCE)
        @Target(AnnotationTarget.VALUE_PARAMETER)
        annotation class PresencePenaltyRange(
            val min: Double = -2.0,
            val max: Double = 2.0
        )
    }
}