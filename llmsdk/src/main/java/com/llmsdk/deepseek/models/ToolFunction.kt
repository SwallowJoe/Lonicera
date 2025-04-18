package com.llmsdk.deepseek.models

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

// 自定义的序列化器，用于解析包装在字符串中的 JSON 对象
object FunctionCallArgumentsSerializer : KSerializer<JsonObject> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("FunctionCallArguments", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): JsonObject {
        // 先获取字符串
        val jsonString = decoder.decodeString()
        // 将字符串解析成 JsonObject
        return Json.decodeFromString(JsonObject.serializer(), jsonString)
    }

    override fun serialize(encoder: Encoder, value: JsonObject) {
        // 将 JsonObject 序列化成字符串再编码
        val jsonString = Json.encodeToString(JsonObject.serializer(), value)
        encoder.encodeString(jsonString)
    }
}

@Serializable(with = ToolFunctionSerializer::class)
sealed interface ToolFunction {
    val name: String
}

@Serializable
class FunctionRequest(
    override val name: String,
    val description: String?,
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
    override val name: String,
    @Serializable(with = FunctionCallArgumentsSerializer::class)
    val arguments: JsonObject?,
) : ToolFunction {
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

internal object ToolFunctionSerializer : JsonContentPolymorphicSerializer<ToolFunction>(ToolFunction::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ToolFunction> {
        return when {
            "parameters" in element.jsonObject -> FunctionRequest.serializer()
            "arguments" in element.jsonObject -> FunctionResponse.serializer()
            else -> throw Exception("Unknown ToolFunction type")
        }
    }
}