package com.dessalines.thumbkey.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class FloatingModeConfig(
    @PrimaryKey val orientation: Int,
    val x: Int,
    val y: Int,
    val opacity: Int = 100,
    @ColumnInfo(name = "expand_in_floating")
    val expandInFloating: Int = 0,
    @ColumnInfo(name = "max_width_fraction")
    val maxWidthFraction: Int = 60,
)
