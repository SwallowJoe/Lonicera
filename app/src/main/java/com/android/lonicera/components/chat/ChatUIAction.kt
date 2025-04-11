package com.android.lonicera.components.chat

import com.android.lonicera.base.Action


sealed class ChatUIAction : Action {
    class LoadChat(val chatId: String) : ChatUIAction()
    class SetTitle(val title: String) : ChatUIAction()
    class SendMessage(val message: String) : ChatUIAction()
    class OnResponse(val message: String) : ChatUIAction()
    class SendMessageTimeout(val message: String) : ChatUIAction()
}
