package com.llmsdk.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ChatModel(
    val nickName: String,
    val provider: String
) {
    @SerialName("deepseek-chat")
    DEEPSEEK_CHAT("DeepSeek V3", "DeepSeek"),

    @SerialName("deepseek-reasoner")
    DEEPSEEK_REASONER("DeepSeek R1", "DeepSeek"),
}

fun ChatModel.isSupportStream(): Boolean {
    return when (this) {
        ChatModel.DEEPSEEK_CHAT -> true
        ChatModel.DEEPSEEK_REASONER -> true
    }
}

fun ChatModel.isSupportFunctionCall(): Boolean {
    return when (this) {
        ChatModel.DEEPSEEK_CHAT -> true
        ChatModel.DEEPSEEK_REASONER -> false
    }
}

fun ChatModel.isSupportTemperature(): Boolean {
    return when (this) {
        ChatModel.DEEPSEEK_CHAT -> true
        ChatModel.DEEPSEEK_REASONER -> false
    }
}

fun ChatModel.isSupportTopP(): Boolean {
    return when (this) {
        ChatModel.DEEPSEEK_CHAT -> true
        ChatModel.DEEPSEEK_REASONER -> false
    }
}

fun ChatModel.isSupportFrequencyPenalty(): Boolean {
    return when (this) {
        ChatModel.DEEPSEEK_CHAT -> true
        ChatModel.DEEPSEEK_REASONER -> false
    }
}

fun ChatModel.isSupportPresencePenalty(): Boolean {
    return when (this) {
        ChatModel.DEEPSEEK_CHAT -> true
        ChatModel.DEEPSEEK_REASONER -> false
    }
}

fun ChatModel.isSupportLogprobs(): Boolean {
    return when (this) {
        ChatModel.DEEPSEEK_CHAT -> true
        ChatModel.DEEPSEEK_REASONER -> false
    }
}

fun ChatModel.isSupportTopLogprobs(): Boolean {
    return when (this) {
        ChatModel.DEEPSEEK_CHAT -> true
        ChatModel.DEEPSEEK_REASONER -> false
    }
}