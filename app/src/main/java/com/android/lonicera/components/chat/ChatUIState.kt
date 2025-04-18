package com.android.lonicera.components.chat

import com.android.lonicera.base.State

data class ChatUIState(
    val model: String,
    val title: String = "",
    val supportedModels: List<String> = emptyList(),
    val messages: List<ChatUIMessage> = emptyList(),
    val error: String? = null,
    var isWaitingResponse: Boolean = false,
    var isLoading: Boolean = false,
    var enableReasonable: Boolean = true,
    var enableNetwork: Boolean = false,
    var enableStreaming: Boolean = false
) : State