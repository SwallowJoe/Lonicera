package com.android.lonicera.components.chat

import android.content.Context
import android.util.Log
import com.android.lonicera.db.DatabaseManager
import com.android.lonicera.db.entity.MessageEntity
import com.llmsdk.deepseek.DeepSeekClient
import com.llmsdk.deepseek.DeepSeekConfig
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.BalanceResponse
import com.llmsdk.deepseek.models.ChatCompletionResponse
import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.ChatModel
import com.llmsdk.deepseek.models.ModelInfo
import com.llmsdk.deepseek.models.ToolCall
import com.llmsdk.deepseek.models.ToolMessage
import com.llmsdk.tools.ToolManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class ChatRepository {
    companion object {
        private const val TAG = "ChatRepository"
    }

    private val mChat = DeepSeekClient()
    private val mConfig = DeepSeekConfig(
        model = ChatModel.DEEPSEEK_CHAT,
        apiKey = "",
        tools = ToolManager.availableTools()
    )

    private val mMessages = ConcurrentHashMap<String, ArrayList<ChatMessage>>()

    suspend fun storeSettings(key: String, value: String) {
        DatabaseManager.insertSettings(key, value)
    }

    suspend fun deleteSettings(key: String) {
        DatabaseManager.deleteSettings(key)
    }

    suspend fun querySettingsValue(key: String): String? {
        return DatabaseManager.querySettingsValue(key)
    }

    fun getModelName(): String {
        return when (mConfig.model) {
            ChatModel.DEEPSEEK_CHAT -> "DeepSeek V3"
            ChatModel.DEEPSEEK_REASONER -> "DeepSeek R1"
            else -> "DeepSeek V3"
        }
    }

    suspend fun setApiKey(model: String, apiKey: String) {
        if (mConfig.apiKey == apiKey) return
        mConfig.apiKey = apiKey
        CoroutineScope(Dispatchers.IO).launch {
            DatabaseManager.insertApiKey(getModelProvider(model), apiKey)
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

    suspend fun selectModel(modelName: String): DeepSeekConfig {
        mConfig.model = when (modelName) {
            "DeepSeek V3" -> ChatModel.DEEPSEEK_CHAT
            "DeepSeek R1" -> ChatModel.DEEPSEEK_REASONER
            else -> ChatModel.DEEPSEEK_CHAT
        }
        mConfig.apiKey = queryApiKey(modelName)
        return mConfig
    }

    fun getConfig(): DeepSeekConfig {
        return mConfig
    }

    fun getSupportedModels(): List<String> {
        return listOf("DeepSeek V3", "DeepSeek R1")
    }

    suspend fun availableModels(): List<ModelInfo> {
        return mChat.getSupportedModels(mConfig).data
    }

    suspend fun getBalance(): BalanceResponse {
        return mChat.getBalance(mConfig)
    }

    suspend fun queryMessageEntities(): List<MessageEntity> {
        val entities = DatabaseManager.queryAllChatMessage()
        entities.forEach { entity ->
            mMessages[entity.createdTimestamp] = ArrayList(entity.messages)
        }
        return entities
    }

    suspend fun deleteChat(createdTimestamp: String) {
        mMessages.remove(createdTimestamp)
        DatabaseManager.deleteChatMessage(createdTimestamp)
    }

    suspend fun deleteAllChats() {
        mMessages.clear()
        DatabaseManager.deleteAllChatMessage()
    }

    suspend fun queryMessageEntity(createdTimestamp: String): MessageEntity? {
        return DatabaseManager.queryChatMessage(createdTimestamp)
    }

    suspend fun deleteChatContent(id: String,
                                  title: String,
                                  message: ChatMessage,
                                  onDatabaseUpdate: (MessageEntity) -> Unit) {

        mMessages[id]?.let { messages ->
            if (messages.remove(message)) {
                DatabaseManager.insertChatMessage(id, title, messages)?.let(onDatabaseUpdate)
            }
        }
    }

    private fun getMessages(id: String): ArrayList<ChatMessage> {
        return mMessages[id] ?: run {
            val newList = ArrayList<ChatMessage>()
            mMessages[id] = newList
            newList
        }
    }

    suspend fun sendMessage(id: String,
                            title: String,
                            message: ChatMessage,
                            onReply: (ChatCompletionResponse) -> Unit,
                            onError: (String) -> Unit,
                            onDatabaseUpdate: (MessageEntity) -> Unit) {
        try {
            val messages = getMessages(id)
            messages.add(message)
            val response = mChat.chatCompletion(
                config = mConfig,
                messages = messages,
            )
            val reply = response.choices.first().message
            messages.add(reply)
            onReply.invoke(response)

            reply.tool_calls?.let {
                if (it.isEmpty()) return@let
                val toolCallReply = processFunctionCall(messages, it)
                messages.add(toolCallReply.choices.first().message)
                onReply.invoke(toolCallReply)

                DatabaseManager.insertChatMessage(id, title, messages)?.let(onDatabaseUpdate)
            } ?: run {
                DatabaseManager.insertChatMessage(id, title, messages)?.let(onDatabaseUpdate)
            }
        } catch (e: Exception) {
            onError.invoke("**Sorry, I'm having trouble understanding your request. Please try again.**\n\n${e}\n\n${e.stackTrace.joinToString("\n")}")
        }
    }

    suspend fun functionCallTest(): AssistantMessage {
        val response = ToolManager.callTool("get_weather", emptyMap())
        Log.i(TAG, "processFunctionCall response: $response")
        return AssistantMessage(
            content = response
        )
    }

    private suspend fun processFunctionCall(messages: ArrayList<ChatMessage>,
                                            toolCalls: List<ToolCall>): ChatCompletionResponse {
        toolCalls.forEach { toolCall ->
            Log.i(TAG, "processFunctionCall: $toolCall")
            val response = ToolManager.callTool(toolCall.function.name,
                toolCall.function.arguments?.toMap()?: emptyMap())
            Log.i(TAG, "processFunctionCall response: $response")
            messages.add(
                ToolMessage(
                    content = response,
                    tool_call_id = toolCall.id
                )
            )
        }
        return mChat.chatCompletion(
            config = mConfig,
            messages = messages,
        )
    }
}