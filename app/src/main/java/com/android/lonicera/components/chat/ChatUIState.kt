package com.android.lonicera.components.chat

import com.android.lonicera.base.State

sealed class ChatUIState : State {
    data object Empty : ChatUIState()
    data object Loading : ChatUIState()

    data class Chat(
        var title: String = "",
        val messages: List<ChatMessage> = emptyList(),
        var error: String? = null,
        var isWaitingResponse: Boolean = false,
    ) : ChatUIState()

}