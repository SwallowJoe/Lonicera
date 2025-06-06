package com.android.lonicera.components.chat.model

import com.android.lonicera.base.State
import com.android.lonicera.db.entity.MessageEntity
import com.llmsdk.deepseek.DeepSeekConfig

data class ChatUIState(
    // created timestamp
    val id: String = "",
    val model: String = "",
    val title: String = "",
    val supportedModels: List<String> = emptyList(),
    val messages: List<ChatUIMessage> = emptyList(),
    // TODO: optimize
    val messageEntities: List<MessageEntity> = emptyList(),
    val error: String? = null,
    var isWaitingResponse: Boolean = false,
    var isLoading: Boolean = false,
    var enableReasonable: Boolean = true,
    var enableNetwork: Boolean = false,
    var enableStreaming: Boolean = false,

    var systemPrompt: String = "",
    var config: DeepSeekConfig = DeepSeekConfig(),

    var showWordCount: Boolean = false,
    var showTokenCount: Boolean = true,
    var showMessageTimestamp: Boolean = true,
) : State