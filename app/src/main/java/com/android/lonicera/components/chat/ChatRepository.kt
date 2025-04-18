package com.android.lonicera.components.chat

import android.util.Log
import com.android.lonicera.BuildConfig
import com.llmsdk.deepseek.DeepSeekClient
import com.llmsdk.deepseek.DeepSeekConfig
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.BalanceResponse
import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.ChatModel
import com.llmsdk.deepseek.models.ModelInfo
import com.llmsdk.deepseek.models.ToolCall
import com.llmsdk.deepseek.models.ToolMessage
import com.llmsdk.tools.ToolManager

class ChatRepository(private var title: String) {
    companion object {
        private const val TAG = "ChatRepository"
    }

    private val mChat = DeepSeekClient(apiKey = BuildConfig.DEEP_SEEK_API_KEY)
    private val mConfig = DeepSeekConfig(
        model = ChatModel.DEEPSEEK_CHAT,
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

    fun selectModel(modelName: String) {
        mConfig.model = when (modelName) {
            "DeepSeek V3" -> ChatModel.DEEPSEEK_CHAT
            "DeepSeek R1" -> ChatModel.DEEPSEEK_REASONER
            else -> ChatModel.DEEPSEEK_CHAT
        }
    }

    fun getSupportedModels(): List<String> {
        return listOf("DeepSeek V3", "DeepSeek R1")
    }

    fun getMessages(): List<ChatMessage> {
        return mMessages
    }

    suspend fun availableModels(): List<ModelInfo> {
        return mChat.getSupportedModels().data
    }

    suspend fun getBalance(): BalanceResponse {
        return mChat.getBalance()
    }

    suspend fun sendMessage(message: ChatMessage): AssistantMessage {
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
            mMessages.add(toolCallReply)
            return toolCallReply
        }
        return reply
    }

    suspend fun functionCallTest(): AssistantMessage {
        val response = ToolManager.callTool("get_weather", emptyMap())
        Log.i(TAG, "processFunctionCall response: $response")
        return AssistantMessage(
            content = response
        )
    }

    private suspend fun processFunctionCall(toolCalls: List<ToolCall>): AssistantMessage {
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
        ).choices.first().message
    }
}