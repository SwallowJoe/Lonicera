package com.llmsdk.deepseek

import com.llmsdk.deepseek.errors.DeepSeekException
import com.llmsdk.deepseek.errors.validateResponse
import com.llmsdk.deepseek.models.BalanceResponse
import com.llmsdk.deepseek.models.ChatCompletionParams
import com.llmsdk.deepseek.models.ChatCompletionRequest
import com.llmsdk.deepseek.models.ChatCompletionResponse
import com.llmsdk.deepseek.models.ChatCompletionResponseChunk
import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.ChatMessageModule
import com.llmsdk.base.ChatModel
import com.llmsdk.base.isSupportFrequencyPenalty
import com.llmsdk.base.isSupportFunctionCall
import com.llmsdk.base.isSupportLogprobs
import com.llmsdk.base.isSupportPresencePenalty
import com.llmsdk.base.isSupportTemperature
import com.llmsdk.base.isSupportTopLogprobs
import com.llmsdk.base.isSupportTopP
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.ChatChoiceChunk
import com.llmsdk.deepseek.models.DeepSeekParams
import com.llmsdk.deepseek.models.FinishReason
import com.llmsdk.deepseek.models.ModelListResponse
import com.llmsdk.deepseek.models.UserMessage
import com.llmsdk.errors.ResponseError
import com.llmsdk.log.ALog
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.plugins.sse.SSEClientException
import io.ktor.client.plugins.sse.sse
import io.ktor.client.plugins.timeout
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlin.random.Random

data class DeepSeekClientConfig (
    private val token: String,
    val params: DeepSeekParams? = null,
    val jsonConfig: Json = Json,
    val chatCompletionTimeout: Long = 45_000,
    val fimCompletionTimeout: Long = 60_000
) {
    fun isSameToken(other: DeepSeekClientConfig): Boolean {
        return this.token == other.token
    }

    fun isSameToken(token: String): Boolean {
        return this.token == token
    }
}

