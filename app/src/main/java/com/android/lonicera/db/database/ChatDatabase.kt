package com.android.lonicera.db.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.android.lonicera.db.SerializableConverter
import com.android.lonicera.db.dao.ApiKeyDao
import com.android.lonicera.db.dao.MessageDao
import com.android.lonicera.db.dao.SettingsDao
import com.android.lonicera.db.entity.ApiKeyEntity
import com.android.lonicera.db.entity.MessageEntity
import com.android.lonicera.db.entity.SettingsEntity

@Database(
    entities = [
        MessageEntity::class,
        ApiKeyEntity::class,
        SettingsEntity::class],
    version = 1,
    exportSchema = false)
@TypeConverters(SerializableConverter::class)
abstract class ChatDatabase: RoomDatabase() {
    abstract fun messageDao(): MessageDao
    abstract fun apiKeyDao(): ApiKeyDao
    abstract fun settingsDao(): SettingsDao
}