package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class Function(
    val name: String,
    val arguments: String
)