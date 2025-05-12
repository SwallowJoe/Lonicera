package com.android.lonicera.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "tb_settings")
data class SettingsEntity(
    @PrimaryKey val settingsKey: String,
    val settingsValue: String
)