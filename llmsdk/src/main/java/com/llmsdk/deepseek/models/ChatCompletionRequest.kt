package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionRequest(
    val messages: List<ChatMessage>,
    val model: ChatModel,
    val frequency_penalty: Double? = null,
    val max_tokens: Int? = null,
    val presence_penalty: Double? = null,
    val response_format: ResponseFormat? = null,
    val stop: StopReason? = null,
    val stream: Boolean? = false,
    val stream_options: StreamOptions? = null,
    val temperature: Double? = null,
    val top_p: Double? = null,
    val tools: List<Tool>? = null,
    val tool_choice: ToolChoice? = null,
    val logprobs: Boolean? = null,
    val top_logprobs: Int? = null
) {
    class Builder {
        private var messages = mutableListOf<ChatMessage>()
        private var params: ChatCompletionParams = ChatCompletionParams(
            model = ChatModel.DEEPSEEK_CHAT,
        )

        fun messages(block: MessageBuilder.() -> Unit) {
            messages.addAll(MessageBuilder().apply(block).build())
        }

        fun messages(list: List<ChatMessage>) {
            messages.addAll(list)
        }

        fun params(block: ChatCompletionParams.Builder.() -> Unit) {
            params = ChatCompletionParams.Builder().apply(block).build()
        }

        internal fun build(): ChatCompletionRequest =
            params.createRequest(messages)
    }

    class StreamBuilder {
        private var messages = mutableListOf<ChatMessage>()
        private var params: ChatCompletionParams = ChatCompletionParams(
            model = ChatModel.DEEPSEEK_CHAT,
            stream = true
        )
        fun messages(list: List<ChatMessage>) {
            messages.addAll(list)
        }
        fun messages(block: MessageBuilder.() -> Unit) {
            messages.addAll(MessageBuilder().apply(block).build())
        }

        fun params(block: ChatCompletionParams.StreamBuilder.() -> Unit) {
            params = ChatCompletionParams.StreamBuilder().apply(block).build()
        }

        internal fun build(): ChatCompletionRequest =
            params.createRequest(messages)
    }

    class MessageBuilder {
        private val messages = mutableListOf<ChatMessage>()
        fun messages(list: List<ChatMessage>) {
            messages.addAll(list)
        }
        fun system(content: String) {
            messages.add(SystemMessage(content))
        }

        fun user(content: String) {
            messages.add(UserMessage(content))
        }

        fun assistant(content: String) {
            messages.add(AssistantMessage(content))
        }

        fun tool(content: String, toolCallId: String) {
            messages.add(ToolMessage(content, toolCallId))
        }

        internal fun build(): List<ChatMessage> = messages.toList()
    }
}