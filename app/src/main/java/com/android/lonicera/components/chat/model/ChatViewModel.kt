package com.android.lonicera.components.chat.model

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import com.android.lonicera.BuildConfig
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
        private const val TIMEOUT = 300_000L // 300s
    }

    override fun onAction(action: ChatUIAction, currentState: ChatUIState?) {
        when (action) {
            is ChatUIAction.LoadChat -> {
                viewModelScope.launch(dispatcherProvider.io()) {
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
            is ChatUIAction.DeleteChat -> {
                deleteChat(currentState, action.message)
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
            is ChatUIAction.SetFrequencyPenalty -> {

            }
            is ChatUIAction.SetMaxTokens -> {
                emitState {
                    currentState?.copy(
                        config = currentState.config.copy(
                            max_tokens = action.maxTokens
                        )
                    )
                }
            }
            is ChatUIAction.SetPresencePenalty -> {

            }
            is ChatUIAction.SetStops -> {

            }
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
                    chatRepository.setApiKey(action.model, action.apiKey)
                    currentState?.copy(
                        config = currentState.config.copy(
                            apiKey = action.apiKey
                        )
                    )
                }
            }

            is ChatUIAction.UseDevelopApiKey -> {
                emitState {
                    val developApiKey = BuildConfig.DEEP_SEEK_API_KEY
                    chatRepository.setApiKey(
                        chatRepository.getModelName(),
                        developApiKey
                    )
                    currentState?.copy(
                        config = currentState.config.copy(
                            apiKey = developApiKey
                        )
                    )
                }
            }

            is ChatUIAction.CleanChatHistory -> {
                viewModelScope.launch(dispatcherProvider.io()) {
                    chatRepository.deleteAllChats()
                    emitState {
                        currentState?.copy(
                            id = System.currentTimeMillis().toString(),
                            title = resources.getString(R.string.new_chat),
                            messages = emptyList(),
                            messageEntities = emptyList(),
                            systemPrompt = "",
                            error = null,
                            isWaitingResponse = false,
                            isLoading = false,
                        )
                    }
                }
            }
            is ChatUIAction.SelectChat -> {
                selectChat(action.createdTimestamp)
            }
            is ChatUIAction.SwitchShowWordTokens -> {
                emitState {
                    currentState?.copy(
                        showWordCount = !currentState.showWordCount
                    )
                }
            }
            is ChatUIAction.SwitchShowTokenConsume -> {
                emitState {
                    currentState?.copy(
                        showTokenCount = !currentState.showTokenCount
                    )
                }
            }
            is ChatUIAction.SwitchShowMessageTimestamp -> {
                emitState {
                    currentState?.copy(
                        showMessageTimestamp = !currentState.showMessageTimestamp
                    )
                }
            }
        }
    }

    private fun selectChat(createdTimestamp: String) {
        viewModelScope.launch(dispatcherProvider.io()) {
            chatRepository.queryMessageEntity(createdTimestamp)?.let {
                emitState {
                    replayState?.copy(
                        id = it.createdTimestamp,
                        title = it.title,
                        messages = it.messages.map {
                            ChatUIMessage(
                                content = it,
                                timestamp = System.currentTimeMillis()
                            )
                        },
                        systemPrompt = "",
                        error = null,
                        isWaitingResponse = false,
                        isLoading = false,
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

    private fun deleteChat(state: ChatUIState?, message: ChatMessage) {
        if (state == null) return
        viewModelScope.launch(dispatcherProvider.io()) {
            chatRepository.deleteChatContent(
                id = state.id,
                title = state.title,
                message = message,
                onDatabaseUpdate = { messageEntity ->
                    replayState?.let { currentState ->
                        val newEntities = currentState.messageEntities.toMutableList()
                        newEntities.removeIf { it.createdTimestamp == messageEntity.createdTimestamp }
                        newEntities.add(messageEntity)
                        emitState {
                            currentState.copy(
                                messages = currentState.messages.filter { it.content != message },
                                messageEntities = newEntities
                            )
                        }
                    }
                }
            )
        }
    }

    private suspend fun changeModel(state: ChatUIState?, model: String): ChatUIState? {
        chatRepository.selectModel(model)
        return state?.copy(
            model = model
        )
    }

    private suspend fun loadChat(state: ChatUIState?) {
        emitState {
            val messageEntities = chatRepository.queryMessageEntities()
            // delay(5000)
            val messageEntity = messageEntities.maxByOrNull { it.updateTimestamp }
            state?.copy(
                id = messageEntity?.createdTimestamp ?: System.currentTimeMillis().toString(),
                supportedModels = chatRepository.getSupportedModels(),
                messages = messageEntity?.messages?.map {
                    ChatUIMessage(
                        content = it,
                        timestamp = System.currentTimeMillis(),
                    )
                } ?: emptyList(),
                messageEntities = messageEntities,
                title = messageEntity?.title ?: resources.getString(R.string.new_chat),
                config = chatRepository.selectModel(chatRepository.getSupportedModels().first()),
                isWaitingResponse = false,
                isLoading = false,
            ) ?: ChatUIState(
                id = messageEntity?.createdTimestamp ?: System.currentTimeMillis().toString(),
                model = chatRepository.getModelName(),
                title = messageEntity?.title ?: resources.getString(R.string.new_chat),
                config = chatRepository.selectModel(chatRepository.getSupportedModels().first()),
                messages = messageEntity?.messages?.map {
                    ChatUIMessage(
                        content = it,
                        timestamp = System.currentTimeMillis(),
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
            createdTimestamp = state.id,
            title = state.title,
            messages = state.messages.map { it.content }.toMutableList().apply {
                add(response)
            }
        )
        delay(1000)
        emitState {
            state.copy(
                isWaitingResponse = false,
                messages = state.messages.plus(
                    ChatUIMessage(
                        content = response,
                        isSender = false,
                        timestamp = System.currentTimeMillis(),
                    )
                ),
                messageEntities =
                    if (insertedMessageEntity == null) {
                        state.messageEntities
                    } else {
                        val newEntities = state.messageEntities.toMutableList()
                        newEntities.removeIf { it.createdTimestamp == state.id }
                        newEntities.add(insertedMessageEntity)
                        newEntities
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
                            replayState?.let { currentState ->
                                val newEntities = currentState.messageEntities.toMutableList()
                                newEntities.removeIf { it.createdTimestamp == messageEntity.createdTimestamp }
                                newEntities.add(messageEntity)
                                currentState.copy(
                                    messageEntities = newEntities
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}