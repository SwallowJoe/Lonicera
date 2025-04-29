package com.llmsdk.deepseek.models

import com.llmsdk.base.ChatModel

fun chatCompletionParams(block: ChatCompletionParams.Builder.() -> Unit): ChatCompletionParams {
    return ChatCompletionParams.Builder().apply(block).build()
}

fun chatCompletionStreamParams(block: ChatCompletionParams.StreamBuilder.() -> Unit): ChatCompletionParams {
    return ChatCompletionParams.StreamBuilder().apply(block).build()
}

class ChatCompletionParams internal constructor(
    val model: ChatModel,
    frequency_penalty: Double? = null,
    max_tokens: Int? = null,
    presence_penalty: Double? = null,
    val response_format: ResponseFormat? = null,
    stop: StopReason? = null,
    val stream: Boolean? = null,
    val stream_options: StreamOptions? = null,
    temperature: Double? = null,
    top_p: Double? = null,
    val tools: List<Tool>? = null,
    val tool_choice: ToolChoice? = null,
    val logprobs: Boolean? = null,
    val top_logprobs: Int? = null,
) : DeepSeekParams(frequency_penalty, max_tokens, presence_penalty, stop, temperature, top_p) {

    /**
     * Builder for creating [ChatCompletionParams] with standard (non-streaming) configuration.
     */
    class Builder {
        var model: ChatModel = ChatModel.DEEPSEEK_CHAT
        var frequency_penalty: Double? = null
        var max_tokens: Int? = null
        var presence_penalty: Double? = null
        var response_format: ResponseFormat? = null
        var stop: StopReason? = null
        var temperature: Double? = null
        var top_p: Double? = null
        var tools: List<Tool>? = null
        var tool_choice: ToolChoice? = null
        var logprobs: Boolean? = null
        var top_logprobs: Int? = null

        internal fun build(): ChatCompletionParams {
            frequency_penalty?.let { require(it in -2.0..2.0) { "frequency_penalty must be between -2.0 and 2.0" } }
            max_tokens?.let { require(it in 1..8192) { "max_tokens must be between 1 and 8192" } }
            presence_penalty?.let { require(it in -2.0..2.0) { "presence_penalty must be between -2.0 and 2.0" } }
            temperature?.let { require(it in 0.0..2.0) { "temperature must be between 0.0 and 2.0" } }
            top_logprobs?.let { require(it <= 20) { "top_logprobs must be <= 20" } }

            return ChatCompletionParams(
                model = model,
                frequency_penalty = frequency_penalty,
                max_tokens = max_tokens,
                presence_penalty = presence_penalty,
                response_format = response_format,
                stop = stop,
                temperature = temperature,
                top_p = top_p,
                tools = tools,
                tool_choice = tool_choice,
                logprobs = logprobs,
                top_logprobs = top_logprobs,
            )
        }
    }

    /**
     * Builder for creating [ChatCompletionParams] specifically configured for streaming responses.
     */
    class StreamBuilder {
        var model: ChatModel = ChatModel.DEEPSEEK_CHAT
        var frequency_penalty: Double? = null
        var max_tokens: Int? = null
        var presence_penalty: Double? = null
        var response_format: ResponseFormat? = null
        var stop: StopReason? = null
        var stream_options: StreamOptions? = null
        var temperature: Double? = null
        var top_p: Double? = null
        var tools: List<Tool>? = null
        var tool_choice: ToolChoice? = null
        var logprobs: Boolean? = null
        var top_logprobs: Int? = null

        internal fun build(): ChatCompletionParams {
            frequency_penalty?.let { require(it in -2.0..2.0) { "frequency_penalty must be between -2.0 and 2.0" } }
            max_tokens?.let { require(it in 1..8192) { "max_tokens must be between 1 and 8192" } }
            presence_penalty?.let { require(it in -2.0..2.0) { "presence_penalty must be between -2.0 and 2.0" } }
            temperature?.let { require(it in 0.0..2.0) { "temperature must be between 0.0 and 2.0" } }
            top_logprobs?.let { require(it <= 20) { "top_logprobs must be <= 20" } }

            return ChatCompletionParams(
                model = model,
                frequency_penalty = frequency_penalty,
                max_tokens = max_tokens,
                presence_penalty = presence_penalty,
                response_format = response_format,
                stop = stop,
                stream = true,
                stream_options = stream_options,
                temperature = temperature,
                top_p = top_p,
                tools = tools,
                tool_choice = tool_choice,
                logprobs = logprobs,
                top_logprobs = top_logprobs,
            )
        }
    }

    /**
     * Creates a [ChatCompletionRequest] from these parameters and the provided messages.
     *
     * @param messages List of chat messages to include in the request
     * @return A fully configured [ChatCompletionRequest]
     */
    fun createRequest(messages: List<ChatMessage>): ChatCompletionRequest =
        ChatCompletionRequest(
            model = model,
            messages = messages,
            frequency_penalty = frequency_penalty,
            max_tokens = max_tokens,
            presence_penalty = presence_penalty,
            response_format = response_format,
            stop = stop,
            stream = stream,
            stream_options = stream_options,
            temperature = temperature,
            top_p = top_p,
            tools = tools,
            tool_choice = tool_choice,
            logprobs = logprobs,
            top_logprobs = top_logprobs,
        )

    /**
     * Creates a copy of these parameters with optional changes to specific properties.
     *
     * Example:
     * ```kotlin
     * // Create a streaming version of existing parameters
     * val streamParams = regularParams.copy(stream = true)
     * ```
     *
     * @param model New chat model to use, or existing value if not specified
     * @param frequency_penalty New frequency penalty value, or existing value if not specified
     * @param max_tokens New maximum token count, or existing value if not specified
     * @param presence_penalty New presence penalty value, or existing value if not specified
     * @param response_format New response format, or existing value if not specified
     * @param stop New stop reason, or existing value if not specified
     * @param stream New streaming setting, or existing value if not specified
     * @param stream_options New stream options, or existing value if not specified
     * @param temperature New temperature value, or existing value if not specified
     * @param top_p New top-p value, or existing value if not specified
     * @param tools New tools list, or existing value if not specified
     * @param tool_choice New tool choice, or existing value if not specified
     * @param logprobs New log probabilities setting, or existing value if not specified
     * @param top_logprobs New top log probabilities count, or existing value if not specified
     * @return A new [ChatCompletionParams] instance with the specified changes
     */
    fun copy(
        model: ChatModel = this.model,
        frequency_penalty: Double? = this.frequency_penalty,
        max_tokens: Int? = this.max_tokens,
        presence_penalty: Double? = this.presence_penalty,
        response_format: ResponseFormat? = this.response_format,
        stop: StopReason? = this.stop,
        stream: Boolean? = this.stream,
        stream_options: StreamOptions? = this.stream_options,
        temperature: Double? = this.temperature,
        top_p: Double? = this.top_p,
        tools: List<Tool>? = this.tools,
        tool_choice: ToolChoice? = this.tool_choice,
        logprobs: Boolean? = this.logprobs,
        top_logprobs: Int? = this.top_logprobs,
    ): ChatCompletionParams {
        return ChatCompletionParams(
            model = model,
            frequency_penalty = frequency_penalty,
            max_tokens = max_tokens,
            presence_penalty = presence_penalty,
            response_format = response_format,
            stop = stop,
            stream = stream,
            stream_options = stream_options,
            temperature = temperature,
            top_p = top_p,
            tools = tools,
            tool_choice = tool_choice,
            logprobs = logprobs,
            top_logprobs = top_logprobs,
        )
    }


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatCompletionParams) return false

        return model == other.model &&
                frequency_penalty == other.frequency_penalty &&
                max_tokens == other.max_tokens &&
                presence_penalty == other.presence_penalty &&
                response_format == other.response_format &&
                stop == other.stop &&
                stream == other.stream &&
                stream_options == other.stream_options &&
                temperature == other.temperature &&
                top_p == other.top_p &&
                tools == other.tools &&
                tool_choice == other.tool_choice &&
                logprobs == other.logprobs &&
                top_logprobs == other.top_logprobs
    }

    override fun hashCode(): Int {
        var result = frequency_penalty?.hashCode() ?: 0
        result = 31 * result + (max_tokens ?: 0)
        result = 31 * result + (presence_penalty?.hashCode() ?: 0)
        result = 31 * result + (stream?.hashCode() ?: 0)
        result = 31 * result + (temperature?.hashCode() ?: 0)
        result = 31 * result + (top_p?.hashCode() ?: 0)
        result = 31 * result + (logprobs?.hashCode() ?: 0)
        result = 31 * result + (top_logprobs ?: 0)
        result = 31 * result + model.hashCode()
        result = 31 * result + (response_format?.hashCode() ?: 0)
        result = 31 * result + (stop?.hashCode() ?: 0)
        result = 31 * result + (stream_options?.hashCode() ?: 0)
        result = 31 * result + (tools?.hashCode() ?: 0)
        result = 31 * result + (tool_choice?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "ChatCompletionParams(model=$model, frequency_penalty=$frequency_penalty, max_tokens=$max_tokens, presence_penalty=$presence_penalty, response_format=$response_format, stop=$stop, stream=$stream, stream_options=$stream_options, temperature=$temperature, top_p=$top_p, tools=$tools, tool_choice=$tool_choice, logprobs=$logprobs, top_logprobs=$top_logprobs)"
    }
}