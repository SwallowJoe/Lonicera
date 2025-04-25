package com.llmsdk.deepseek.models

import kotlinx.serialization.Serializable

@Serializable
data class ChatCompletionResponseChunk(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val system_fingerprint: String,
    val choices: List<ChatChoiceChunk>,
    val usage: Usage? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ChatCompletionResponseChunk) return false

        return id == other.id &&
                choices == other.choices &&
                created == other.created &&
                model == other.model &&
                system_fingerprint == other.system_fingerprint &&
                `object` == other.`object` &&
                usage == other.usage
    }

    override fun hashCode(): Int {
        var result = created.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + choices.hashCode()
        result = 31 * result + model.hashCode()
        result = 31 * result + system_fingerprint.hashCode()
        result = 31 * result + `object`.hashCode()
        result = 31 * result + (usage?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "ChatCompletionResponseChunk(id='$id', choices=$choices, created=$created, model='$model', systemFingerprint='$system_fingerprint', `object`='$`object`', usage=$usage)"
}