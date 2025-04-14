package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatResponse(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    val system_fingerprint: String,
    val `object`: String,
    val usage: Usage
)