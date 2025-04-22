package com.android.lonicera.db.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.android.lonicera.db.SerializableConverter
import com.android.lonicera.db.dao.MessageDao
import com.android.lonicera.db.entity.MessageEntity

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
@TypeConverters(SerializableConverter::class)
abstract class ChatDatabase: RoomDatabase() {
    abstract fun messageDao(): MessageDao
}