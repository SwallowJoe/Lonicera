package com.android.lonicera.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.llmsdk.deepseek.models.ChatMessage

const val TABLE_NAME_OF_MESSAGE = "tb_message"

@Entity(tableName = TABLE_NAME_OF_MESSAGE)
data class MessageEntity(
    @PrimaryKey val createdTimestamp: String,
    val title: String,
    val updateTimestamp: Long,
    val messages: List<ChatMessage>
)