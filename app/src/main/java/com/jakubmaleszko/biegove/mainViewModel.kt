package com.jakubmaleszko.biegove

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jakubmaleszko.biegove.db.AppDatabase
import com.jakubmaleszko.biegove.db.entities.Settings
import com.jakubmaleszko.biegove.db.entities.Timestamp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BiegoveViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val settingsDao = db.settingsDao()
    private val timestampDao = db.timestampDao()

    // SETTINGS FLOW
    val settingsState: StateFlow<Settings?> = settingsDao.observeSettings()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly, // Change this from WhileSubscribed
            initialValue = Settings(useDraw = false) // Give it a real default object
        )

    // TIMESTAMPS FLOW (Updates UI automatically on any change)
    // Make sure your DAO has: fun observeAll(): Flow<List<Timestamp>>
    val allTimestamps: StateFlow<List<Timestamp>> = timestampDao.observeTimestamp()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // --- INSERT ---
    fun addTimestamp(number: Int) {
        viewModelScope.launch {
            val newEntry = Timestamp(number = number, timestamp = System.currentTimeMillis())
            timestampDao.insert(newEntry)
            // If you use the Flow (observeAll), the UI updates automatically here!
        }
    }

    // --- REMOVE ---
    fun removeTimestamp(timestamp: Timestamp) {
        viewModelScope.launch {
            timestampDao.delete(timestamp)
        }
    }

    // --- CLEAR ALL ---
    fun clearAllData() {
        viewModelScope.launch {
            timestampDao.deleteAll()
        }
    }

    // --- GET DATA FOR SYNC ---
    suspend fun getAllTimestamps(): List<Timestamp> = timestampDao.getAllOrdered()

    fun toggleDraw(enabled: Boolean) {
        viewModelScope.launch {
            settingsDao.insertSettings(Settings(uid = 0, useDraw = enabled))
        }
    }

    // --- THEME SETTINGS ---
    fun updateTheme(mode: Int) {
        viewModelScope.launch {
            val current = settingsDao.getSettings() ?: Settings()
            settingsDao.insertSettings(current.copy(themeMode = mode))
        }
    }
}