abstract class DeepSeekBaseClient(
    val client: HttpClient, val config: DeepSeekClientConfig
): AutoCloseable {
    companion object {
        private const val TAG = "DeepSeekClient"
    }

    fun isSameToken(token: String): Boolean {
        return this.config.isSameToken(token)
    }

    /**
     * 根据请求参数，生成一个经过验证的请求
     * 根据DeepSeek官网要求：
     * 1. DeepSeek R1 推理模型暂不支持Function Call:
     *      https://api-docs.deepseek.com/zh-cn/guides/reasoning_model
     * 2. 推理模型的reasoning_content
     *     a. 不支持的功能：Function Call、Json Output、FIM 补全 (Beta)
     *     b. 不支持的参数：temperature、top_p、presence_penalty、frequency_penalty、logprobs、top_logprobs。
     *     请注意，为了兼容已有软件，设置 temperature、top_p、presence_penalty、frequency_penalty 参数不会报错，但也不会生效。
     *     设置 logprobs、top_logprobs 会报错
     * @return validated request
     */
    private fun ChatCompletionRequest.toValidatedRequest(): ChatCompletionRequest {
        return copy(
            messages = messages.map {
                if (it is AssistantMessage) {
                    // 注意对于 AssistantMessage，需要去除 reasoning_content
                    // refer: https://api-docs.deepseek.com/zh-cn/guides/reasoning_model
                    it.copy(
                        reasoning_content = null,
                        tool_calls = if (model.isSupportFunctionCall()) it.tool_calls else null
                    )
                } else it
            },
            temperature = if (model.isSupportTemperature()) temperature else null,
            top_p = if (model.isSupportTopP()) top_p else null,
            presence_penalty = if (model.isSupportPresencePenalty()) presence_penalty else null,
            frequency_penalty = if (model.isSupportFrequencyPenalty()) frequency_penalty else null,
            logprobs = if (model.isSupportLogprobs()) logprobs else null,
            top_logprobs = if (model.isSupportTopLogprobs()) top_logprobs else null,
            tools = if (model.isSupportFunctionCall()) tools else null,
            tool_choice = if (model.isSupportFunctionCall()) tool_choice else null
        )
    }

    suspend fun chatCompletion(request: ChatCompletionRequest): ChatCompletionResponse {
        val validatedRequest = request.toValidatedRequest()
        ALog.i(TAG, "chatCompletion request: ${config.jsonConfig.encodeToString(validatedRequest)}")

        val response = client.post(urlString = "https://api.deepseek.com/v1/chat/completions") {
            timeout { requestTimeoutMillis = config.chatCompletionTimeout }
            setBody(validatedRequest)
        }
        validateResponse(response)
        return response.body()
    }

    suspend fun chatCompletionStream(request: ChatCompletionRequest): Flow<ChatCompletionResponseChunk> {
        return flow {
            try {
                val validatedRequest = request.toValidatedRequest()
                ALog.i(
                    TAG,
                    "chatCompletionStream request: ${
                        config.jsonConfig.encodeToString(validatedRequest)
                    }"
                )

                client.sse(
                    urlString = "https://api.deepseek.com/v1/chat/completions",
                    request = {
                        method = HttpMethod.Post
                        accept(ContentType.Text.EventStream)
                        headers {
                            append(HttpHeaders.CacheControl, "no-cache")
                            append(HttpHeaders.Connection, "keep-alive")
                        }
                        setBody(validatedRequest)
                        timeout { requestTimeoutMillis = config.chatCompletionTimeout }
                    }
                ) {
                    try {
                        // ALog.i(TAG, "chatCompletionStream started ${config.hashCode()}")
                        incoming.collect { event ->
                            event.data?.trim()?.takeIf { it != "[DONE]" }?.let { data ->
                                ALog.i(TAG, "chatCompletionStream collect: $data")
                                val chatChunk =
                                    config.jsonConfig.decodeFromString<ChatCompletionResponseChunk>(
                                        data
                                    )
                                emit(chatChunk)
                            }
                        }
                    } finally {
                        ALog.i(TAG, "chatCompletionStream completed")
                    }
                }
            } catch (e: SSEClientException) {
                ALog.e(TAG, "chatCompletionStream SSEClientException: ${e.message}")
                // emit(exceptionChatCompletionResponseChunk(content = "${e.message}, ${e.response?.status}"))
            } catch (e: SerializationException) {
                ALog.e(TAG, "chatCompletionStream SerializationException: ${e.message}")
                // emit(exceptionChatCompletionResponseChunk(content = "${e.message}"))
            } catch (e: Exception) {
                ALog.e(TAG, "chatCompletionStream error: ${e.message}")
                // emit(exceptionChatCompletionResponseChunk(content = "${e.message}, stack: ${e.stackTrace.joinToString()}"))
            }
        }.flowOn(Dispatchers.IO)
    }

    private fun exceptionChatCompletionResponseChunk(content: String): ChatCompletionResponseChunk {
        return ChatCompletionResponseChunk(
            id = "",
            `object` = "",
            created = 0,
            model = "",
            system_fingerprint = "",
            choices = listOf(
                ChatChoiceChunk(
                    index = 0,
                    finish_reason = FinishReason.STOP,
                    delta = AssistantMessage(
                        content = content,
                        name = null,
                        prefix = null,
                        reasoning_content = null,
                        tool_calls = null
                    ),
                    logprobs = null
                )
            ),
            usage = null
        )
    }

    suspend fun getBalance(): BalanceResponse {
        val response = client.get(urlString = "https://api.deepseek.com/v1/user/balance")
        validateResponse(response)
        return response.body()
    }

    suspend fun getSupportedModels(): ModelListResponse {
        val response = client.get(urlString = "https://api.deepseek.com/v1/models")
        validateResponse(response)
        return response.body()
    }

    override fun close() {
        client.close()
    }

    abstract class Builder(val token: String) {
        protected var baseUrl: String = "https://api.deepseek.com"

        protected var jsonConfig: Json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            allowStructuredMapKeys = true
            allowSpecialFloatingPointValues = true
            serializersModule = ChatMessageModule
        }

        protected var chatCompletionTimeout: Long = 45_000
        protected var fimCompletionTimeout: Long = 60_000
        protected open var client: HttpClient = HttpClient(Android) {
            install(Auth) {
                if (token.isEmpty()) return@install
                bearer {
                    loadTokens {
                        BearerTokens(token, "")
                    }
                }
            }

            install(ContentNegotiation) {
                json(jsonConfig)
            }

            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
            install(HttpRequestRetry) {
                maxRetries = 3
                retryIf { _, httpResponse -> !httpResponse.status.isSuccess()}
                delayMillis { retry: Int ->
                    val delay = (retry * 0.2).toLong().coerceAtLeast(1L)
                    retry + Random.nextLong(delay)
                }
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 300_000 // 300s
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
                // 忽略 Authorization 头
                sanitizeHeader { header -> header.lowercase() == "authorization" }
            }
        }
        protected var params: DeepSeekParams? = null
        fun baseUrl(url: String): Builder {
            baseUrl = url
            return this
        }
        fun params(block: DeepSeekParams.() -> DeepSeekParams): Builder {
            params = DeepSeekParams().block()
            return this
        }
        fun jsonConfig(block: Json.() -> Unit): Builder {
            jsonConfig = jsonConfig.apply(block)
            client.config {
                install(ContentNegotiation) {
                    json(jsonConfig)
                }
            }
            return this
        }
        fun jsonConfig(jsonConfig: Json): Builder {
            this.jsonConfig = jsonConfig
            client.config {
                install(ContentNegotiation) {
                    json(jsonConfig)
                }
            }
            return this
        }
        fun chatCompletionTimeout(timeout: Long): Builder {
            chatCompletionTimeout = timeout
            return this
        }
        fun fimCompletionTimeout(timeout: Long): Builder {
            fimCompletionTimeout = timeout
            return this
        }
        fun httpClient(client: HttpClient): Builder {
            this.client = client
            return this
        }
        fun httpClientConfig(block: HttpClientConfig<*>.() -> Unit): Builder {
            client = HttpClient(Android) {
                block(this)
            }
            return this
        }
    }
}

