package com.llmsdk.deepseek

import com.llmsdk.deepseek.models.ChatRequest.Companion.FrequencyPenaltyRange
import com.llmsdk.deepseek.models.ChatRequest.Companion.PresencePenaltyRange
import com.llmsdk.deepseek.models.ResponseFormat
import com.llmsdk.deepseek.models.StreamOptions
import com.llmsdk.deepseek.models.ToolCall

class DeepSeekConfig(
    var model: String,
    @FrequencyPenaltyRange var frequency_penalty: Double = 0.0,
    var max_tokens: Int = 2048,
    @PresencePenaltyRange var presence_penalty: Double = 0.0,
    var response_format: ResponseFormat = ResponseFormat(type = ResponseFormat.TEXT),
    var stop: List<String>? = null,
    var stream: Boolean = false,
    var stream_options: StreamOptions? = null,
    var temperature: Double = 1.0,
    var top_p: Double = 1.0,
    var tools: List<ToolCall>? = null,
    var tool_choice: String = "none",
    var logprobs: Boolean = false,
    var top_logprobs: Int? = null
) {

}