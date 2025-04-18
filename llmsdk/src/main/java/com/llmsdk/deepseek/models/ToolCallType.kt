package com.llmsdk.deepseek.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ToolCallType {
    @SerialName("function")
    FUNCTION,
}