package com.mettyoung.fitbro.data.cronometer

import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.Metabolism

interface CronometerDataSource {
    suspend fun fetchDailyIntake(startDate: String, endDate: String): ApiResult<List<DailyIntake>>
    suspend fun fetchMetabolism(startDate: String, endDate: String): ApiResult<List<Metabolism>>
}
