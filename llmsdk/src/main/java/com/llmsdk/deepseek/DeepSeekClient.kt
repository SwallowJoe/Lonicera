package com.llmsdk.deepseek

import android.util.Log
import com.llmsdk.deepseek.errors.DeepSeekException
import com.llmsdk.deepseek.errors.validateResponse
import com.llmsdk.deepseek.models.BalanceResponse
import com.llmsdk.deepseek.models.ChatCompletionRequest
import com.llmsdk.deepseek.models.ChatCompletionResponse
import com.llmsdk.deepseek.models.ChatCompletionResponseChunk
import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.ChatMessageModule
import com.llmsdk.deepseek.models.ModelListResponse
import com.llmsdk.errors.ResponseError
import com.llmsdk.tools.ToolManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.SSEClientContent
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.plugins.sse.sse
import io.ktor.client.plugins.sse.sseSession
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json


class DeepSeekClient(
    val httpConnectionTimeout: Long = 15_000L,      // 连接超时时间, 默认15秒
    val httpSocketTimeout: Long = 120_000L,         // socket读写超时时间, 默认120秒
    val httpRequestTimeout: Long = 240_000L,        // 请求超时时间, 默认240秒
) {
    companion object {
        private const val TAG = "DeepSeekClient"
        const val BASE_URL = "https://api.deepseek.com/v1"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowStructuredMapKeys = true
        allowSpecialFloatingPointValues = true
        serializersModule = ChatMessageModule
    }

    private val client = HttpClient(Android) {
        install(SSE)
        install(HttpTimeout) {
            connectTimeoutMillis = httpConnectionTimeout
            socketTimeoutMillis = httpSocketTimeout
            requestTimeoutMillis = httpRequestTimeout
        }

        install(ContentNegotiation) {
            json(json)
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
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.Accept, ContentType.Application.Json)

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

    suspend fun chatCompletionStream(
        config: DeepSeekConfig,
        messages: List<ChatMessage>,
    ): Flow<ChatCompletionResponseChunk> = streamRequest(
        urlString = "$BASE_URL/chat/completions",
        block = {
            method = HttpMethod.Post
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            header(HttpHeaders.CacheControl, "no-cache")
            header(HttpHeaders.Connection, "keep-alive")
            accept(ContentType.Text.EventStream)
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
    )

    // 关闭客户端（可选）
    fun close() {
        client.close()
    }

    private suspend inline fun <reified T> executeRequest(block: HttpRequestBuilder.() -> Unit): T {
        val response = client.request(block)
        validateResponse(response)
        return response.body()
    }

    private suspend inline fun <reified T> streamRequest(
        urlString: String,
        noinline block: HttpRequestBuilder.() -> Unit
    ): Flow<T> = flow {
        try {
            Log.i(TAG, "streamRequest sse $urlString")
            client.sse(
                urlString = urlString,
                request = block
            ) {
                Log.i(TAG, "streamRequest incoming collect event")
                incoming.collect { event ->
                    // event.data 可能为 null，或者是终止标志 "[DONE]"
                    Log.i(TAG, "streamRequest event: $event")
                    event.data
                        ?.trim()
                        ?.takeIf { it.isNotEmpty() && it != "[DONE]" }
                        ?.let { data ->
                            // 反序列化 JSON 为 T
                            val chatChunk: T = try {
                                json.decodeFromString(data)
                            } catch (e: SerializationException) {
                                // 根据业务需要，选择忽略或抛出
                                throw DeepSeekException.from(
                                    statusCode = 999,
                                    headers = Headers.Empty,
                                    error = ResponseError(
                                        ResponseError.Error(
                                            message = "Failed to parse SSE data: $data",
                                            type = "parse_error",
                                            param = null,
                                            code = null
                                        )
                                    )
                                )
                            }

                            Log.i(TAG, "streamRequest emit: $chatChunk")
                            emit(chatChunk)
                        }
                }
            }
        } catch (e: SSEClientException) {
            Log.e(TAG, "streamRequest sse $urlString error: $e")
            throw e
        }
        Log.i(TAG, "streamRequest sse $urlString end")
    }.flowOn(Dispatchers.IO)
}