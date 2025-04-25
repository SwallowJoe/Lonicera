package com.llmsdk.tools

import com.llmsdk.deepseek.models.FunctionRequest
import com.llmsdk.deepseek.models.Tool
import com.llmsdk.deepseek.models.ToolCallType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun io.modelcontextprotocol.kotlin.sdk.Tool.toTool(): Tool {
    val schema = inputSchema
    val props = JsonObject(schema.properties)
    val requiredArray = schema.required
        .takeIf { !it.isNullOrEmpty() }
        ?.let { req -> JsonArray(req.map { JsonPrimitive(it) }) }

    val parameters = buildJsonObject {
        put("type", schema.type)
        put("properties", props)
        requiredArray?.let { put("required", it) }
    }

    return Tool(
        type = ToolCallType.FUNCTION,
        function = FunctionRequest(
            name = name,
            description = description,
            parameters = parameters
        )
    )
}