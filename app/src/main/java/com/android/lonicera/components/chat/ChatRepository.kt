package com.android.lonicera.components.chat

import android.content.Context
import android.util.Log
import com.android.lonicera.BuildConfig
import com.android.lonicera.db.DatabaseManager
import com.android.lonicera.db.SharedPreferencesManager
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

class ChatRepository(context: Context, private var title: String) {
    companion object {
        private const val TAG = "ChatRepository"
        private const val TABLE_NAME = "chat_repository"
        private const val KEY_API = "api"
    }

    private val mChat = DeepSeekClient()
    private val mConfig = DeepSeekConfig(
        model = ChatModel.DEEPSEEK_CHAT,
        apiKey = readApiKey(context), //BuildConfig.DEEP_SEEK_API_KEY,
        tools = ToolManager.availableTools()
    )

    private val mMessages = ArrayList<ChatMessage>()

    fun getModelName(): String {
        return when (mConfig.model) {
            ChatModel.DEEPSEEK_CHAT -> "DeepSeek V3"
            ChatModel.DEEPSEEK_REASONER -> "DeepSeek R1"
            else -> "DeepSeek V3"
        }
    }

    fun setApiKey(context: Context, apiKey: String) {
        if (mConfig.apiKey == apiKey) return

        mConfig.apiKey = apiKey

        CoroutineScope(Dispatchers.IO).launch {
            val data = HashMap<String, String>()
            data[KEY_API] = apiKey
            SharedPreferencesManager.save(context, TABLE_NAME, data)
        }
    }

    private fun readApiKey(context: Context): String {
        return SharedPreferencesManager.read(context, TABLE_NAME, KEY_API, "")
    }

    fun selectModel(modelName: String) {
        mConfig.model = when (modelName) {
            "DeepSeek V3" -> ChatModel.DEEPSEEK_CHAT
            "DeepSeek R1" -> ChatModel.DEEPSEEK_REASONER
            else -> ChatModel.DEEPSEEK_CHAT
        }
    }

    fun getConfig(): DeepSeekConfig {
        return mConfig
    }

    fun getSupportedModels(): List<String> {
        return listOf("DeepSeek V3", "DeepSeek R1")
    }

    fun getMessages(): List<ChatMessage> {
        return mMessages
    }

    suspend fun availableModels(): List<ModelInfo> {
        return mChat.getSupportedModels(mConfig).data
    }

    suspend fun getBalance(): BalanceResponse {
        return mChat.getBalance(mConfig)
    }

    suspend fun sendMessage(message: ChatMessage,
                            onReply: (ChatCompletionResponse) -> Unit,
                            onError: (String) -> Unit) {
        try {
            mMessages.add(message)
            val response = mChat.chatCompletion(
                config = mConfig,
                messages = mMessages,
            )
            val reply = response.choices.first().message
            mMessages.add(reply)
            onReply.invoke(response)

            reply.tool_calls?.let {
                if (it.isEmpty()) return@let
                val toolCallReply = processFunctionCall(it)
                mMessages.add(toolCallReply.choices.first().message)
                onReply.invoke(toolCallReply)

                DatabaseManager.insertChatMessage(toolCallReply.id, title, mMessages)
            }
        } catch (e: Exception) {
            onError.invoke("**Sorry, I'm having trouble understanding your request. Please try again.**\n\n${e.message}}")
        }
    }

    suspend fun sendMessage(message: ChatMessage): AssistantMessage {
        try {
            mMessages.add(message)
            val response = mChat.chatCompletion(
                config = mConfig,
                messages = mMessages,
            )
            val reply = response.choices.first().message
            mMessages.add(reply)

            reply.tool_calls?.let {
                if (it.isEmpty()) return@let
                val toolCallReply = processFunctionCall(it)
                mMessages.add(toolCallReply.choices.first().message)
                return toolCallReply.choices.first().message
            }
            return reply
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage: ${e.message}")

            return AssistantMessage(
                content = "**Sorry, I'm having trouble understanding your request. Please try again.**\n\n${e.message}}"
            )
        }
    }

    suspend fun functionCallTest(): AssistantMessage {
        val response = ToolManager.callTool("get_weather", emptyMap())
        Log.i(TAG, "processFunctionCall response: $response")
        return AssistantMessage(
            content = response
        )
    }

    private suspend fun processFunctionCall(toolCalls: List<ToolCall>): ChatCompletionResponse {
        toolCalls.forEach { toolCall ->
            Log.i(TAG, "processFunctionCall: $toolCall")
            val response = ToolManager.callTool(toolCall.function.name,
                toolCall.function.arguments?.toMap()?: emptyMap())
            Log.i(TAG, "processFunctionCall response: $response")
            mMessages.add(
                ToolMessage(
                    content = response,
                    tool_call_id = toolCall.id
                )
            )
        }
        return mChat.chatCompletion(
            config = mConfig,
            messages = mMessages,
        )
    }
}