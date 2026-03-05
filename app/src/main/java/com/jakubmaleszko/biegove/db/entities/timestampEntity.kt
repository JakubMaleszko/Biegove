package com.jakubmaleszko.biegove.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class Timestamp (
    @PrimaryKey(autoGenerate = true) val uid: Int = 0,
    @ColumnInfo(name="number") val number: Int,
    @ColumnInfo(name="timestamp") val timestamp: Long
)