package com.android.lonicera.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.android.lonicera.db.entity.SettingsEntity

@Dao
interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: SettingsEntity)

    @Query("SELECT * FROM tb_settings WHERE settingsKey = :key")
    suspend fun query(key: String): SettingsEntity?

    @Query("SELECT * FROM tb_settings")
    suspend fun queryAll(): List<SettingsEntity>

    @Query("DELETE FROM tb_settings WHERE settingsKey = :key")
    suspend fun delete(key: String)

    @Query("DELETE FROM tb_settings")
    suspend fun deleteAll()
}