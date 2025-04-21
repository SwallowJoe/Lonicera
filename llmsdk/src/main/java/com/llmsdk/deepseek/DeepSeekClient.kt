package com.llmsdk.deepseek

import com.llmsdk.deepseek.errors.validateResponse
import com.llmsdk.deepseek.models.BalanceResponse
import com.llmsdk.deepseek.models.ChatCompletionRequest
import com.llmsdk.deepseek.models.ChatCompletionResponse
import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.ChatMessageModule
import com.llmsdk.deepseek.models.ModelListResponse
import com.llmsdk.tools.ToolManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json


class DeepSeekClient {
    companion object {
        const val BASE_URL = "https://api.deepseek.com/v1"
    }

    private val client = HttpClient(Android) {
        install(SSE)
        install(HttpTimeout) {
            requestTimeoutMillis = 50000
            connectTimeoutMillis = 50000
            socketTimeoutMillis = 15000L
        }

        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                allowStructuredMapKeys = true
                allowSpecialFloatingPointValues = true
                serializersModule = ChatMessageModule
            })
        }
    }

    // 基础请求方法
    suspend fun chatCompletion(
        config: DeepSeekConfig,
        messages: List<ChatMessage>,
    ): ChatCompletionResponse {
        return executeRequest {
            url("$BASE_URL/chat/completions")
            method = HttpMethod.Post
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                ChatCompletionRequest(
                    model = config.model,
                    messages = messages,
                    temperature = config.temperature,
                    max_tokens = config.max_tokens,
                    top_p = config.top_p,
                    frequency_penalty = config.frequency_penalty,
                    presence_penalty = config.presence_penalty,
                    stop = config.stop,
                    stream = config.stream,
                    stream_options = config.stream_options,
                    response_format = config.response_format,
                    top_logprobs = config.top_logprobs,
                    logprobs = config.logprobs,
                    tools = ToolManager.availableTools(), // TODO:
                    tool_choice = config.tool_choice,
                )
            )
        }
    }

    suspend fun getSupportedModels(config: DeepSeekConfig): ModelListResponse {
        return executeRequest {
            url("$BASE_URL/models")
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            method = HttpMethod.Get
        }
    }

    suspend fun getBalance(config: DeepSeekConfig): BalanceResponse {
        return executeRequest {
            url("$BASE_URL/user/balance")
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            method = HttpMethod.Get
        }
    }

    // 关闭客户端（可选）
    fun close() {
        client.close()
    }

    private suspend inline fun <reified T> executeRequest(block: HttpRequestBuilder.() -> Unit): T {
        val response = client.request(block)
        validateResponse(response)
        return response.body()
    }
}