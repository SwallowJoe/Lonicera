package com.android.lonicera.components.chat.model

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import com.android.lonicera.BuildConfig
import com.android.lonicera.R
import com.android.lonicera.base.BaseViewModel
import com.android.lonicera.base.CoroutineDispatcherProvider
import com.android.lonicera.components.chat.ChatRepository
import com.llmsdk.deepseek.DeepSeekConfig
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.ChatModel
import com.llmsdk.deepseek.models.ToolMessage
import com.llmsdk.deepseek.models.UserMessage
import com.llmsdk.tools.ToolManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
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
                viewModelScope.launch(dispatcherProvider.io()) {
                    loadChat(currentState)
                }
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
                        config = currentState.config.copy(
                            stream = !currentState.config.stream
                        )
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
                currentState?.let {
                    emitState {
                        val developApiKey = BuildConfig.DEEP_SEEK_API_KEY
                        chatRepository.setApiKey(
                            currentState.model,
                            developApiKey
                        )
                        currentState.copy(
                            config = currentState.config.copy(
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

    private suspend fun changeModel(state: ChatUIState?, nickName: String): ChatUIState? {
        val model = when (nickName) {
            ChatModel.DEEPSEEK_CHAT.nickName -> {
                ChatModel.DEEPSEEK_CHAT
            }
            ChatModel.DEEPSEEK_REASONER.nickName -> {
                ChatModel.DEEPSEEK_REASONER
            }
            else -> ChatModel.DEEPSEEK_CHAT
        }

        return state?.copy(
            model = model
        )
    }

    private suspend fun loadChat(state: ChatUIState?) {
        emitState {
            val chatEntities = chatRepository.queryAllChatEntity()
            // delay(5000)
            val chatEntity = chatEntities.maxByOrNull { it.updateTimestamp }
            state?.copy(
                chatEntity = chatEntity ?: chatRepository.newMessageEntity(
                    title = resources.getString(R.string.new_chat),
                    systemPrompt = ""
                ),
                supportedModels = chatRepository.getSupportedModels(),
                chatHistories = chatEntities.associate { it.createdTimestamp to it.title },
                config = chatRepository.selectModel(chatRepository.getSupportedModels().first()),
                isWaitingResponse = false,
                isLoading = false,
            ) ?: ChatUIState(
                chatEntity = chatEntity ?: chatRepository.newMessageEntity(
                    title = resources.getString(R.string.new_chat),
                    systemPrompt = ""
                ),
                model = chatRepository.getDefaultChatModel(),
                config = chatRepository.selectModel(chatRepository.getSupportedModels().first()),
                chatHistories = chatEntities.associate { it.createdTimestamp to it.title },
                isWaitingResponse = false,
                isLoading = false,
                supportedModels = chatRepository.getSupportedModels()
            )
        }
    }

    private fun sendMessage(state: ChatUIState?, content: String) {
        if (state == null) return
        if (content.isEmpty()) return

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
        if (!state.config.stream) {
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
        if (state.config.stream) {
            val assistantMessage = AssistantMessage(content = "")
            chatRepository.chatStream(
                config = state.config,
                chatEntity = state.chatEntity
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
            }.collect { chunk ->
                if (state.chatEntity.messages.lastOrNull()?.message != assistantMessage) {
                    state.chatEntity.messages.add(
                        ChatUIMessage(
                            message = assistantMessage,
                            timestamp = System.currentTimeMillis(),
                        )
                    )
                }
                assistantMessage.content += chunk.choices.firstOrNull()?.delta?.content ?: ""
                chunk.choices.firstOrNull()?.delta?.reasoning_content?.let {
                    assistantMessage.reasoning_content += it
                }

                emitState {
                    replayState?.copy(
                        chatEntity = state.chatEntity.copy(
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
                    state.config,
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