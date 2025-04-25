package com.llmsdk.deepseek.models

open class DeepSeekParams internal constructor(
    val frequency_penalty: Double? = null,
    val max_tokens: Int? = null,
    val presence_penalty: Double? = null,
    val stop: StopReason? = null,
    val temperature: Double? = null,
    val top_p: Double? = null,
) {

    /**
     * Creates chat completion parameters with custom settings.
     *
     * This function provides a convenient way to create parameters specifically
     * for chat completion requests using a builder pattern.
     *
     * Example:
     * ```kotlin
     * params {
     *     chat {
     *         model = ChatModel.DEEPSEEK_CHAT
     *         temperature = 0.8
     *         maxTokens = 2000
     *    }
     * }
     * ```
     *
     * @param block Configuration block for building chat parameters
     * @return Configured [ChatCompletionParams] for use with chat endpoints
     */
    fun chat(block: ChatCompletionParams.Builder.() -> Unit): ChatCompletionParams =
        chatCompletionParams(block)

    fun chatStream(block: ChatCompletionParams.StreamBuilder.() -> Unit): ChatCompletionParams =
        chatCompletionStreamParams(block)

    // TODO:
    // fun fim(block: FIMCompletionParams.Builder.() -> Unit): FIMCompletionParams = fimCompletionParams(block)

    // TODO:
    // fun fimStream(block: FIMCompletionParams.StreamBuilder.() -> Unit): FIMCompletionParams = fimCompletionStreamParams(block)
}