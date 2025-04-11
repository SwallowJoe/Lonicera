package com.android.lonicera.components.chat

import com.android.lonicera.base.Effect

sealed class ChatUIEvent : Effect {
    data object ShowWarring : ChatUIEvent()
    data object NavDetails : ChatUIEvent()
}