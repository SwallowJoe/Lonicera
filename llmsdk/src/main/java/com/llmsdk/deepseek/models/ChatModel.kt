package com.llmsdk.deepseek.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ChatModel {
    @SerialName("deepseek-chat")
    DEEPSEEK_CHAT,

    @SerialName("deepseek-reasoner")
    DEEPSEEK_REASONER,
}