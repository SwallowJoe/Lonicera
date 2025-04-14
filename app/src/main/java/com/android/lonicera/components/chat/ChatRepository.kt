package com.android.lonicera.components.chat

import android.icu.text.SimpleDateFormat
import com.android.lonicera.BuildConfig
import com.llmsdk.deepseek.DeepSeekClient
import com.llmsdk.deepseek.models.Message
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.random.Random

class ChatRepository(private var title: String) {
    private val chat = DeepSeekClient(apiKey = BuildConfig.DEEP_SEEK_API_KEY)
    private val messages = ArrayList<Message>()

    fun getTitle(): String {
        return title
    }

    fun getMessages(): List<Message> {
        return messages
    }

    suspend fun sendMessageTest(message: String): String {
        delay(Random.nextLong(100, 2500))

        return "**Ok, I got your message**:\r\r$message\r\r> ${SimpleDateFormat("yyyy-MM-dd HH:mm:sss").format(Date(System.currentTimeMillis()))}"
    }

    suspend fun availableModels(): String {
        return chat.listModels().data.toString()
    }

    suspend fun waitResponse(): Message {
        val response = chat.chatCompletion(
            messages = messages,
        )
        val message = response.choices.first().message
        messages.add(message)
        return message
    }

    fun sendMessage(message: String): Message {
        val msg = Message(role = "user", content = message)
        messages.add(msg)
        return msg
    }
}