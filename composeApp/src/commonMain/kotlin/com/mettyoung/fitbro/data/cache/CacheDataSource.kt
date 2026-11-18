package com.mettyoung.fitbro.data.cache

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.Metabolism

enum class CacheSource { HEALTH_METABOLISM, HEALTH_ACTIVITY }

interface CacheDataSource {
    suspend fun saveMetabolism(startDate: String, endDate: String, data: List<Metabolism>)
    suspend fun getMetabolism(startDate: String, endDate: String): List<Metabolism>?

    suspend fun saveActivityBurn(startDate: String, endDate: String, data: List<ActivityBurn>)
    suspend fun getActivityBurn(startDate: String, endDate: String): List<ActivityBurn>?

    /** Last successful body-measurement read (any range), used as a fallback. */
    suspend fun saveBodyMeasurements(data: List<com.mettyoung.fitbro.data.model.BodyMeasurement>)
    suspend fun getBodyMeasurements(): List<com.mettyoung.fitbro.data.model.BodyMeasurement>?

    fun getSyncTimestamp(source: CacheSource): Long?
    fun saveSyncTimestamp(source: CacheSource, timestampMs: Long)
    fun getLatestBmr(): Double?
    fun saveLatestBmr(bmr: Double)
    fun clearAll()
}

expect fun createCacheDataSource(): CacheDataSource
