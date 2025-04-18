package com.llmsdk.deepseek

import com.llmsdk.deepseek.models.ChatModel
import com.llmsdk.deepseek.models.ResponseFormat
import com.llmsdk.deepseek.models.StopReason
import com.llmsdk.deepseek.models.StreamOptions
import com.llmsdk.deepseek.models.Tool
import com.llmsdk.deepseek.models.ToolChoice

class DeepSeekConfig(
    var model: ChatModel,
    var frequency_penalty: Double = 0.0,
    var max_tokens: Int = 2048,
    var presence_penalty: Double = 0.0,
    var response_format: ResponseFormat = ResponseFormat.TEXT,
    var stop: StopReason? = null,
    var stream: Boolean = false,
    var stream_options: StreamOptions? = null,
    var temperature: Double = 1.0,
    var top_p: Double = 1.0,
    var tools: List<Tool>? = null,
    var tool_choice: ToolChoice? = null,
    var logprobs: Boolean = false,
    var top_logprobs: Int? = null
) {

}