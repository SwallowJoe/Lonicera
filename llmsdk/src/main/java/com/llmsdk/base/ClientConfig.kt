package com.llmsdk.base

import kotlinx.serialization.json.Json

data class ClientConfig(
    val params: Params? = null,
    val jsonConfig: Json = Json,
)