package com.android.lonicera.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

const val TABLE_NAME_OF_API_KEY = "tb_apikey"

@Entity(tableName = TABLE_NAME_OF_API_KEY)
data class ApiKeyEntity(
    @PrimaryKey val modelProvider: String,
    val apiKey: String
)