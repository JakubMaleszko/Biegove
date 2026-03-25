package com.jakubmaleszko.biegove

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jakubmaleszko.biegove.db.AppDatabase
import com.jakubmaleszko.biegove.db.entities.Race
import com.jakubmaleszko.biegove.db.entities.Settings
import com.jakubmaleszko.biegove.db.entities.Timestamp
import com.jakubmaleszko.biegove.db.mmkv.SettingsManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BiegoveViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val raceDao = db.raceDao()
    private val timestampDao = db.timestampDao()
    // 1. SETTINGS & SELECTION
    private val _settingsState = MutableStateFlow(
        Settings(
            themeMode = SettingsManager.themeMode,
            useDraw = SettingsManager.useDraw,
            selectedRace = SettingsManager.selectedRace
        )
    )
    val settingsState: StateFlow<Settings> = _settingsState.asStateFlow()

    val selectedRaceObject: StateFlow<Race?> = settingsState
        .map { settings -> settings?.selectedRace?.let { raceDao.getById(it) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // 2. TIMESTAMPS FOR THE ACTIVE RACE
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

    init {
        viewModelScope.launch {
            currentRaceResults.collect { results ->
                val race = selectedRaceObject.value
                if (race != null && ConnectionManager.isConnected.value) {
                    ConnectionManager.syncData(race.name,race.startTime, results.map { it.number to it.time })
                }
            }
        }
    }

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
    fun updateRace(race: Race) {
        viewModelScope.launch {
            raceDao.update(race)
        }
    }

    // --- RESULT OPERATIONS ---
    fun addResultToSelectedRace(runnerNumber: Int) {
        val currentRace = selectedRaceObject.value ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
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
    fun updateTimestamp(timestamp: Timestamp) {
        val currentRace = selectedRaceObject.value ?: return

        viewModelScope.launch {
            timestampDao.update(timestamp)
        }
    }

    fun removeTimestamp(timestamp: Timestamp) {
        viewModelScope.launch {
            timestampDao.delete(timestamp)
        }
    }

    // --- SETTINGS ---
    fun updateTheme(mode: Int) {
        SettingsManager.themeMode = mode
        _settingsState.value = _settingsState.value.copy(themeMode = mode)
    }

    fun selectRace(raceId: Int) {
        SettingsManager.selectedRace = raceId
        _settingsState.value = _settingsState.value.copy(selectedRace = raceId)
    }

    fun toggleDraw(enabled: Boolean) {
        SettingsManager.useDraw = enabled
        _settingsState.value = _settingsState.value.copy(useDraw = enabled)
    }

    fun removeRace(race: Race){
        viewModelScope.launch {
            selectRace(-1)
            raceDao.delete(race)
        }
    }

    fun clearAllRaces() {
        viewModelScope.launch {
            selectRace(-1)
            raceDao.deleteAll()
        }
    }
}