package com.dessalines.thumbkey.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FloatingModeConfigDao {
    @Query("SELECT * FROM FloatingModeConfig WHERE orientation = :orientation LIMIT 1")
    fun getConfig(orientation: Int): FloatingModeConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(config: FloatingModeConfig)
}
