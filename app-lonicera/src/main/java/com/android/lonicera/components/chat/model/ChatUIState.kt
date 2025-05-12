package com.android.lonicera.components.chat.model

import com.android.lonicera.base.State
import com.android.lonicera.db.entity.ChatEntity
import com.llmsdk.base.ChatConfig
import com.llmsdk.base.ChatModel

data class ChatUIState(
    var chatConfig: ChatConfig = ChatConfig(),
    val chatEntity: ChatEntity,
    /**
     * Chat histories
     * key: createdTimestamp
     * value: title
     */
    val chatHistories: Map<String /*createdTimestamp*/, String /*title*/> = HashMap(),
    val supportedModels: List<ChatModel> = emptyList(),

    // State
    val error: String? = null,
    var isWaitingResponse: Boolean = false,
    var isLoading: Boolean = false,
    var isStreamingOutput: Int = 0,

    // Settings
    var enableReasonable: Boolean = true,
    var enableNetwork: Boolean = false,
    // var enableStreaming: Boolean = false,

    // UI Options
    var showWordCount: Boolean = false,
    var showTokenCount: Boolean = true,
    var showMessageTimestamp: Boolean = true,
) : State