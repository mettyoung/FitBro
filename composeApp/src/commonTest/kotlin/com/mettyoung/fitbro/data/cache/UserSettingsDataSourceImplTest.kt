package com.mettyoung.fitbro.data.cache

import com.mettyoung.fitbro.data.model.MacroDataSource
import com.russhwolf.settings.Settings
import kotlin.test.Test
import kotlin.test.assertEquals

class UserSettingsDataSourceImplTest {

    @Test
    fun macroDataSourcePersistsIndependentlyPerDate() {
        val settings = InMemorySettings()
        val dataSource = UserSettingsDataSourceImpl(settings)

        assertEquals(MacroDataSource.FOOD_DIARY, dataSource.getMacroDataSourceForDate("2026-05-19"))
        assertEquals(MacroDataSource.FOOD_DIARY, dataSource.getMacroDataSourceForDate("2026-05-20"))

        dataSource.setMacroDataSourceForDate("2026-05-19", MacroDataSource.HEALTH_CONNECT)
        dataSource.setMacroDataSourceForDate("2026-05-20", MacroDataSource.FOOD_DIARY)

        val restored = UserSettingsDataSourceImpl(settings)
        assertEquals(MacroDataSource.HEALTH_CONNECT, restored.getMacroDataSourceForDate("2026-05-19"))
        assertEquals(MacroDataSource.FOOD_DIARY, restored.getMacroDataSourceForDate("2026-05-20"))
    }
}

private class InMemorySettings : Settings {
    private val values = mutableMapOf<String, Any>()

    override val keys: Set<String> get() = values.keys
    override val size: Int get() = values.size

    override fun clear() = values.clear()
    override fun remove(key: String) {
        values.remove(key)
    }

    override fun hasKey(key: String): Boolean = values.containsKey(key)

    override fun putInt(key: String, value: Int) {
        values[key] = value
    }

    override fun getInt(key: String, defaultValue: Int): Int = getIntOrNull(key) ?: defaultValue
    override fun getIntOrNull(key: String): Int? = values[key] as? Int

    override fun putLong(key: String, value: Long) {
        values[key] = value
    }

    override fun getLong(key: String, defaultValue: Long): Long = getLongOrNull(key) ?: defaultValue
    override fun getLongOrNull(key: String): Long? = values[key] as? Long

    override fun putString(key: String, value: String) {
        values[key] = value
    }

    override fun getString(key: String, defaultValue: String): String = getStringOrNull(key) ?: defaultValue
    override fun getStringOrNull(key: String): String? = values[key] as? String

    override fun putFloat(key: String, value: Float) {
        values[key] = value
    }

    override fun getFloat(key: String, defaultValue: Float): Float = getFloatOrNull(key) ?: defaultValue
    override fun getFloatOrNull(key: String): Float? = values[key] as? Float

    override fun putDouble(key: String, value: Double) {
        values[key] = value
    }

    override fun getDouble(key: String, defaultValue: Double): Double = getDoubleOrNull(key) ?: defaultValue
    override fun getDoubleOrNull(key: String): Double? = values[key] as? Double

    override fun putBoolean(key: String, value: Boolean) {
        values[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean = getBooleanOrNull(key) ?: defaultValue
    override fun getBooleanOrNull(key: String): Boolean? = values[key] as? Boolean
}
