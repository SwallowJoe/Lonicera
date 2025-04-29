package com.android.lonicera.components.chat

import com.android.lonicera.components.chat.model.ChatUIMessage
import com.android.lonicera.db.DatabaseManager
import com.android.lonicera.db.entity.ChatEntity
import com.llmsdk.deepseek.DeepSeekClient
import com.llmsdk.deepseek.DeepSeekClientStream
import com.llmsdk.base.ChatConfig
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.BalanceResponse
import com.llmsdk.base.ChatModel
import com.llmsdk.deepseek.models.FunctionResponse
import com.llmsdk.deepseek.models.ModelInfo
import com.llmsdk.deepseek.models.ToolCall
import com.llmsdk.deepseek.models.ToolMessage
import com.llmsdk.log.ALog
import com.llmsdk.tools.ToolManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

class ChatRepository {
    companion object {
        private const val TAG = "ChatRepository"

        val DEFAULT_MODEL = ChatModel.DEEPSEEK_CHAT
    }

    private val mutex = Mutex()
    private var client: DeepSeekClient? = null
    private var clientStream: DeepSeekClientStream? = null

    private suspend fun getClientStream(token: String): DeepSeekClientStream {
        return mutex.withLock {
            clientStream.takeIf { it?.isSameToken(token) == true }
                ?: DeepSeekClientStream(token) {
                    params {
                        chatStream {
                            model = ChatModel.DEEPSEEK_CHAT
                        }
                    }
                }.also { clientStream = it }
        }
    }

    private suspend fun getClient(token: String): DeepSeekClient {
        return mutex.withLock {
            client.takeIf { it?.isSameToken(token) == true }
                ?: DeepSeekClient(token) {
                    params {
                        chat {
                            model = ChatModel.DEEPSEEK_CHAT
                        }
                    }
                }.also { client = it }
        }
    }

    fun newMessageEntity(title: String, systemPrompt: String): ChatEntity {
        return ChatEntity(
            createdTimestamp = System.currentTimeMillis().toString(),
            title = title,
            updateTimestamp = System.currentTimeMillis(),
            systemPrompt = systemPrompt,
            messages = ArrayList()
        )
    }

    suspend fun storeSettings(key: String, value: String) {
        DatabaseManager.insertSettings(key, value)
    }

    suspend fun deleteSettings(key: String) {
        DatabaseManager.deleteSettings(key)
    }

    suspend fun querySettingsValue(key: String): String? {
        return DatabaseManager.querySettingsValue(key)
    }

