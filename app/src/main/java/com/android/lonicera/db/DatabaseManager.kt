package com.android.lonicera.db

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.android.lonicera.db.database.ChatDatabase
import com.android.lonicera.db.entity.ApiKeyEntity
import com.android.lonicera.db.entity.ChatEntity
import com.android.lonicera.db.entity.SettingsEntity

object DatabaseManager {
    private const val TAG = "DatabaseManager"

    private var chatDatabase: ChatDatabase? = null

    fun initDatabase(context: Context) {
        chatDatabase = Room.databaseBuilder(
            context = context.applicationContext,
            klass = ChatDatabase::class.java,
            name = "db_lonicera"
        ).build()
    }

    suspend fun insertChatEntity(entity: ChatEntity): Boolean {
        chatDatabase?.chatDao()?.insert(entity) ?: run {
            Log.i(TAG, "insert failed of $entity")
            return false
        }
        return true
    }

    suspend fun queryChatEntity(createdTimestamp: String): ChatEntity? {
        return chatDatabase?.chatDao()?.query(createdTimestamp)
    }

    suspend fun queryAllChatEntity(): List<ChatEntity> {
        return chatDatabase?.chatDao()?.queryAll() ?: emptyList()
    }

    suspend fun deleteChatEntity(createdTimestamp: String) {
        chatDatabase?.chatDao()?.delete(createdTimestamp)
    }

    suspend fun deleteAllChatEntity() {
        chatDatabase?.chatDao()?.deleteAll()
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