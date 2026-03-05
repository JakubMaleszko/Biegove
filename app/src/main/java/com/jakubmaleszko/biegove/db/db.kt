package com.jakubmaleszko.biegove.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.jakubmaleszko.biegove.db.dao.TimestampDao
import com.jakubmaleszko.biegove.db.entities.Timestamp

@Database(entities = [Timestamp::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun timestampDao(): TimestampDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "biegove"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}