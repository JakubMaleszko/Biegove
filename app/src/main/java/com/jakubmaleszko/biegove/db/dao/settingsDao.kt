package com.jakubmaleszko.biegove.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jakubmaleszko.biegove.db.entities.Settings
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {

    @Query("SELECT * FROM Settings WHERE uid = 0")
    suspend fun getSettings(): Settings?

    @Query("SELECT * FROM Settings WHERE uid = 0")
    fun observeSettings(): Flow<Settings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: Settings)

    @Query("UPDATE Settings SET useDraw = :useDraw WHERE uid = 0")
    suspend fun updateUseDraw(useDraw: Boolean)
}