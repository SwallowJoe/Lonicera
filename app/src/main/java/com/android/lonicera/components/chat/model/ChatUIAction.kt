package com.android.lonicera.components.chat.model

import com.android.lonicera.base.Action
import com.llmsdk.base.ChatModel

sealed class ChatUIAction : Action {
    data object LoadChat : ChatUIAction()
    data object NewChat : ChatUIAction()
    data object CleanChatHistory : ChatUIAction()
    class SelectChat(val createdTimestamp: String) : ChatUIAction()
    class SetTitle(val title: String) : ChatUIAction()
    class SetSystemPrompt(val systemPrompt: String): ChatUIAction()
    class SendMessage(val content: String) : ChatUIAction()
    class DeleteChat(val message: ChatUIMessage): ChatUIAction()
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

    class SetApiKey(val model: ChatModel, val apiKey: String) : ChatUIAction()
    data object UseDevelopApiKey: ChatUIAction()

    data object SwitchShowWordTokens: ChatUIAction()
    data object SwitchShowTokenConsume: ChatUIAction()
    data object SwitchShowMessageTimestamp: ChatUIAction()
}
