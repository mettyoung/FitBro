package com.mettyoung.fitbro.data.cache

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.Metabolism

enum class CacheSource { CRONOMETER_INTAKE, CRONOMETER_METABOLISM, HEALTH_ACTIVITY }

interface CacheDataSource {
    suspend fun saveDailyIntake(startDate: String, endDate: String, data: List<DailyIntake>)
    suspend fun getDailyIntake(startDate: String, endDate: String): List<DailyIntake>?

    suspend fun saveMetabolism(startDate: String, endDate: String, data: List<Metabolism>)
    suspend fun getMetabolism(startDate: String, endDate: String): List<Metabolism>?

    suspend fun saveActivityBurn(startDate: String, endDate: String, data: List<ActivityBurn>)
    suspend fun getActivityBurn(startDate: String, endDate: String): List<ActivityBurn>?

    fun getSyncTimestamp(source: CacheSource): Long?
    fun saveSyncTimestamp(source: CacheSource, timestampMs: Long)
    fun clearAll()
}

expect fun createCacheDataSource(): CacheDataSource
