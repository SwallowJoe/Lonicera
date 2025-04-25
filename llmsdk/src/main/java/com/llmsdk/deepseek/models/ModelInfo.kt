package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo(
    val id: String,       // 如 "deepseek-chat", "deepseek-reasoner"
    val `object`: ModelObjectType,   // 固定值 "model"
    val owned_by: String  // 所属方标识
) {
    override fun toString(): String {
        return "ModelInfo(id='$id', object=$`object`, owned_by='$owned_by')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ModelInfo) return false
        return id == other.id && `object` == other.`object` && owned_by == other.owned_by
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + `object`.hashCode()
        result = 31 * result + owned_by.hashCode()
        return result
    }
}