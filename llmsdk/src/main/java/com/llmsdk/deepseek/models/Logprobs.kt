package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class Logprobs(
    val content: List<LogprobContent>
)