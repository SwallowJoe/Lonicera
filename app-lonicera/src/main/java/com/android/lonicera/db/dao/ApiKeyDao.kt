package com.android.lonicera.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.lonicera.db.entity.ApiKeyEntity

@Dao
interface ApiKeyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(apiKey: ApiKeyEntity)

    @Query("SELECT * FROM tb_apikey WHERE modelProvider = :modelProvider")
    suspend fun query(modelProvider: String): ApiKeyEntity?

    @Query("SELECT * FROM tb_apikey")
    suspend fun queryAll(): List<ApiKeyEntity>

    @Query("DELETE FROM tb_apikey WHERE modelProvider = :modelProvider")
    suspend fun delete(modelProvider: String)

    @Query("DELETE FROM tb_apikey")
    suspend fun deleteAll()
}