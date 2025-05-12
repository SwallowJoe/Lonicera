package com.android.lonicera.db.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.android.lonicera.db.SerializableConverter
import com.android.lonicera.db.dao.ApiKeyDao
import com.android.lonicera.db.dao.ChatDao
import com.android.lonicera.db.dao.SettingsDao
import com.android.lonicera.db.entity.ApiKeyEntity
import com.android.lonicera.db.entity.ChatEntity
import com.android.lonicera.db.entity.SettingsEntity

@Database(
    entities = [
        ChatEntity::class,
        ApiKeyEntity::class,
        SettingsEntity::class],
    version = 1,
    exportSchema = false)
@TypeConverters(SerializableConverter::class)
abstract class ChatDatabase: RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun settingsDao(): SettingsDao
}