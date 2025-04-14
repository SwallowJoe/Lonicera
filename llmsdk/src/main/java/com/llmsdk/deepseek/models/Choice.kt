package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class Choice(
    val finish_reason: String,
    val index: Int,
    val message: Message,
    val logprobs: Logprobs?
)