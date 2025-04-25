package com.llmsdk.tools.functions

import com.llmsdk.deepseek.models.FunctionRequest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

@Serializable
data class Parameters(
    val type: String,
    val properties: Properties,
    val required: List<String>
)

@Serializable
data class Properties(
    val location: Location
)

@Serializable
data class Location(
    val type: String,
    val description: String
)

@Serializable
data class GetWeather(
    val name: String,
    val description: String,
    val parameters: Parameters
) {

    companion object {
        fun parse(json: String): GetWeather {
            return kotlinx.serialization.json.Json.decodeFromString(json)
        }
    }

    fun toJson(): String {
        return kotlinx.serialization.json.Json.encodeToString(this)
    }
}

fun getWeatherFunctionRequest(): FunctionRequest {
    val parameters = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("location") {
                put("type", "string")
                put("description", "The city and state, e.g. San Francisco, CA")
            }
        }
        putJsonArray("required") {
            add("location")
        }
    }

    return FunctionRequest(
        name = "get_weather",
        description = "Get weather of an location, the user should supply a location first",
        parameters = parameters
    )
}