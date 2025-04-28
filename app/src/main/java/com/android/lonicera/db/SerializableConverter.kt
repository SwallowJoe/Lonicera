package com.android.lonicera.db

import androidx.room.TypeConverter
import com.android.lonicera.components.chat.model.ChatUIMessage
import com.llmsdk.deepseek.models.ChatMessage
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

object SerializableConverter {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        classDiscriminator = "role"
    }

    // ArrayList<ChatUIMessage> 转换
    @TypeConverter
    fun fromChatUIMessageList(list: ArrayList<ChatUIMessage>): String {
        return json.encodeToString(list)
    }

    @TypeConverter
    fun toChatUIMessageList(jsonStr: String): ArrayList<ChatUIMessage> {
        return json.decodeFromString(jsonStr)
    }

    // ChatMessage 多态转换
    @TypeConverter
    fun fromChatMessage(message: ChatMessage): String {
        return json.encodeToString(message)
    }

    @TypeConverter
    fun toChatMessage(jsonStr: String): ChatMessage {
        return json.decodeFromString(jsonStr)
    }

    // JsonObject 转换
    @TypeConverter
    fun fromJsonObject(jsonObject: JsonObject?): String? {
        return jsonObject?.toString()
    }

    @TypeConverter
    fun toJsonObject(jsonStr: String?): JsonObject? {
        return jsonStr?.let { json.parseToJsonElement(it) as JsonObject }
    }
}