package com.android.lonicera.components.chat.model

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import com.android.lonicera.R
import com.android.lonicera.base.BaseViewModel
import com.android.lonicera.base.CoroutineDispatcherProvider
import com.android.lonicera.components.chat.ChatRepository
import com.android.lonicera.db.DatabaseManager
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.ChatMessage
import com.llmsdk.deepseek.models.UserMessage
import kotlinx.coroutines.delay
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
                    loadChat(currentState)
                }
            }
            is ChatUIAction.NewChat -> {
                if (currentState == null || currentState.messages.isEmpty()) {
                    return
                }
                emitState {
                    currentState.copy(
                        id = System.currentTimeMillis().toString(),
                        title = resources.getString(R.string.new_chat),
                        messages = emptyList(),
                        systemPrompt = "",
                        error = null,
                        isWaitingResponse = false,
                        isLoading = false,
                    )
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

            is ChatUIAction.CleanChatHistory -> {
                // TODO:
            }
            is ChatUIAction.SelectChat -> {
                emitState {
                    currentState?.copy(
                        id = action.messageEntity.id,
                        title = action.messageEntity.title,
                        messages = action.messageEntity.messages.map {
                            ChatUIMessage(
                                content = it,
                                timestamp = System.currentTimeMillis(),
                                avatar = 1
                            )
                        },
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

    private suspend fun loadChat(state: ChatUIState?) {
        emitState {
            val messageEntities =
                DatabaseManager.queryAllChatMessage()
            val messageEntity = messageEntities.maxByOrNull { it.timestamp }

            state?.copy(
                id = messageEntity?.id ?: System.currentTimeMillis().toString(),
                supportedModels = chatRepository.getSupportedModels(),
                messages = messageEntity?.messages?.map {
                    ChatUIMessage(
                        content = it,
                        timestamp = System.currentTimeMillis(),
                        avatar = 1
                    )
                } ?: emptyList(),
                messageEntities = messageEntities,
                title = messageEntity?.title ?: resources.getString(R.string.new_chat),
                config = chatRepository.getConfig(),
                isWaitingResponse = false,
                isLoading = false,
            ) ?: ChatUIState(
                id = messageEntity?.id ?: System.currentTimeMillis().toString(),
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
                messageEntities = messageEntities,
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
            val new = state.copy(
                isWaitingResponse = true,
                title = if (state.title == resources.getString(R.string.new_chat)) content else state.title,
                messages = state.messages.plus(
                    ChatUIMessage(
                        content = message,
                        timestamp = System.currentTimeMillis(),
                        avatar = 1
                    )
                )
            )
            viewModelScope.launch(dispatcherProvider.io()) {
                replayState?.let {
                    processChat(it, message)
                }
            }
            new
        }
    }

    private suspend fun fakeChat(state: ChatUIState, message: UserMessage) {
        val response = AssistantMessage(content = "Assistant received: ${message.content}")
        val insertedMessageEntity = DatabaseManager.insertChatMessage(
            id = state.id,
            title = state.title,
            messages = state.messages.map { it.content }.toMutableList().apply {
                add(response)
            }
        )
        delay(500)
        emitState {
            state.copy(
                isWaitingResponse = false,
                messages = state.messages.plus(
                    ChatUIMessage(
                        content = response,
                        isSender = false,
                        timestamp = System.currentTimeMillis(),
                        avatar = 2
                    )
                ),
                messageEntities =
                    if (insertedMessageEntity == null) {
                        state.messageEntities
                    } else {
                        state.messageEntities.plus(insertedMessageEntity)
                    }
            )
        }
    }

    private suspend fun processChat(state: ChatUIState, message: UserMessage) {
        if (!state.enableNetwork) {
            fakeChat(state, message)
            return
        }
        withTimeoutOrNull(TIMEOUT) {
            chatRepository.sendMessage(
                id = state.id,
                title = state.title,
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
                },
                onDatabaseUpdate = { messageEntity ->
                    viewModelScope.launch {
                        emitState {
                            replayState?.let {
                                it.copy(
                                    messageEntities = it.messageEntities.plus(messageEntity)
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}