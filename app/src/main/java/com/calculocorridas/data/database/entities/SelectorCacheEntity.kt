package com.calculocorridas.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "selector_cache",
    indices = [Index("app_key", "pattern_type")]
)
data class SelectorCacheEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val version: Int,
    @ColumnInfo(name = "app_key") val appKey: String,
    @ColumnInfo(name = "pattern_type") val patternType: String,
    val pattern: String,
    @ColumnInfo(name = "selector_type") val selectorType: String,
    val priority: Int = 0,
    @ColumnInfo(name = "is_active") val isActive: Int = 1,
    @ColumnInfo(name = "downloaded_at") val downloadedAt: Long = System.currentTimeMillis()
)
