package com.android.lonicera.components.chat.model

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import com.android.lonicera.R
import com.android.lonicera.base.BaseViewModel
import com.android.lonicera.base.CoroutineDispatcherProvider
import com.android.lonicera.components.chat.ChatRepository
import com.android.lonicera.db.DatabaseManager
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.UserMessage
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ChatViewModel(
    private val resources: Resources,
    private val chatRepository: ChatRepository,
    private val dispatcherProvider: CoroutineDispatcherProvider
): BaseViewModel<ChatUIAction, ChatUIState, ChatUIEvent>() {
    companion object {
        private const val TAG = "ChatViewModel"
        private const val TIMEOUT = 30000L
    }

    override fun onAction(action: ChatUIAction, currentState: ChatUIState?) {
        when (action) {
            is ChatUIAction.LoadChat -> {
                viewModelScope.launch {
                    loadChat(currentState, action.chatId)
                }
            }
            is ChatUIAction.SendMessage -> {
                viewModelScope.launch {
                    sendMessage(currentState, action.content)
                }
            }

            is ChatUIAction.SetTitle -> {
                emitState {
                    setTitle(currentState, action.title)
                }
            }

            is ChatUIAction.SetSystemPrompt -> {
                emitState {
                    currentState?.copy(
                        systemPrompt = action.systemPrompt
                    )
                }
            }

            is ChatUIAction.ChangeModel -> {
                emitState {
                    changeModel(currentState, action.model)
                }
            }

            is ChatUIAction.SwitchNetworkState -> {
                emitState {
                    currentState?.copy(
                        enableNetwork = !currentState.enableNetwork
                    )
                }
            }
            is ChatUIAction.SwitchReasonableState -> {
                emitState {
                    currentState?.copy(
                        enableReasonable = !currentState.enableReasonable,
                    )
                }
                viewModelScope.launch {
                    chatRepository.functionCallTest()
                }
            }
            is ChatUIAction.SwitchStreamingState -> {
                emitState {
                    currentState?.copy(
                        enableStreaming = !currentState.enableStreaming
                    )
                }
            }
            is ChatUIAction.SetFrequencyPenalty -> TODO()
            is ChatUIAction.SetMaxTokens -> {
                emitState {
                    currentState?.copy(
                        config = currentState.config.copy(
                            max_tokens = action.maxTokens
                        )
                    )
                }
            }
            is ChatUIAction.SetPresencePenalty -> TODO()
            is ChatUIAction.SetStops -> TODO()
            is ChatUIAction.SetTemperature -> {
                emitState {
                    currentState?.copy(
                        config = currentState.config.copy(
                            temperature = action.temperature.toDouble()
                        )
                    )
                }
            }
            is ChatUIAction.SetTopP -> {
                emitState {
                    currentState?.copy(
                        config = currentState.config.copy(
                            top_p = action.topP.toDouble()
                        )
                    )
                }
            }
            is ChatUIAction.SetApiKey -> {
                if (action.apiKey == currentState?.config?.apiKey) {
                    return
                }
                emitState {
                    chatRepository.setApiKey(action.context, action.apiKey)
                    currentState?.copy(
                        config = currentState.config.copy(
                            apiKey = action.apiKey
                        )
                    )
                }
            }
        }
    }

    private fun setTitle(state: ChatUIState?, title: String): ChatUIState? {
        return state?.copy(
            title = title
        )
    }

    private fun changeModel(state: ChatUIState?, model: String): ChatUIState? {
        chatRepository.selectModel(model)
        return state?.copy(
            model = model
        )
    }

    private suspend fun loadChat(state: ChatUIState?, chatId: String?) {
        emitState {
            val messageEntity = if (chatId == null) {
                DatabaseManager.queryAllChatMessage().maxByOrNull { it.timestamp }
            } else {
                DatabaseManager.queryChatMessage(chatId)
            }
            state?.copy(
                supportedModels = chatRepository.getSupportedModels(),
                messages = messageEntity?.messages?.map {
                    ChatUIMessage(
                        content = it,
                        timestamp = System.currentTimeMillis(),
                        avatar = 1
                    )
                } ?: emptyList(),
                title = messageEntity?.title ?: resources.getString(R.string.new_chat),
                config = chatRepository.getConfig(),
                isWaitingResponse = false,
                isLoading = false,
            ) ?: ChatUIState(
                model = chatRepository.getModelName(),
                title = messageEntity?.title ?: resources.getString(R.string.new_chat),
                config = chatRepository.getConfig(),
                messages = messageEntity?.messages?.map {
                    ChatUIMessage(
                        content = it,
                        timestamp = System.currentTimeMillis(),
                        avatar = 1
                    )
                } ?: emptyList(),
                isWaitingResponse = false,
                isLoading = false,
                supportedModels = chatRepository.getSupportedModels()
            )
        }
    }

    private suspend fun sendMessage(state: ChatUIState?, content: String) {
        if (state == null) return
        if (content.isEmpty()) return
        val message = UserMessage(content = content)
        emitState {
            state.copy(
                isWaitingResponse = true,
                messages = state.messages.plus(
                    ChatUIMessage(
                        content = message,
                        timestamp = System.currentTimeMillis(),
                        avatar = 1
                    )
                )
            )
        }

        val messages = state.messages.map { it.content }.toMutableList().apply {
            add(message)
        }
        viewModelScope.launch(dispatcherProvider.io()) {
            if (!state.enableNetwork) {
                val response = AssistantMessage(content = "Assistant received: $content")
                messages.add(response)
                DatabaseManager.insertChatMessage(
                    id = "test",
                    title = state.title,
                    messages = messages
                )
                emitState {
                    replayState?.let {
                        it.copy(
                            isWaitingResponse = false,
                            messages = it.messages.plus(
                                ChatUIMessage(
                                    content = response,
                                    isSender = false,
                                    timestamp = System.currentTimeMillis(),
                                    avatar = 2
                                )
                            )
                        )
                    }
                }
                return@launch
            }
            withTimeoutOrNull(TIMEOUT) {
                chatRepository.sendMessage(
                    message = message,
                    onReply = { chatCompletionResponse ->
                        viewModelScope.launch {
                            val assistantMessage = chatCompletionResponse.choices.first().message
                            emitState {
                                replayState?.let {
                                    it.copy(
                                        isWaitingResponse = false,
                                        messages = it.messages.plus(
                                            ChatUIMessage(
                                                content = assistantMessage,
                                                isSender = false,
                                                timestamp = System.currentTimeMillis(),
                                                avatar = 2,
                                                completion_tokens = chatCompletionResponse.usage.completion_tokens,
                                                prompt_hit_tokens = chatCompletionResponse.usage.prompt_cache_hit_tokens,
                                                prompt_miss_tokens = chatCompletionResponse.usage.prompt_cache_miss_tokens,
                                                reasoning_tokens = chatCompletionResponse.usage.completion_tokens_details?.reasoning_tokens?:0,
                                                timeout = false
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    },
                    onError = { error ->
                        viewModelScope.launch {
                            emitState {
                                replayState?.let {
                                    it.copy(
                                        isWaitingResponse = false,
                                        messages = it.messages.plus(
                                            ChatUIMessage(
                                                content = AssistantMessage(error),
                                                isSender = false,
                                                timestamp = System.currentTimeMillis(),
                                                avatar = 2
                                           )
                                        )
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}