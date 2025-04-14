package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class Usage(
    val completion_tokens: Int,
    val prompt_tokens: Int,
    val prompt_cache_hit_tokens: Int,
    val prompt_cache_miss_tokens: Int,
    val total_tokens: Int,
    val completion_tokens_details: CompletionTokensDetails? = null
)