package com.mettyoung.fitbro.data.cache

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.BodyMeasurement
import com.mettyoung.fitbro.data.model.Metabolism
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CacheDataSourceImpl(private val settings: Settings) : CacheDataSource {

    private val json = Json { ignoreUnknownKeys = true }

    private fun metabolismKey(start: String, end: String) = "metabolism_${start}_${end}"
    private fun activityKey(start: String, end: String) = "activity_${start}_${end}"
    private fun syncKey(source: CacheSource) = "sync_${source.name}"
    private val latestBmrKey = "latest_known_bmr"
    private val BODY_KEY = "body_measurements"

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

    override suspend fun saveBodyMeasurements(data: List<BodyMeasurement>) {
        settings.putString(BODY_KEY, json.encodeToString(data))
    }

    override suspend fun getBodyMeasurements(): List<BodyMeasurement>? =
        settings.getStringOrNull(BODY_KEY)?.let { json.decodeFromString(it) }

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
