package com.jakubmaleszko.biegove.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.jakubmaleszko.biegove.db.entities.Timestamp
import kotlinx.coroutines.flow.Flow

@Dao
interface TimestampDao {
    @Insert
    suspend fun insert(timestamp: Timestamp)

    @Query("SELECT * FROM Timestamp")
    suspend fun getAll(): List<Timestamp>

    @Query("SELECT * FROM Timestamp ORDER BY timestamp DESC")
    fun observeTimestamp(): Flow<List<Timestamp>>
    @Query("SELECT * FROM Timestamp ORDER BY timestamp DESC")
    suspend fun getAllOrdered(): List<Timestamp>

    @Query("SELECT * FROM Timestamp WHERE uid = :id LIMIT 1")
    suspend fun getById(id: Int): Timestamp?

    @Delete
    suspend fun delete(timestamp: Timestamp)

    @Query("DELETE FROM Timestamp")
    suspend fun deleteAll()
}