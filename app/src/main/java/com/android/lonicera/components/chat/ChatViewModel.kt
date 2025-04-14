package com.android.lonicera.components.chat

import com.android.lonicera.base.BaseViewModel
import com.android.lonicera.base.CoroutineDispatcherProvider
import com.llmsdk.deepseek.models.Message
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull

class ChatViewModel(
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
                emitState {
                    ChatUIState.Chat(
                        title = "New Chat",
                        messages = emptyList(),
                        isWaitingResponse = false,
                    )
                }
            }
            is ChatUIAction.SendMessage -> {
                emitState {
                    sendMessage(currentState as? ChatUIState.Chat, action.message)
                }
            }
            is ChatUIAction.OnResponse -> {
                emitState {
                    onResponse(currentState as? ChatUIState.Chat, action.message)
                }
            }
            is ChatUIAction.SendMessageTimeout -> {
                emitState {
                    sendMessageTimeout(currentState as? ChatUIState.Chat, action.message)
                }
            }
            is ChatUIAction.SetTitle -> {

            }
        }
    }

    private suspend fun sendMessage(state: ChatUIState.Chat?, message: String): ChatUIState? {
        if (state == null) return null
        if (message.isEmpty()) return state
        val newState = state.messages.toMutableList().let {
            state.copy(
                isWaitingResponse = true,
                messages = it.apply {
                    add(
                        ChatMessage(
                            id = it.size,
                            content = chatRepository.sendMessage(message),
                            timestamp = System.currentTimeMillis(),
                            avatar = 1
                        )
                    )
                }
            )
        }
        CoroutineScope(dispatcherProvider.io()).launch {
            val response = withTimeoutOrNull(TIMEOUT) {
                chatRepository.waitResponse()
            }
            if (response != null) {
                sendAction(ChatUIAction.OnResponse(response))
            } else {
                sendAction(ChatUIAction.SendMessageTimeout("Timeout..."))
            }
        }
        return newState
    }

    private suspend fun onResponse(state: ChatUIState.Chat?, message: Message): ChatUIState? {
        if (state == null) return null
        return withContext(dispatcherProvider.io()) {
            state.messages.toMutableList().let {
                state.copy(
                    isWaitingResponse = false,
                    messages = it.apply {
                        add(
                            ChatMessage(
                                id = it.size,
                                content = message,
                                timestamp = System.currentTimeMillis(),
                                avatar = 2
                            )
                        )
                    }
                )
            }
        }
    }

    private suspend fun sendMessageTimeout(state: ChatUIState.Chat?, message: String): ChatUIState? {
        if (state == null) return null
        return withContext(dispatcherProvider.io()) {
            state.messages.toMutableList().let {
                state.copy(
                    isWaitingResponse = false,
                    messages = it.apply {
                        add(
                            ChatMessage(
                                id = it.size,
                                content = Message(message, Message.ROLE_SYSTEM),
                                timeout = true,
                                timestamp = System.currentTimeMillis(),
                                avatar = 2
                            )
                        )
                    }
                )
            }
        }
     }
}