package com.android.lonicera.components.chat.model

import android.content.res.Resources
import androidx.lifecycle.viewModelScope
import com.android.lonicera.R
import com.android.lonicera.base.BaseViewModel
import com.android.lonicera.base.CoroutineDispatcherProvider
import com.android.lonicera.components.chat.ChatRepository
import com.llmsdk.deepseek.models.AssistantMessage
import com.llmsdk.deepseek.models.SystemMessage
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
            is ChatUIAction.SetMaxTokens -> TODO()
            is ChatUIAction.SetPresencePenalty -> TODO()
            is ChatUIAction.SetStops -> TODO()
            is ChatUIAction.SetTemperature -> TODO()
            is ChatUIAction.SetTopP -> TODO()
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
        if (chatId == null || state == null) {
            emitState {
                ChatUIState(
                    model = chatRepository.getModelName(),
                    title = resources.getString(R.string.new_chat),
                    messages = emptyList(),
                    isWaitingResponse = false,
                    isLoading = false,
                    supportedModels = chatRepository.getSupportedModels()
                )
            }
        } else {
            // TODO: 持久化存储中读取Chat记录
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
                        id = state.messages.size,
                        content = message,
                        timestamp = System.currentTimeMillis(),
                        avatar = 1
                    )
                )
            )
        }

        viewModelScope.launch(dispatcherProvider.io()) {
            val response = withTimeoutOrNull(TIMEOUT) {
                if (state.enableNetwork) {
                    chatRepository.sendMessage(message)
                } else {
                    AssistantMessage(
                        content = "Assistant received: $content"
                    )
                }
            }
            emitState {
                replayState?.let {
                    it.copy(
                        isWaitingResponse = false,
                        messages = it.messages.plus(
                            ChatUIMessage(
                                id = it.messages.size,
                                content = response ?: SystemMessage(resources.getString(R.string.chat_timeout)),
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
}