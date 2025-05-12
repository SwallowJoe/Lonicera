package com.android.lonicera.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.lonicera.db.entity.ChatEntity

@Dao
interface ChatDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatEntity)

    @Query("SELECT * FROM tb_chat WHERE createdTimestamp = :createdTimestamp")
    suspend fun query(createdTimestamp: String): ChatEntity?

    @Query("SELECT * FROM tb_chat")
    suspend fun queryAll(): List<ChatEntity>

    @Query("DELETE FROM tb_chat WHERE createdTimestamp = :createdTimestamp")
    suspend fun delete(createdTimestamp: String)

    @Query("DELETE FROM tb_chat")
    suspend fun deleteAll()
}