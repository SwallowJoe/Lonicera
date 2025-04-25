package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class StreamOptions(
    val include_usage: Boolean
)