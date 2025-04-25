package com.llmsdk.base

data class Model(
    val name: String,
    val description: String,
    val temperature: Double,
    val maxTokens: Int,
    val topP: Double,
    val frequencyPenalty: Double,
    val presencePenalty: Double,
    val stop: List<String>?,
)