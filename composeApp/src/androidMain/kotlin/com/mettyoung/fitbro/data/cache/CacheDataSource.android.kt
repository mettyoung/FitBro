package com.mettyoung.fitbro.data.cache

import android.content.Context
import com.mettyoung.fitbro.AndroidAppContext
import com.russhwolf.settings.SharedPreferencesSettings

actual fun createCacheDataSource(): CacheDataSource {
    val prefs = AndroidAppContext.context.getSharedPreferences("fitbro_cache", Context.MODE_PRIVATE)
    return CacheDataSourceImpl(SharedPreferencesSettings(prefs))
}
