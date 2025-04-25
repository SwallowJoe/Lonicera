package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class CompletionTokensDetails(
    val reasoning_tokens: Int
)