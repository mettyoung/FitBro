package com.mettyoung.fitbro.ui

import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.cache.createUserSettingsDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide appearance choice. A singleton because the theme wraps everything, including the
 * Android permission gate that runs before [com.mettyoung.fitbro.App] builds its dependencies.
 */
object ThemePreference {
    private val settings: UserSettingsDataSource by lazy { createUserSettingsDataSource() }
    private val _mode by lazy { MutableStateFlow(settings.getThemeMode()) }
    val mode: StateFlow<ThemeMode> get() = _mode.asStateFlow()

    fun set(mode: ThemeMode) {
        settings.setThemeMode(mode)
        _mode.value = mode
    }
}
