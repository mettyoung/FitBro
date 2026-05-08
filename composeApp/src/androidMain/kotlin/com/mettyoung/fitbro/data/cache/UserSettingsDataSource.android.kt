package com.mettyoung.fitbro.data.cache

import android.content.Context
import com.mettyoung.fitbro.AndroidAppContext
import com.russhwolf.settings.SharedPreferencesSettings

actual fun createUserSettingsDataSource(): UserSettingsDataSource {
    val prefs = AndroidAppContext.context.getSharedPreferences("fitbro_settings", Context.MODE_PRIVATE)
    return UserSettingsDataSourceImpl(SharedPreferencesSettings(prefs))
}