    suspend fun setApiKey(model: ChatModel, apiKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseManager.insertApiKey(model.provider, apiKey)
        }
    }

    private fun getModelProvider(model: String): String {
        return when (model) {
            "DeepSeek V3", "DeepSeek R1" -> "DeepSeek"
            else -> "Unknown"
        }
    }

    private suspend fun queryApiKey(model: ChatModel): String {
        return queryApiKey(model.provider)
    }

    private suspend fun queryApiKey(modelProvider: String): String {
        return DatabaseManager.queryApiKey(getModelProvider(modelProvider))?.apiKey ?: ""
    }

    fun getDefaultChatModel(): ChatModel {
        return DEFAULT_MODEL
    }

    suspend fun selectModel(model: ChatModel): ChatConfig {
        return ChatConfig(
            model = model,
            apiKey = queryApiKey(model.nickName),
            tools = ToolManager.availableTools()
        )
    }

    fun getSupportedModels(): List<ChatModel> {
        return listOf(ChatModel.DEEPSEEK_CHAT, ChatModel.DEEPSEEK_REASONER)
    }

    suspend fun availableModels(): List<ModelInfo> {
        return getClient(queryApiKey(ChatModel.DEEPSEEK_CHAT)).getSupportedModels().data
    }

    suspend fun getBalance(): BalanceResponse {
        return getClient(queryApiKey(ChatModel.DEEPSEEK_CHAT)).getBalance()
    }

    suspend fun queryAllChatEntity(): List<ChatEntity> {
        return DatabaseManager.queryAllChatEntity()
    }

    suspend fun deleteChat(createdTimestamp: String) {
        // mMessages.remove(createdTimestamp)
        DatabaseManager.deleteChatEntity(createdTimestamp)
    }

    suspend fun deleteAllChats() {
        // mMessages.clear()
        DatabaseManager.deleteAllChatEntity()
    }

    suspend fun queryMessageEntity(createdTimestamp: String): ChatEntity? {
        return DatabaseManager.queryChatEntity(createdTimestamp)
    }

    suspend fun insertChatEntity(chatEntity: ChatEntity) {
        DatabaseManager.insertChatEntity(chatEntity)
    }

    suspend fun chat(config: ChatConfig, chatEntity: ChatEntity) {
        ALog.i(TAG, "chat")
        try {
            val client = getClient(config.apiKey)
            val response = client.chatCompletion {
                params {
                    model = config.model
                    frequency_penalty = config.frequency_penalty
                    max_tokens = config.max_tokens
                    presence_penalty = config.presence_penalty
                    response_format = config.response_format
                    stop = config.stop
                    temperature = config.temperature
                    top_p = config.top_p
                    tools = config.tools
                    tool_choice = config.tool_choice
                    logprobs = config.logprobs
                    top_logprobs = config.top_logprobs
                }
                messages(list = chatEntity.chatMessages())
            }
            ALog.i(TAG, "chat response: ${response.choices.first()}")
            if (response.choices.first().message.tool_calls?.isNotEmpty() == true) {
                chatEntity.messages.add(
                    ChatUIMessage(
                        message = AssistantMessage(
                            content = response.choices.first().message.content,
                            reasoning_content = response.choices.first().message.reasoning_content,
                            tool_calls = response.choices.first().message.tool_calls
                        ),
                        timestamp = System.currentTimeMillis(),
                        prompt_hit_tokens = response.usage.prompt_cache_hit_tokens,
                        prompt_miss_tokens = response.usage.prompt_cache_miss_tokens,
                        reasoning_tokens = response.usage.completion_tokens_details?.reasoning_tokens
                            ?: 0,
                        isToolCall = true
                    )
                )
                callToolFunctions(config, chatEntity, response.choices.first().message.tool_calls!!)
            } else {
                chatEntity.messages.add(
                    ChatUIMessage(
                        message = AssistantMessage(
                            content = response.choices.first().message.content,
                            reasoning_content = response.choices.first().message.reasoning_content,
                            tool_calls = response.choices.first().message.tool_calls
                        ),
                        timestamp = System.currentTimeMillis(),
                        prompt_hit_tokens = response.usage.prompt_cache_hit_tokens,
                        prompt_miss_tokens = response.usage.prompt_cache_miss_tokens,
                        reasoning_tokens = response.usage.completion_tokens_details?.reasoning_tokens
                            ?: 0,
                    )
                )
            }
        } catch (e: Exception) {
            chatEntity.messages.add(
                ChatUIMessage(
                    message = AssistantMessage(
                        content = e.message ?: "An error occurred"
                    ),
                    timestamp = System.currentTimeMillis(),
                    isError = true
                )
            )
        }
    }

    suspend fun chatStream(
        config: ChatConfig,
        chatEntity: ChatEntity
    ): Flow<ChatEntity> {
        ALog.i(TAG, "chatStream $chatEntity")
        val chatClient = getClientStream(config.apiKey)
        return flow {
            val assistantMessage = AssistantMessage(content = "")
            try {
                chatClient.chatCompletion {
                    params {
                        model = config.model
                        frequency_penalty = config.frequency_penalty
                        max_tokens = config.max_tokens
                        presence_penalty = config.presence_penalty
                        response_format = config.response_format
                        stop = config.stop
                        temperature = config.temperature
                        top_p = config.top_p
                        tools = config.tools
                        tool_choice = config.tool_choice
                        logprobs = config.logprobs
                        top_logprobs = config.top_logprobs
                    }
                    messages(list = chatEntity.chatMessages())
                }.onCompletion {
                    emit(chatEntity)
                }.collect { chunk ->
                    if (chatEntity.messages.lastOrNull()?.message != assistantMessage) {
                        chatEntity.messages.add(
                            ChatUIMessage(
                                message = assistantMessage,
                                timestamp = System.currentTimeMillis(),
                                prompt_hit_tokens = chunk.usage?.prompt_cache_hit_tokens ?: 0,
                                prompt_miss_tokens = chunk.usage?.prompt_cache_miss_tokens ?: 0,
                                reasoning_tokens = chunk.usage?.completion_tokens_details?.reasoning_tokens
                                    ?: 0,
                            )
                        )
                    }
                    chunk.usage?.let { usage ->
                        chatEntity.messages.lastOrNull()?.apply {
                            completion_tokens += usage.completion_tokens
                            prompt_hit_tokens += usage.prompt_cache_hit_tokens
                            prompt_miss_tokens += usage.prompt_cache_miss_tokens
                        }
                    }
                    chunk.choices?.firstOrNull()?.delta?.let { chunkMessage ->
                        ALog.i(TAG, "chatStream chunk: $chunk")
                        chunkMessage.content
                            ?.takeIf { it.isNotEmpty() }
                            ?.also {
                                assistantMessage.apply {
                                    content = (content ?: "") + it
                                }
                            }
                        chunkMessage.reasoning_content
                            ?.takeIf { it.isNotEmpty() }
                            ?.also {
                                assistantMessage.apply {
                                    reasoning_content = (reasoning_content ?: "") + it
                                }
                            }
                        chunkMessage.tool_calls?.let { incomingToolCalls ->

                            val pendingUpdateToolCalls = assistantMessage.tool_calls ?: run {
                                val new = ArrayList<ToolCall>()
                                assistantMessage.tool_calls = new
                                new
                            }

                            incomingToolCalls.forEach { incomingToolCall ->
                                ALog.i(TAG, "chatStream toolCalls: $incomingToolCall")
                                // 通过index定位已有ToolCall
                                val existingIndex = pendingUpdateToolCalls.indexOfFirst {
                                    it.index == incomingToolCall.index
                                }

                                if (existingIndex != -1) {
                                    // 更新已有ToolCall
                                    val existing = pendingUpdateToolCalls[existingIndex]
                                    pendingUpdateToolCalls[existingIndex] = existing.copy(
                                        id = incomingToolCall.id ?: existing.id,
                                        type = incomingToolCall.type ?: existing.type,
                                        function = mergeFunction(
                                            existing.function,
                                            incomingToolCall.function
                                        )
                                    )
                                } else {
                                    // 创建新ToolCall（首次出现该index）
                                    pendingUpdateToolCalls.add(
                                        ToolCall(
                                            index = incomingToolCall.index,
                                            id = incomingToolCall.id,
                                            type = incomingToolCall.type,
                                            function = incomingToolCall.function?.copy(
                                                arguments = incomingToolCall.function?.arguments
                                                    ?: ""
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    }

                    emit(chatEntity)
                }
            } catch (e: Exception) {
                if (chatEntity.messages.lastOrNull()?.message == assistantMessage) {
                    chatEntity.messages.removeLast()
                }
                chatEntity.messages.add(
                    ChatUIMessage(
                        message = AssistantMessage(
                            content = e.message ?: "An error occurred"
                        ),
                        timestamp = System.currentTimeMillis(),
                        isError = true
                    )
                )
                emit(chatEntity)
            }

            val pendingToolCalls = assistantMessage.tool_calls
            ALog.i(TAG, "chatStream pendingToolCalls: $pendingToolCalls")

            if (pendingToolCalls?.isNotEmpty() == true) {
                chatEntity.messages.lastOrNull()?.isToolCall = true
                callToolFunctionsStream(config, chatEntity, pendingToolCalls)
                    .onCompletion {
                        emit(chatEntity)
                    }
                    .collect { callToolEntity ->
                        emit(callToolEntity)
                    }
            }
        }
    }

    private fun mergeFunction(
        existing: FunctionResponse?,
        incoming: FunctionResponse?
    ): FunctionResponse? {
        return when {
            existing == null -> incoming
            incoming == null -> existing
            else -> FunctionResponse(
                name = incoming.name ?: existing.name,
                arguments = (existing.arguments ?: "") + (incoming.arguments ?: "")
            )
        }
    }

    suspend fun functionCallTest(): AssistantMessage {
        val response = ToolManager.callTool("get_weather", emptyMap())
        ALog.i(TAG, "processFunctionCall response: $response")
        return AssistantMessage(
            content = response
        )
    }

    private fun parseArguments(arguments: String?): Map<String, Any> {
        if (arguments.isNullOrEmpty()) return emptyMap()
        return try {
            Json.parseToJsonElement(arguments).jsonObject.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun callToolFunctions(
        config: ChatConfig,
        chatEntity: ChatEntity,
        toolCalls: List<ToolCall>
    ) {
        toolCalls.forEach { toolCall ->
            val functionName = toolCall.function?.name ?: return@forEach
            ALog.i(TAG, "processFunctionCall: $toolCall")
            val response = ToolManager.callTool(
                functionName,
                parseArguments(toolCall.function?.arguments)
            )
            ALog.i(TAG, "processFunctionCall response: $response")
            chatEntity.messages.add(
                ChatUIMessage(
                    timestamp = System.currentTimeMillis(),
                    message = ToolMessage(
                        content = response,
                        tool_call_id = toolCall.id
                    ),
                    isToolResponse = true
                )
            )
        }
        if (toolCalls.isNotEmpty()) {
            chat(config, chatEntity)
        }
    }

    private suspend fun callToolFunctionsStream(
        config: ChatConfig,
        chatEntity: ChatEntity,
        toolCalls: List<ToolCall>
    ): Flow<ChatEntity> {
        if (toolCalls.isEmpty()) throw IllegalArgumentException("toolCalls is empty!")
        toolCalls.forEach { toolCall ->
            val functionName = toolCall.function?.name ?: return@forEach
            ALog.i(TAG, "processFunctionCall: $toolCall")
            val response = ToolManager.callTool(
                functionName,
                parseArguments(toolCall.function?.arguments)
            )
            ALog.i(TAG, "processFunctionCall response: $response")
            chatEntity.messages.add(
                ChatUIMessage(
                    timestamp = System.currentTimeMillis(),
                    message = ToolMessage(
                        content = response,
                        tool_call_id = toolCall.id
                    ),
                    isToolResponse = true
                )
            )
        }
        return chatStream(config, chatEntity)
    }

    private fun mergeArguments(old: JsonObject?, incoming: JsonObject): JsonObject {
        val result = old?.toMutableMap() ?: mutableMapOf()
        for ((key, value) in incoming) {
            result[key] = value
        }
        return JsonObject(result)
    }
}