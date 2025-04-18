package com.android.lonicera.components.chat.model

import com.android.lonicera.base.Action

sealed class ChatUIAction : Action {
    class LoadChat(val chatId: String? = null) : ChatUIAction()
    class SetTitle(val title: String) : ChatUIAction()
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
}
