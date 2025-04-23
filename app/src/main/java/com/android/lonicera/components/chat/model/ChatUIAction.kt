package com.android.lonicera.components.chat.model

import android.content.Context
import com.android.lonicera.base.Action
import com.android.lonicera.db.entity.MessageEntity

sealed class ChatUIAction : Action {
    data object LoadChat : ChatUIAction()
    data object NewChat : ChatUIAction()
    data object CleanChatHistory : ChatUIAction()
    class SelectChat(val messageEntity: MessageEntity) : ChatUIAction()
    class SetTitle(val title: String) : ChatUIAction()
    class SetSystemPrompt(val systemPrompt: String): ChatUIAction()
    class SendMessage(val content: String) : ChatUIAction()
    class ChangeModel(val model: String) : ChatUIAction()
    data object SwitchNetworkState : ChatUIAction()
    data object SwitchReasonableState : ChatUIAction()
    data object SwitchStreamingState : ChatUIAction()
    class SetTemperature(val temperature: Float) : ChatUIAction()
    class SetMaxTokens(val maxTokens: Int) : ChatUIAction()
    class SetTopP(val topP: Float) : ChatUIAction()
    class SetFrequencyPenalty(val frequencyPenalty: Float) :
        ChatUIAction()
    class SetPresencePenalty(val presencePenalty: Float) :
        ChatUIAction()
    class SetStops(val stops: List<String>) : ChatUIAction()

    class SetApiKey(val context: Context, val apiKey: String) : ChatUIAction()
}
