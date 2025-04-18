package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionResponse(
    val id: String,
    val choices: List<ChatChoice>,
    val created: Long,
    val model: String,
    val system_fingerprint: String,
    val `object`: String,
    val usage: Usage
)