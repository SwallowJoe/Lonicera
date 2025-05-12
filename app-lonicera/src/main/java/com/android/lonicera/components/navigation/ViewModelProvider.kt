package com.android.lonicera.components.navigation

import android.content.res.Resources
import com.android.lonicera.base.CoroutineDispatcherProvider
import com.android.lonicera.components.chat.ChatRepository
import com.android.lonicera.components.chat.model.ChatViewModel


object ViewModelProvider {
    fun getChatViewModel(resources: Resources,
                         chatRepository: ChatRepository,
                         dispatcherProvider: CoroutineDispatcherProvider): ChatViewModel {
        return ChatViewModel(
            resources = resources,
            chatRepository = chatRepository,
            dispatcherProvider = dispatcherProvider
        )
    }
}