package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ToolCall(
    val id: String,
    val type: String,
    val function: Function
)