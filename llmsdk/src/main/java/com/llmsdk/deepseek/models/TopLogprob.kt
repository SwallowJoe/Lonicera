package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class TopLogprob(
    val token: String,
    val logprob: Double,
    val bytes: List<Int>
)