package com.android.lonicera.components.chat

import android.util.Log
import com.android.lonicera.components.chat.model.ChatUIMessage
import com.android.lonicera.db.DatabaseManager
import com.android.lonicera.db.entity.ChatEntity
import com.llmsdk.deepseek.DeepSeekClient
import com.llmsdk.deepseek.DeepSeekClientStream
import com.llmsdk.deepseek.DeepSeekConfig
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.BalanceResponse
import com.llmsdk.deepseek.models.ChatCompletionResponse
import com.llmsdk.deepseek.models.ChatCompletionResponseChunk
import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.ChatModel
import com.llmsdk.deepseek.models.ModelInfo
import com.llmsdk.deepseek.models.ResponseFormat
import com.llmsdk.deepseek.models.StopReason
import com.llmsdk.deepseek.models.Tool
import com.llmsdk.deepseek.models.ToolCall
import com.llmsdk.deepseek.models.ToolChoice
import com.llmsdk.deepseek.models.ToolMessage
import com.llmsdk.tools.ToolManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatRepository {
    companion object {
        private const val TAG = "ChatRepository"

        val DEFAULT_MODEL = ChatModel.DEEPSEEK_CHAT
    }

    private var client: DeepSeekClient? = null
    private var clientStream: DeepSeekClientStream? = null

    private fun getClientStream(token: String): DeepSeekClientStream {
        return clientStream ?: DeepSeekClientStream(token) {
            params {
                chatStream {
                    model = ChatModel.DEEPSEEK_CHAT
                }
            }

        }
    }

    private fun getClient(token: String): DeepSeekClient {
        // 如果已经初始化过，直接返回
        client?.let { return it }

        // 用刚拿到的 token 构造新的 DeepSeekClient
        val newClient = DeepSeekClient(token) {
            params {
                chat {
                    model = ChatModel.DEEPSEEK_CHAT
                }
            }
        }

        // 缓存起来，下次直接复用
        client = newClient
        return newClient
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
            "DeepSeek V3","DeepSeek R1" -> "DeepSeek"
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

    suspend fun selectModel(model: ChatModel): DeepSeekConfig {
        return DeepSeekConfig(
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

    suspend fun chat(config: DeepSeekConfig, chatEntity: ChatEntity) {
        Log.i(TAG, "chat")
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
            Log.i(TAG, "chat response: ${response.choices.first()}")
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
                    reasoning_tokens = response.usage.completion_tokens_details?.reasoning_tokens ?: 0,
                )
            )
            if (response.choices.first().message.tool_calls?.isNotEmpty() == true) {
                callToolFunctions(config, chatEntity, response.choices.first().message.tool_calls!!)
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
        config: DeepSeekConfig,
        chatEntity: ChatEntity
    ): Flow<ChatCompletionResponseChunk> {
        Log.i(TAG, "chatStream $chatEntity")
        val chatClient = getClientStream(config.apiKey)
        return chatClient.chatCompletion {
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
    }

    suspend fun functionCallTest(): AssistantMessage {
        val response = ToolManager.callTool("get_weather", emptyMap())
        Log.i(TAG, "processFunctionCall response: $response")
        return AssistantMessage(
            content = response
        )
    }

    private suspend fun callToolFunctions(config: DeepSeekConfig,
                                          chatEntity: ChatEntity,
                                          toolCalls: List<ToolCall>) {
        toolCalls.forEach { toolCall ->
            Log.i(TAG, "processFunctionCall: $toolCall")
            val response = ToolManager.callTool(toolCall.function.name,
                toolCall.function.arguments?.toMap()?: emptyMap())
            Log.i(TAG, "processFunctionCall response: $response")
            chatEntity.messages.add(
                ChatUIMessage(
                    timestamp = System.currentTimeMillis(),
                    message = ToolMessage(
                        content = response,
                        tool_call_id = toolCall.id
                    ),
                    isToolCall = true
                )
            )
        }
        if (toolCalls.isNotEmpty()) {
            if (config.stream) chatStream(config, chatEntity)
            else chat(config, chatEntity)
        }
    }
}