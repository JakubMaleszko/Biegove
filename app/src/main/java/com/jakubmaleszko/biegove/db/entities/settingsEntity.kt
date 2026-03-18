package com.jakubmaleszko.biegove.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

// Changed for mmkv reasons
data class Settings(
    @PrimaryKey val uid: Int = 0,
    @ColumnInfo(name = "useDraw") val useDraw: Boolean = false,
    @ColumnInfo(name = "themeMode") val themeMode: Int = 0, // 0: System, 1: Light, 2: Dark
    @ColumnInfo(name = "selectedRace") val selectedRace: Int = -1
)