package com.mettyoung.fitbro.data.cache

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.Metabolism
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CacheDataSourceImpl(private val settings: Settings) : CacheDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    private fun intakeKey(start: String, end: String) = "intake_${start}_${end}"
    private fun metabolismKey(start: String, end: String) = "metabolism_${start}_${end}"
    private fun activityKey(start: String, end: String) = "activity_${start}_${end}"
    private fun syncKey(source: CacheSource) = "sync_${source.name}"
    private val latestBmrKey = "latest_known_bmr"

    override suspend fun saveDailyIntake(startDate: String, endDate: String, data: List<DailyIntake>) {
        settings.putString(intakeKey(startDate, endDate), json.encodeToString(data))
    }

    override suspend fun getDailyIntake(startDate: String, endDate: String): List<DailyIntake>? =
        settings.getStringOrNull(intakeKey(startDate, endDate))
            ?.let { json.decodeFromString(it) }

    override suspend fun saveMetabolism(startDate: String, endDate: String, data: List<Metabolism>) {
        settings.putString(metabolismKey(startDate, endDate), json.encodeToString(data))
    }

    override suspend fun getMetabolism(startDate: String, endDate: String): List<Metabolism>? =
        settings.getStringOrNull(metabolismKey(startDate, endDate))
            ?.let { json.decodeFromString(it) }

    override suspend fun saveActivityBurn(startDate: String, endDate: String, data: List<ActivityBurn>) {
        settings.putString(activityKey(startDate, endDate), json.encodeToString(data))
    }

    override suspend fun getActivityBurn(startDate: String, endDate: String): List<ActivityBurn>? =
        settings.getStringOrNull(activityKey(startDate, endDate))
            ?.let { json.decodeFromString(it) }

    override fun getSyncTimestamp(source: CacheSource): Long? =
        settings.getLongOrNull(syncKey(source))

    override fun saveSyncTimestamp(source: CacheSource, timestampMs: Long) {
        settings.putLong(syncKey(source), timestampMs)
    }

    override fun getLatestBmr(): Double? = settings.getDoubleOrNull(latestBmrKey)

    override fun saveLatestBmr(bmr: Double) = settings.putDouble(latestBmrKey, bmr)

    override fun clearAll() {
        settings.clear()
    }
}
