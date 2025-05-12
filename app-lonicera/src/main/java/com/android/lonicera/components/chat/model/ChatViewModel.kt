package com.android.lonicera.components.chat.model

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import com.android.lonicera.BuildConfig
import com.android.lonicera.R
import com.android.lonicera.base.BaseViewModel
import com.android.lonicera.base.CoroutineDispatcherProvider
import com.android.lonicera.components.chat.ChatRepository
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.base.ChatModel
import com.llmsdk.deepseek.models.UserMessage
import com.llmsdk.log.ALog
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
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

    fun getInitChatUIState(): ChatUIState {
        return ChatUIState(
            chatEntity = chatRepository.newMessageEntity(
                title = resources.getString(R.string.new_chat),
                systemPrompt = ""
            ),
            error = null,
            isWaitingResponse = false,
            isLoading = true,
        )
    }

    override fun onAction(action: ChatUIAction, currentState: ChatUIState?) {
        when (action) {
            is ChatUIAction.LoadChat -> {
                loadChat(currentState)
            }
            is ChatUIAction.NewChat -> {
                if (currentState == null || currentState.chatEntity.messages.isEmpty()) {
                    return
                }
                emitState {
                    currentState.copy(
                        chatEntity = chatRepository.newMessageEntity(
                            title = resources.getString(R.string.new_chat),
                            systemPrompt = ""
                        ),
                        error = null,
                        isWaitingResponse = false,
                        isLoading = false,
                    )
                }
            }
            is ChatUIAction.SendMessage -> {
                sendMessage(currentState, action.content)
            }
            is ChatUIAction.DeleteChat -> {
                deleteChat(currentState, action.message)
            }
            is ChatUIAction.SetTitle -> {
                setTitle(currentState, action.title)
            }

            is ChatUIAction.SetSystemPrompt -> {
                emitState {
                    currentState?.copy(
                        chatEntity = currentState.chatEntity.copy(
                            systemPrompt = action.systemPrompt
                        ),
                    )
                }
            }

            is ChatUIAction.ChangeModel -> {
                changeModel(currentState, action.model)
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
                        chatConfig = currentState.chatConfig.copy(
                            stream = !currentState.chatConfig.stream
                        )
                    )
                }
            }
            is ChatUIAction.SetFrequencyPenalty -> {

            }
            is ChatUIAction.SetMaxTokens -> {
                emitState {
                    currentState?.copy(
                        chatConfig = currentState.chatConfig.copy(
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
                        chatConfig = currentState.chatConfig.copy(
                            temperature = action.temperature.toDouble()
                        )
                    )
                }
            }
            is ChatUIAction.SetTopP -> {
                emitState {
                    currentState?.copy(
                        chatConfig = currentState.chatConfig.copy(
                            top_p = action.topP.toDouble()
                        )
                    )
                }
            }
            is ChatUIAction.SetApiKey -> {
                if (action.apiKey == currentState?.chatConfig?.apiKey) {
                    return
                }
                emitState {
                    chatRepository.setApiKey(action.model, action.apiKey)
                    currentState?.copy(
                        chatConfig = currentState.chatConfig.copy(
                            apiKey = action.apiKey
                        )
                    )
                }
            }

            is ChatUIAction.UseDevelopApiKey -> {
                currentState?.let {
                    emitState {
                        val developApiKey = BuildConfig.DEEP_SEEK_API_KEY
                        chatRepository.setApiKey(
                            currentState.chatConfig.model,
                            developApiKey
                        )
                        currentState.copy(
                            chatConfig = currentState.chatConfig.copy(
                                apiKey = developApiKey
                            )
                        )
                    }
                }
            }

            is ChatUIAction.CleanChatHistory -> {
                viewModelScope.launch(dispatcherProvider.io()) {
                    chatRepository.deleteAllChats()
                }
                emitState {
                    currentState?.copy(
                        chatEntity = chatRepository.newMessageEntity(
                            title = resources.getString(R.string.new_chat),
                            systemPrompt = ""
                        ),
                        chatHistories = emptyMap(),
                        error = null,
                        isWaitingResponse = false,
                        isLoading = false,
                    )
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
                        chatEntity = it.copy(
                            updateTimestamp = System.currentTimeMillis()
                        ),
                        error = null,
                        isWaitingResponse = false,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun setTitle(state: ChatUIState?, title: String) {
        emitState {
            state?.copy(
                chatEntity = state.chatEntity.copy(
                    title = title
                ),
            )
        }
    }

    private fun deleteChat(state: ChatUIState?, message: ChatUIMessage) {
        if (state == null) return

        val messageRemoved = state.chatEntity.messages.removeIf {
            it == message
        }
        if (messageRemoved) {
            val chatEntity = state.chatEntity.copy(
                updateTimestamp = System.currentTimeMillis(),
            )
            emitState {
                state.copy(
                    chatEntity = chatEntity
                )
            }
            viewModelScope.launch(dispatcherProvider.io()) {
                chatRepository.insertChatEntity(chatEntity)
            }
        }
    }

    private fun changeModel(state: ChatUIState?, nickName: String) {
        emitState {
            val model = when (nickName) {
                ChatModel.DEEPSEEK_CHAT.nickName -> {
                    ChatModel.DEEPSEEK_CHAT
                }
                ChatModel.DEEPSEEK_REASONER.nickName -> {
                    ChatModel.DEEPSEEK_REASONER
                }
                else -> ChatModel.DEEPSEEK_CHAT
            }
            state?.copy(
                chatConfig = state.chatConfig.copy(
                    model = model
                )
            )
        }
    }

    private suspend fun updateChatHistory() {
        val chatHistories = chatRepository.queryAllChatEntity()
            .sortedByDescending { it.updateTimestamp }
            .associate { it.createdTimestamp to it.title }
        emitState {
            replayState?.copy(
                chatHistories = chatHistories
            )
        }
    }

    private fun loadChat(state: ChatUIState?) {
        viewModelScope.launch(dispatcherProvider.io()) {
            ALog.i(TAG, "loadChat $state")
            val chatEntities = chatRepository.queryAllChatEntity().sortedByDescending { it.updateTimestamp }
            // delay(5000)
            val chatEntity = chatEntities.maxByOrNull { it.updateTimestamp }
            val chatConfig = chatRepository.selectModel(chatRepository.getSupportedModels().first())
            val chatHistories = chatEntities.associate { it.createdTimestamp to it.title }
            emitState {
                state?.copy(
                    chatEntity = chatEntity ?: chatRepository.newMessageEntity(
                        title = resources.getString(R.string.new_chat),
                        systemPrompt = ""
                    ),
                    supportedModels = chatRepository.getSupportedModels(),
                    chatHistories = chatHistories,
                    chatConfig = chatConfig,
                    isWaitingResponse = false,
                    isLoading = false,
                ) ?: ChatUIState(
                    chatEntity = chatEntity ?: chatRepository.newMessageEntity(
                        title = resources.getString(R.string.new_chat),
                        systemPrompt = ""
                    ),
                    chatConfig = chatConfig,
                    chatHistories = chatHistories,
                    isWaitingResponse = false,
                    isLoading = false,
                    supportedModels = chatRepository.getSupportedModels()
                )
            }
        }
    }

    private fun sendMessage(state: ChatUIState?, content: String) {
        if (state == null) return
        if (content.isEmpty()) return
        val showUpdateHistory = state.chatEntity.messages.isEmpty()

        val message = UserMessage(content = content)
        state.chatEntity.messages.add(
            ChatUIMessage(
                message = message,
                timestamp = System.currentTimeMillis(),
            )
        )
        emitState {
            val new = state.copy(
                isWaitingResponse = true,
                chatEntity = state.chatEntity.copy(
                    title = state.chatEntity.title.takeIf {
                        it != resources.getString(R.string.new_chat)
                    } ?: content,
                    updateTimestamp = System.currentTimeMillis(),
                ),
            )
            viewModelScope.launch(dispatcherProvider.io()) {
                processChat(new)
                if (showUpdateHistory) {
                    updateChatHistory()
                }
            }
            new
        }
    }

    private suspend fun fakeChat(state: ChatUIState, content: String) {
        val chatEntity = state.chatEntity.copy(
            updateTimestamp = System.currentTimeMillis(),
        )
        chatEntity.messages.add(
            ChatUIMessage(
                message = AssistantMessage(content = "Assistant received: $content"),
                timestamp = System.currentTimeMillis(),
            )
        )
        if (!state.chatConfig.stream) {
            emitState {
                replayState?.copy(
                    chatEntity = chatEntity.copy(
                        updateTimestamp = System.currentTimeMillis(),
                    ),
                    isWaitingResponse = false,
                    isLoading = false,
                )
            }
            viewModelScope.launch(dispatcherProvider.io()) {
                replayState?.let {
                    chatRepository.insertChatEntity(it.chatEntity)
                }
            }
            return
        }

        flow {
            repeat(3) {
                emit("\nNow is ${System.currentTimeMillis()}, \t\n```kotlin\nprivate val mMessages = ConcurrentHashMap<String, ArrayList<ChatMessage>>()\n```")
                delay(1000)
            }
        }.onCompletion {
            emitState {
                replayState?.copy(
                    chatEntity = chatEntity.copy(
                        updateTimestamp = System.currentTimeMillis(),
                    ),
                    isWaitingResponse = false,
                    isLoading = false,
                )
            }
            viewModelScope.launch(dispatcherProvider.io()) {
                replayState?.let {
                    chatRepository.insertChatEntity(it.chatEntity)
                }
            }
        }.flowOn(dispatcherProvider.io())
        .collect { what ->
            (chatEntity.messages.last().message as AssistantMessage).content += what

            emitState {
                replayState?.copy(
                    chatEntity = chatEntity.copy(
                        updateTimestamp = System.currentTimeMillis(),
                    ),
                    isWaitingResponse = true,
                    isLoading = false,
                )
            }
        }
    }

    private suspend fun processChat(state: ChatUIState) {
        if (!state.enableNetwork) {
            fakeChat(state, state.chatEntity.messages.lastOrNull()?.message?.content?:"")
            return
        }
        if (state.chatConfig.stream) {
            chatRepository.chatStream(
                config = state.chatConfig,
                chatEntity = state.chatEntity.copy()
            ).onCompletion {
                emitState {
                    state.copy(
                        chatEntity = state.chatEntity.copy(
                            updateTimestamp = System.currentTimeMillis(),
                        ),
                        isWaitingResponse = false,
                        isLoading = false,
                    )
                }
                viewModelScope.launch(dispatcherProvider.io()) {
                    replayState?.let {
                        chatRepository.insertChatEntity(it.chatEntity)
                    }
                }
            }.collect { entity ->
                emitState {
                    replayState?.copy(
                        chatEntity = entity.copy(
                            updateTimestamp = System.currentTimeMillis(),
                        ),
                        isWaitingResponse = true,
                        isLoading = false,
                    )
                }
            }
        } else {
            withTimeoutOrNull(TIMEOUT) {
                val chatEntity = state.chatEntity.copy(
                    updateTimestamp = System.currentTimeMillis(),
                )
                chatRepository.chat(
                    state.chatConfig,
                    chatEntity
                )
                emitState {
                    replayState?.copy(
                        chatEntity = chatEntity,
                        isWaitingResponse = false,
                        isLoading = false,
                    )
                }
                viewModelScope.launch(dispatcherProvider.io()) {
                    replayState?.let {
                        chatRepository.insertChatEntity(it.chatEntity)
                    }
                }
            }
        }
    }
}