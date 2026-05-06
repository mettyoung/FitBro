package com.mettyoung.fitbro.data.health

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.Metabolism

interface HealthDataSource {
    suspend fun readActivityData(startDate: String, endDate: String): HealthResult<List<ActivityBurn>>
    suspend fun readDailyIntake(startDate: String, endDate: String): HealthResult<List<DailyIntake>>
    suspend fun readBasalMetabolicRate(startDate: String, endDate: String): HealthResult<List<Metabolism>>
}

expect fun createHealthDataSource(): HealthDataSource
