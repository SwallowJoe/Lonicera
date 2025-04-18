package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ModelListResponse(
    val `object`: String,
    val data: List<ModelInfo>,
) {
    override fun toString(): String {
        return "ModelListResponse(object=$`object`, data=$data)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelListResponse) return false
        return `object` == other.`object` && data == other.data
    }

    override fun hashCode(): Int {
        var result = `object`.hashCode()
        result = 31 * result + data.hashCode()
        return result
    }
}