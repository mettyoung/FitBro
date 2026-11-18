package com.mettyoung.fitbro.data.health

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.BodyMeasurement
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.Metabolism
import com.mettyoung.fitbro.data.model.WorkoutDetail

interface HealthDataSource {
    suspend fun readActivityData(startDate: String, endDate: String): HealthResult<List<ActivityBurn>>
    suspend fun readBasalMetabolicRate(startDate: String, endDate: String): HealthResult<List<Metabolism>>
    suspend fun writeNutritionRecord(entry: FoodDiaryEntry)
    /** Exercise sessions from any app (e.g. Mi Fitness), keyed to the local date they started. */
    suspend fun readExerciseSessions(startDate: String, endDate: String): HealthResult<List<CardioSession>>
    /**
     * Weight, body fat % and lean mass by local date (last reading of the day per field).
     * Reads whichever of the three are permitted; PermissionDenied only if none are.
     */
    suspend fun readBodyMeasurements(startDate: String, endDate: String): HealthResult<List<BodyMeasurement>>
    /**
     * Full record for one session plus heart rate and active calories in its time window,
     * each tagged with the app(s) that wrote it. Heart rate is optional (null when not allowed).
     */
    suspend fun readWorkoutDetail(session: CardioSession): HealthResult<WorkoutDetail>
}

expect fun createHealthDataSource(): HealthDataSource
