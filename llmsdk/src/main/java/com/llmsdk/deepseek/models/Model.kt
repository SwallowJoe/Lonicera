package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class Model(
    val id: String,       // 如 "deepseek-chat", "deepseek-reasoner"
    val `object`: String,   // 固定值 "model"
    val owned_by: String  // 所属方标识
)