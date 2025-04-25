package com.android.lonicera.components.chat

import android.util.Log
import com.android.lonicera.components.chat.model.ChatUIMessage
import com.android.lonicera.db.DatabaseManager
import com.android.lonicera.db.entity.ChatEntity
import com.llmsdk.deepseek.DeepSeekClient
import com.llmsdk.deepseek.DeepSeekConfig
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.BalanceResponse
import com.llmsdk.deepseek.models.ChatCompletionResponse
import com.llmsdk.deepseek.models.ChatCompletionResponseChunk
import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.ChatModel
import com.llmsdk.deepseek.models.ModelInfo
import com.llmsdk.deepseek.models.ToolCall
import com.llmsdk.deepseek.models.ToolMessage
import com.llmsdk.tools.ToolManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

class ChatRepository {
    companion object {
        private const val TAG = "ChatRepository"

        val DEFAULT_MODEL = ChatModel.DEEPSEEK_CHAT
    }

    private val mChat = DeepSeekClient()

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

    private suspend fun queryApiKey(model: String): String {
        return DatabaseManager.queryApiKey(getModelProvider(model))?.apiKey ?: ""
    }

    fun getDefaultChatModel(): ChatModel {
        return DEFAULT_MODEL
    }

    suspend fun selectModel(model: ChatModel): DeepSeekConfig {
        return DeepSeekConfig(
            model = model,
            apiKey = queryApiKey(model.nickName)
        )
    }

    fun getSupportedModels(): List<ChatModel> {
        return listOf(ChatModel.DEEPSEEK_CHAT, ChatModel.DEEPSEEK_REASONER)
    }

    suspend fun availableModels(config: DeepSeekConfig): List<ModelInfo> {
        return mChat.getSupportedModels(config).data
    }

    suspend fun getBalance(config: DeepSeekConfig): BalanceResponse {
        return mChat.getBalance(config)
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
            val response = mChat.chatCompletion(
                config = config,
                messages = chatEntity.chatMessages()
            )
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

    suspend fun chatStream(config: DeepSeekConfig, chatEntity: ChatEntity): Flow<ChatEntity> {
        Log.i(TAG, "chatStream")
        var assistantMessage: AssistantMessage? = null
        return flow {
            try {
                mChat.chatCompletionStream(
                    config = config,
                    messages = chatEntity.chatMessages()
                ).onCompletion { cause ->
                    if (cause == null) {
                        assistantMessage?.tool_calls?.let { toolCalls ->
                            if (toolCalls.isNotEmpty()) {
                                callToolFunctions(config, chatEntity, toolCalls)
                            }
                        }
                    }
                    Log.i(
                        TAG,
                        "chatStream onCompletion $assistantMessage with cause: ${cause?.stackTraceToString()}"
                    )
                }.collect { chunk ->
                    if (assistantMessage == null) {
                        assistantMessage = chunk.choices.first().delta
                        chatEntity.messages.add(
                            ChatUIMessage(
                                message = assistantMessage!!,
                                timestamp = System.currentTimeMillis(),
                                completion_tokens = chunk.usage?.completion_tokens ?: 0,
                                prompt_hit_tokens = chunk.usage?.prompt_cache_hit_tokens ?: 0,
                                prompt_miss_tokens = chunk.usage?.prompt_cache_miss_tokens ?: 0,
                                reasoning_tokens = chunk.usage?.completion_tokens_details?.reasoning_tokens
                                    ?: 0,
                            )
                        )
                    } else {
                        assistantMessage?.content += chunk.choices.first().delta.content
                        assistantMessage?.reasoning_content += chunk.choices.first().delta.reasoning_content
                        // TODO: remove duplicate tool calls?
                        assistantMessage?.tool_calls = chunk.choices.first().delta.tool_calls
                    }
                    assistantMessage?.let {
                        Log.i(TAG, "chatStream collect $chunk")
                        emit(chatEntity)
                    }
                }
            } catch (e: Exception) {
                if (assistantMessage == null) {
                    assistantMessage = AssistantMessage(
                        content = e.message ?: "An error occurred"
                    )
                    chatEntity.messages.add(
                        ChatUIMessage(
                            message = assistantMessage!!,
                            timestamp = System.currentTimeMillis(),
                            isError = true
                        )
                    )
                } else {
                    assistantMessage?.content += e.message ?: "An error occurred"
                }
                emit(chatEntity)
            }
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
            chatStream(config, chatEntity)
        }
    }
}