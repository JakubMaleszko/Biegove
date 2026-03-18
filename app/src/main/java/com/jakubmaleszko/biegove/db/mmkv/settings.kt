package com.jakubmaleszko.biegove.db.mmkv

import com.tencent.mmkv.MMKV

object SettingsManager {
    private val mmkv = MMKV.defaultMMKV()

    // Keys
    private const val KEY_THEME = "theme_mode"
    private const val KEY_USE_DRAW = "use_draw"
    private const val KEY_SELECTED_RACE = "selected_race"

    var themeMode: Int
        get() = mmkv.decodeInt(KEY_THEME, 0)
        set(value) { mmkv.encode(KEY_THEME, value) }

    var useDraw: Boolean
        get() = mmkv.decodeBool(KEY_USE_DRAW, false)
        set(value) { mmkv.encode(KEY_USE_DRAW, value) }

    var selectedRace: Int
        get() = mmkv.decodeInt(KEY_SELECTED_RACE, -1)
        set(value) { mmkv.encode(KEY_SELECTED_RACE, value) }
}