package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
class ResponseFormat private constructor(val type: String) {
    companion object {
        val TEXT: ResponseFormat = ResponseFormat("text")
        val JSON: ResponseFormat = ResponseFormat("json_object")
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ResponseFormat) return false
        return type == other.type
    }

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun toString(): String = "ResponseFormat(type='$type')"
}