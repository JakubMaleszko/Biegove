package com.jakubmaleszko.biegove.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jakubmaleszko.biegove.db.entities.Race
import kotlinx.coroutines.flow.Flow

@Dao
interface RaceDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(race: Race)

    @Update
    suspend fun update(race: Race)

    @Query("SELECT * FROM Race")
    suspend fun getAll(): List<Race>

    @Query("SELECT * FROM Race ORDER BY startTime DESC")
    fun observeRaces(): Flow<List<Race>>

    @Query("SELECT * FROM Race ORDER BY startTime DESC")
    suspend fun getAllOrdered(): List<Race>

    @Query("SELECT * FROM Race WHERE uid = :id LIMIT 1")
    suspend fun getById(id: Int): Race?

    @Delete
    suspend fun delete(race: Race)

    @Query("DELETE FROM Race")
    suspend fun deleteAll()
}