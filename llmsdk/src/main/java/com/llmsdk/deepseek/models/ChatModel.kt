package com.llmsdk.deepseek.models

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