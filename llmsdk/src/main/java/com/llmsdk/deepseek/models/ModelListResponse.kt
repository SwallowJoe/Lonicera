package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ModelListResponse(
    val data: List<Model>,
    val `object`: String = "list"
)