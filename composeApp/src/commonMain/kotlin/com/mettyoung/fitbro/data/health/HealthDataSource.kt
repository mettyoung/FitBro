package com.mettyoung.fitbro.data.health

import com.mettyoung.fitbro.data.model.ActivityBurn

interface HealthDataSource {
    suspend fun readActivityData(startDate: String, endDate: String): HealthResult<List<ActivityBurn>>
}

expect fun createHealthDataSource(): HealthDataSource
