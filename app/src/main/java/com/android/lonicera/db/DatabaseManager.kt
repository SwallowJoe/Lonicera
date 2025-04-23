package com.android.lonicera.db

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.android.lonicera.db.database.ChatDatabase
import com.android.lonicera.db.entity.MessageEntity
import com.android.lonicera.db.entity.TABLE_NAME_OF_MESSAGE
import com.llmsdk.deepseek.models.ChatMessage

object DatabaseManager {
    private const val TAG = "DatabaseManager"

    private var chatDatabase: ChatDatabase? = null

    fun initDatabase(context: Context) {
        chatDatabase = Room.databaseBuilder(
            context = context.applicationContext,
            klass = ChatDatabase::class.java,
            name = TABLE_NAME_OF_MESSAGE
        ).build()
    }

    suspend fun insertChatMessage(id: String, title: String, messages: List<ChatMessage>): MessageEntity? {
        val entity = MessageEntity(id, title, System.currentTimeMillis(), messages)
        chatDatabase?.messageDao()?.insert(entity) ?: run {
            Log.i(TAG, "insert failed of $id")
            return null
        }
        return entity
    }

    suspend fun queryChatMessage(id: String): MessageEntity? {
        return chatDatabase?.messageDao()?.query(id)
    }

    suspend fun queryAllChatMessage(): List<MessageEntity> {
        return chatDatabase?.messageDao()?.queryAll() ?: emptyList()
    }
}