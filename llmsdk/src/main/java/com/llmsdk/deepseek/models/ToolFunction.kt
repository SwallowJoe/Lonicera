package com.llmsdk.deepseek.models

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface ToolFunction : java.io.Serializable {
    val name: String?
}

@Serializable
class FunctionRequest(
    override val name: String?,
    val description: String?,
    @Serializable(with = JsonObjectSerializer::class)
    val parameters: JsonObject?,
) : ToolFunction {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionRequest) return false
        return name == other.name && description == other.description && parameters == other.parameters
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (description?.hashCode() ?: 0)
        result = 31 * result + (parameters?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FunctionRequest(name='$name', description=$description, parameters=$parameters)"
}

@Serializable
class FunctionResponse(
    override var name: String? = null,
    var arguments: String? = null,
) : ToolFunction {

    fun copy(name: String? = this.name, arguments: String? = this.arguments): FunctionResponse {
        return FunctionResponse(name, arguments)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionResponse) return false
        return name == other.name && arguments == other.arguments
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (arguments?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        "FunctionResponse(name='$name', arguments=$arguments)"
}

internal object JsonObjectSerializer : KSerializer<JsonObject?> {
    // 使用内置 JsonObject 的 descriptor
    override val descriptor: SerialDescriptor = JsonObject.serializer().descriptor

    override fun serialize(encoder: Encoder, value: JsonObject?) {
        require(encoder is JsonEncoder)
        // 支持 null，也支持正常对象
        encoder.encodeSerializableValue(JsonObject.serializer().nullable, value)
    }

    override fun deserialize(decoder: Decoder): JsonObject? {
        require(decoder is JsonDecoder)
        return when (val element = decoder.decodeJsonElement()) {
            is JsonObject -> {
                // 正常对象
                decoder.json.decodeFromJsonElement(JsonObject.serializer(), element)
            }
            is JsonNull -> null
            else -> {
                // 其它类型（如字符串、数字等）一律当作 null 处理
                null
            }
        }
    }
}