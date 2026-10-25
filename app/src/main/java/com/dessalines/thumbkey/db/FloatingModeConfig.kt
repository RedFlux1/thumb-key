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
    @ColumnInfo(name = "scale_x")
    val scaleX: Float = 1.0f,
    @ColumnInfo(name = "scale_y")
    val scaleY: Float = 1.0f,
    @ColumnInfo(name = "auto_float_landscape")
    val autoFloatLandscape: Int = 0,
    @ColumnInfo(name = "dock_hint_shown")
    val dockHintShown: Int = 0,
)
