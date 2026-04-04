package com.jakubmaleszko.biegove.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity
data class Timestamp(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val raceId: Int,
    val number: Int?,
    val time: Int,
    val note: String? = null
)