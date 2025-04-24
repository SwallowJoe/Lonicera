package com.android.lonicera.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.lonicera.db.entity.MessageEntity

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Query("SELECT * FROM tb_message WHERE createdTimestamp = :createdTimestamp")
    suspend fun query(createdTimestamp: String): MessageEntity?

    @Query("SELECT * FROM tb_message")
    suspend fun queryAll(): List<MessageEntity>

    @Query("DELETE FROM tb_message WHERE createdTimestamp = :createdTimestamp")
    suspend fun delete(createdTimestamp: String)

    @Query("DELETE FROM tb_message")
    suspend fun deleteAll()
}