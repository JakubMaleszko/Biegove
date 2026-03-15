package com.jakubmaleszko.biegove.db.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class Timestamp(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val raceId: Int, // This links the result to the Race
    val number: Int,
    val time: Int    // Calculated: (Now - Race StartTime)
)