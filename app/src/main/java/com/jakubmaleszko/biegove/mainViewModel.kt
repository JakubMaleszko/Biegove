package com.jakubmaleszko.biegove

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jakubmaleszko.biegove.db.AppDatabase
import com.jakubmaleszko.biegove.db.entities.Race
import com.jakubmaleszko.biegove.db.entities.Settings
import com.jakubmaleszko.biegove.db.entities.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BiegoveViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val settingsDao = db.settingsDao()
    private val raceDao = db.raceDao()
    private val timestampDao = db.timestampDao() // Re-added this

    // 1. SETTINGS & SELECTION
    val settingsState: StateFlow<Settings?> = settingsDao.observeSettings()
        .stateIn(viewModelScope, SharingStarted.Eagerly, Settings())

    val selectedRaceObject: StateFlow<Race?> = settingsState
        .map { settings -> settings?.selectedRace?.let { raceDao.getById(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 2. TIMESTAMPS FOR THE ACTIVE RACE
    // This flow automatically updates whenever the selectedRace changes
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentRaceResults: StateFlow<List<Timestamp>> = settingsState
        .flatMapLatest { settings ->
            val id = settings?.selectedRace ?: -1
            timestampDao.observeByRace(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All races (for the selector list)
    val allRaces: StateFlow<List<Race>> = raceDao.observeRaces()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- RACE OPERATIONS ---
    fun addNewRace(name: String, startTime: Long) {
        viewModelScope.launch {
            val newRace = Race(
                name = name,
                startTime = startTime
            )
            raceDao.insert(newRace)
        }
    }

    // --- RESULT OPERATIONS ---
    fun addResultToSelectedRace(runnerNumber: Int) {
        val currentRace = selectedRaceObject.value ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            // Calculate seconds since race start
            val elapsedSeconds = ((now - currentRace.startTime) / 1000).toInt()

            val newResult = Timestamp(
                raceId = currentRace.uid,
                number = runnerNumber,
                time = elapsedSeconds
            )
            timestampDao.insert(newResult)
        }
    }
    fun insertResultToSelectedRace(timestamp: Timestamp) {
        val currentRace = selectedRaceObject.value ?: return

        viewModelScope.launch {
            timestampDao.insert(timestamp)
        }
    }

    fun removeTimestamp(timestamp: Timestamp) {
        viewModelScope.launch {
            timestampDao.delete(timestamp)
        }
    }

    // --- SETTINGS ---
    fun selectRace(raceId: Int) {
        viewModelScope.launch {
            val current = settingsDao.getSettings() ?: Settings()
            settingsDao.insertSettings(current.copy(selectedRace = raceId))
        }
    }

    fun toggleDraw(enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsDao.getSettings() ?: Settings()
            settingsDao.insertSettings(current.copy(useDraw = enabled))
        }
    }

    fun updateTheme(mode: Int) {
        viewModelScope.launch {
            val current = settingsDao.getSettings() ?: Settings()
            settingsDao.insertSettings(current.copy(themeMode = mode))
        }
    }

    fun removeRace(race: Race){
        viewModelScope.launch {
            settingsDao.updateSelectedRace(-1)
            raceDao.delete(race)
        }
    }

    fun clearAllRaces() {
        viewModelScope.launch {
            settingsDao.updateSelectedRace(-1)
            raceDao.deleteAll()
        }
    }
}