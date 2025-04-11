package com.android.lonicera.components.chat

import android.icu.text.SimpleDateFormat
import com.android.mcpclient.McpClient
import kotlinx.coroutines.delay
import java.util.Date
import kotlin.random.Random

class ChatRepository(private val name: String) {
    private val chat = McpClient(name, "1.0.0")

    fun getTitle(): String {
        return name
    }

    suspend fun sendMessage(message: String): String {
        return chat.chat(message) ?: "Null"
//        delay(Random.nextLong(100, 2500))
//        return "**Ok, I got your message**:\r\r$message\r\r> ${SimpleDateFormat("yyyy-MM-dd HH:mm:sss").format(Date(System.currentTimeMillis()))}"
    }
}