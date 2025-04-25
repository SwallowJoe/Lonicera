package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class LogprobContent(
    val token: String,
    val logprob: Double,
    val bytes: List<Int>,
    val top_logprobs: List<TopLogprob>?
)