class DeepSeekClient internal constructor(
    client: HttpClient, config: DeepSeekClientConfig
) : DeepSeekBaseClient(client, config) {
    class Builder(token: String) : DeepSeekBaseClient.Builder(token) {
        internal fun build(): DeepSeekClient {
            return DeepSeekClient(
                client = client,
                config = DeepSeekClientConfig(
                    token = token,
                    params = params,
                    jsonConfig = jsonConfig,
                    chatCompletionTimeout = chatCompletionTimeout,
                    fimCompletionTimeout = fimCompletionTimeout
                )
            )
        }
    }

    suspend fun chat(params: ChatCompletionParams, messages: List<ChatMessage>): ChatCompletionResponse {
        val request = (if (params.stream == true)
                           params.copy(stream = false)
                       else
                           params
                      ).createRequest(messages)
        return chatCompletion(request)
    }

    suspend fun chat(messages: List<ChatMessage>): ChatCompletionResponse {
        val params =
            config.params as? ChatCompletionParams ?: ChatCompletionParams(ChatModel.DEEPSEEK_CHAT)
        return chat(params, messages)
    }

    suspend fun chat(message: String): ChatCompletionResponse {
        val params =
            config.params as? ChatCompletionParams ?: ChatCompletionParams(ChatModel.DEEPSEEK_CHAT)
        return chat(params, listOf(UserMessage(content = message)))
    }

    suspend fun chat(blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit
    ): ChatCompletionResponse = chat(ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

    suspend fun chat(params: ChatCompletionParams,
                     blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit
    ): ChatCompletionResponse = chat(params, ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

    suspend fun chatCompletion(block: ChatCompletionRequest.Builder.() -> Unit): ChatCompletionResponse {
        val request = ChatCompletionRequest.Builder().apply(block).build()
        return chatCompletion(request)
    }
}

class DeepSeekClientStream internal constructor(
    client: HttpClient,
    config: DeepSeekClientConfig
): DeepSeekBaseClient(client, config) {
    class Builder(token: String) : DeepSeekBaseClient.Builder(token) {
        override var client: HttpClient = HttpClient(Android) {
            install(Auth) {
                if (token.isEmpty()) return@install
                bearer {
                    loadTokens {
                        BearerTokens(token, "")
                    }
                }
            }
            install(SSE)

            install(ContentNegotiation) {
                json(jsonConfig)
            }

            defaultRequest {
                url(baseUrl)
                contentType(ContentType.Application.Json)
            }
            install(HttpRequestRetry) {
                maxRetries = 3
                retryIf { _, httpResponse -> !httpResponse.status.isSuccess()}
                delayMillis { retry: Int ->
                    val delay = (retry * 0.2).toLong().coerceAtLeast(1L)
                    retry + Random.nextLong(delay)
                }
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 300_000 // 300s
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
                // 忽略 Authorization 头
                sanitizeHeader { header -> header.lowercase() == "authorization" }
            }
        }
        internal fun build(): DeepSeekClientStream {
            return DeepSeekClientStream(
                client = client,
                config = DeepSeekClientConfig(
                    token = token,
                    params = params,
                    jsonConfig = jsonConfig,
                    chatCompletionTimeout = chatCompletionTimeout,
                    fimCompletionTimeout = fimCompletionTimeout
                )
            )
        }
    }

    suspend fun chat(
        params: ChatCompletionParams,
        messages: List<ChatMessage>
    ): Flow<ChatCompletionResponseChunk> {
        val request = (if (params.stream == null || !params.stream) params.copy(stream = true) else params).createRequest(messages)
        return chatCompletionStream(request)
    }

    suspend fun chat(messages: List<ChatMessage>): Flow<ChatCompletionResponseChunk> {
        val params = config.params as? ChatCompletionParams ?: ChatCompletionParams(
            model = ChatModel.DEEPSEEK_CHAT,
            stream = true
        )
        return chat(params, messages)
    }

    suspend fun chat(message: String): Flow<ChatCompletionResponseChunk> {
        return chat(listOf(UserMessage(content = message)))
    }

    suspend fun chat(
        params: ChatCompletionParams,
        blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit,
    ): Flow<ChatCompletionResponseChunk> =
        chat(params, ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

    suspend fun chat(
        blockMessage: ChatCompletionRequest.MessageBuilder.() -> Unit
    ): Flow<ChatCompletionResponseChunk> =
        chat(ChatCompletionRequest.MessageBuilder().apply(blockMessage).build())

    suspend fun chatCompletion(
        block: ChatCompletionRequest.StreamBuilder.() -> Unit
    ): Flow<ChatCompletionResponseChunk> {
        val request = ChatCompletionRequest.StreamBuilder().apply(block).build()
        return chatCompletionStream(request)
    }
}


fun DeepSeekClient(token: String, block: DeepSeekClient.Builder.() -> Unit = {}): DeepSeekClient
        = DeepSeekClient.Builder(token).apply(block).build()


fun DeepSeekClientStream(token: String, block: DeepSeekClientStream.Builder.() -> Unit = {}): DeepSeekClientStream
        = DeepSeekClientStream.Builder(token).apply(block).build()

/*
class DeepSeekClient(
    val httpConnectionTimeout: Long = 15_000L,      // 连接超时时间, 默认15秒
    val httpSocketTimeout: Long = 120_000L,         // socket读写超时时间, 默认120秒
    val httpRequestTimeout: Long = 240_000L,        // 请求超时时间, 默认240秒
): Closeable {
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
        config: ChatConfig,
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

    suspend fun getSupportedModels(config: ChatConfig): ModelListResponse {
        return executeRequest {
            url("$BASE_URL/models")
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            method = HttpMethod.Get
        }
    }

    suspend fun getBalance(config: ChatConfig): BalanceResponse {
        return executeRequest {
            url("$BASE_URL/user/balance")
            header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
            header(HttpHeaders.Accept, ContentType.Application.Json)
            method = HttpMethod.Get
        }
    }

    suspend fun chatCompletionStream(
        config: ChatConfig,
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
    override fun close() {
        // TODO: add chat job cancel.
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
            ALog.i(TAG, "streamRequest sse $urlString")
            client.sse(
                urlString = urlString,
                request = {
                    block()
                }
            ) {
                ALog.i(TAG, "streamRequest incoming collect event")
                incoming.collect { event ->
                    // event.data 可能为 null，或者是终止标志 "[DONE]"
                    ALog.i(TAG, "streamRequest event: $event")
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

                            ALog.i(TAG, "streamRequest emit: $chatChunk")
                            emit(chatChunk)
                        }
                }
            }
        } catch (e: SSEClientException) {
            ALog.e(TAG, "streamRequest sse $urlString error: $e")
            throw e
        }
        ALog.i(TAG, "streamRequest sse $urlString end")
    }.flowOn(Dispatchers.IO)
}*/