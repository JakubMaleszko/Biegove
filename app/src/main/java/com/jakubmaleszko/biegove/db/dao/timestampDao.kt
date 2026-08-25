package com.jakubmaleszko.biegove.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.jakubmaleszko.biegove.db.entities.Timestamp
import kotlinx.coroutines.flow.Flow
@Dao
interface TimestampDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timestamp: Timestamp)

    // Get everything (for backups/sync)
    @Query("SELECT * FROM Timestamp")
    suspend fun getAll(): List<Timestamp>

    @Update
    suspend fun update(timestamp: Timestamp)
    @Query("SELECT * FROM Timestamp WHERE raceId = :raceId ORDER BY time ASC")
    fun observeByRace(raceId: Int): Flow<List<Timestamp>>

    @Query("SELECT * FROM Timestamp WHERE raceId = :raceId ORDER BY time ASC")
    suspend fun getByRace(raceId: Int): List<Timestamp>

    @Query("SELECT COUNT(*) FROM Timestamp WHERE raceId = :raceId")
    fun observeCountByRace(raceId: Int): Flow<Int>

    // --- GENERAL OPERATIONS ---

    @Query("SELECT * FROM Timestamp WHERE id = :id LIMIT 1")
    suspend fun getById(id: Int): Timestamp?

    @Delete
    suspend fun delete(timestamp: Timestamp)

    @Query("DELETE FROM Timestamp")
    suspend fun deleteAll()

    // Useful if you want to clear results for just one race
    @Query("DELETE FROM Timestamp WHERE raceId = :raceId")
    suspend fun deleteByRace(raceId: Int)

    @Query("UPDATE Timestamp SET time = time - :deltaSeconds WHERE raceId = :raceId")
    suspend fun adjustTimesForRace(raceId: Int, deltaSeconds: Int)
}