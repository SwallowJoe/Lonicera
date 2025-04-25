package com.android.lonicera.db

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.android.lonicera.db.database.ChatDatabase
import com.android.lonicera.db.entity.ApiKeyEntity
import com.android.lonicera.db.entity.MessageEntity
import com.android.lonicera.db.entity.SettingsEntity
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

    suspend fun insertChatMessage(createdTimestamp: String,
                                  title: String,
                                  messages: List<ChatMessage>): MessageEntity? {
        val entity = MessageEntity(createdTimestamp, title, System.currentTimeMillis(), messages)
        chatDatabase?.messageDao()?.insert(entity) ?: run {
            Log.i(TAG, "insert failed of $createdTimestamp")
            return null
        }
        return entity
    }

    suspend fun queryChatMessage(createdTimestamp: String): MessageEntity? {
        return chatDatabase?.messageDao()?.query(createdTimestamp)
    }

    suspend fun queryAllChatMessage(): List<MessageEntity> {
        return chatDatabase?.messageDao()?.queryAll() ?: emptyList()
    }

    suspend fun deleteChatMessage(createdTimestamp: String) {
        chatDatabase?.messageDao()?.delete(createdTimestamp)
    }

    suspend fun deleteAllChatMessage() {
        chatDatabase?.messageDao()?.deleteAll()
    }

    suspend fun insertApiKey(modelProvider: String, apiKey: String) {
        chatDatabase?.apiKeyDao()?.insert(ApiKeyEntity(modelProvider, apiKey))
    }

    suspend fun queryApiKey(modelProvider: String): ApiKeyEntity? {
        return chatDatabase?.apiKeyDao()?.query(modelProvider)
    }

    suspend fun insertSettings(key: String, value: String) {
        chatDatabase?.settingsDao()?.insert(SettingsEntity(key, value))
    }

    suspend fun deleteSettings(key: String) {
        chatDatabase?.settingsDao()?.delete(key)
    }

    suspend fun querySettingsValue(key: String): String? {
        return chatDatabase?.settingsDao()?.query(key)?.settingsValue
    }

    suspend fun queryAllSettings(): List<SettingsEntity> {
        return chatDatabase?.settingsDao()?.queryAll() ?: emptyList()
    }

    suspend fun deleteAllSettings() {
        chatDatabase?.settingsDao()?.deleteAll()
    }
}