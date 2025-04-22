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

    @Query("SELECT * FROM tb_message WHERE id = :id")
    suspend fun query(id: String): MessageEntity?

    @Query("SELECT * FROM tb_message")
    suspend fun queryAll(): List<MessageEntity>
}