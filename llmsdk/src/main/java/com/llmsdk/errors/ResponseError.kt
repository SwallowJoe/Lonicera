package com.llmsdk.errors

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
class ResponseError(
    val error: Error
) {
    @Serializable
    class Error(
        val message: String? = null,
        val type: String? = null,
        val param: JsonObject? = null,
        val code: String? = null,
    ) {
        override fun toString(): String = "Error(message=$message, type=$type, param=$param, code=$code)"
    }

    override fun toString(): String = "ResponseError(error=$error)"
}