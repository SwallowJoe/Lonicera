package com.android.lonicera.db

import androidx.room.TypeConverter
import com.llmsdk.deepseek.models.ChatMessage
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable

object SerializableConverter {

    @TypeConverter
    fun fromSerializable(serializable: Serializable?): ByteArray? {
        if (serializable == null) return null
        val output = ByteArrayOutputStream()
        ObjectOutputStream(output).use {
            it.writeObject(serializable)
        }
        return output.toByteArray()
    }

    @TypeConverter
    fun toSerializable(byteArray: ByteArray?): Serializable? {
        if (byteArray == null) return null
        return ObjectInputStream(byteArray.inputStream()).use {
            it.readObject() as? Serializable
        }
    }

    @TypeConverter
    fun fromSerializableList(list: List<Serializable>?): ByteArray? {
        if (list == null) return null
        val output = ByteArrayOutputStream()
        ObjectOutputStream(output).use { it.writeObject(list) }
        return output.toByteArray()
    }

    @TypeConverter
    fun toSerializableList(byteArray: ByteArray?): List<Serializable>? {
        if (byteArray == null) return null
        return ObjectInputStream(byteArray.inputStream()).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as? List<Serializable>
        }
    }

    @TypeConverter
    fun fromChatMessageList(list: List<ChatMessage>?): ByteArray? {
        if (list == null) return null
        val output = ByteArrayOutputStream()
        ObjectOutputStream(output).use {
            it.writeObject(list)
        }
        return output.toByteArray()
    }

    @TypeConverter
    fun toChatMessageList(bytes: ByteArray?): List<ChatMessage> {
        if (bytes == null) return emptyList()
        return ObjectInputStream(bytes.inputStream()).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as? List<ChatMessage>
        } ?: emptyList()
    }
}