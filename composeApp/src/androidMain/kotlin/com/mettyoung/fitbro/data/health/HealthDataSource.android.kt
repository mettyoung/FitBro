package com.mettyoung.fitbro.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata as HCMetadata
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.mettyoung.fitbro.AndroidAppContext
import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.Metabolism
import java.time.Duration
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId
import android.util.Log

actual fun createHealthDataSource(): HealthDataSource = HealthConnectDataSource()

private class HealthConnectDataSource : HealthDataSource {

    private val client: HealthConnectClient? by lazy {
        val context = AndroidAppContext.context
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
            HealthConnectClient.getOrCreate(context)
        } else null
    }

    override suspend fun readActivityData(
        startDate: String,
        endDate: String
    ): HealthResult<List<ActivityBurn>> {
        val healthClient = client ?: return HealthResult.Failure(HealthDataError.NotAvailable)

        val required = setOf(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))
        val granted = try {
            healthClient.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            return HealthResult.Failure(HealthDataError.QueryError(e))
        }
        if (!granted.containsAll(required)) {
            return HealthResult.Failure(HealthDataError.PermissionDenied)
        }

        return try {
            val zone = ZoneId.systemDefault()
            val start = LocalDate.parse(startDate).atStartOfDay(zone).toLocalDateTime()
            val end = LocalDate.parse(endDate).plusDays(1).atStartOfDay(zone).toLocalDateTime()

            val buckets = healthClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )

            Log.d("HealthConnect", "Activity query returned ${buckets.size} days with data")

            val activities = buckets.map { bucket ->
                val activity = bucket.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]
                    ?.inKilocalories ?: 0.0
                val date = bucket.startTime.atZone(zone).toLocalDate().toString()
                Log.d("HealthConnect", "Activity on $date: $activity kcal")
                ActivityBurn(date = date, neat = activity, eat = 0.0)
            }

            HealthResult.Success(activities)
        } catch (e: Exception) {
            Log.e("HealthConnect", "Activity query failed: ${e.message}", e)
            HealthResult.Failure(HealthDataError.QueryError(e))
        }
    }

    override suspend fun readBasalMetabolicRate(
        startDate: String,
        endDate: String
    ): HealthResult<List<Metabolism>> {
        val healthClient = client ?: return HealthResult.Failure(HealthDataError.NotAvailable)

        val required = setOf(HealthPermission.getReadPermission(BasalMetabolicRateRecord::class))
        val granted = try {
            healthClient.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            return HealthResult.Failure(HealthDataError.QueryError(e))
        }
        if (!granted.containsAll(required)) {
            return HealthResult.Failure(HealthDataError.PermissionDenied)
        }

        return try {
            val zone = ZoneId.systemDefault()
            val start = LocalDate.parse(startDate).atStartOfDay(zone).toInstant()
            val end = LocalDate.parse(endDate).plusDays(1).atStartOfDay(zone).toInstant()

            val response = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = BasalMetabolicRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )

            val byDay = mutableMapOf<String, MutableList<Double>>()
            for (record in response.records) {
                val date = record.time.atZone(zone).toLocalDate().toString()
                byDay.getOrPut(date) { mutableListOf() }
                    .add(record.basalMetabolicRate.inKilocaloriesPerDay)
            }

            val metabolisms = byDay.entries.sortedBy { it.key }.map { (date, values) ->
                Metabolism(date = date, bmr = values.average(), tef = 0.0)
            }

            HealthResult.Success(metabolisms)
        } catch (e: Exception) {
            HealthResult.Failure(HealthDataError.QueryError(e))
        }
    }

    override suspend fun readExerciseSessions(
        startDate: String,
        endDate: String
    ): HealthResult<List<CardioSession>> {
        val healthClient = client ?: return HealthResult.Failure(HealthDataError.NotAvailable)

        val required = setOf(HealthPermission.getReadPermission(ExerciseSessionRecord::class))
        val granted = try {
            healthClient.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            return HealthResult.Failure(HealthDataError.QueryError(e))
        }
        if (!granted.containsAll(required)) {
            return HealthResult.Failure(HealthDataError.PermissionDenied)
        }

        return try {
            val zone = ZoneId.systemDefault()
            val start = LocalDate.parse(startDate).atStartOfDay()
            val end = LocalDate.parse(endDate).plusDays(1).atStartOfDay()

            val records = mutableListOf<ExerciseSessionRecord>()
            var pageToken: String? = null
            do {
                val response = healthClient.readRecords(
                    ReadRecordsRequest(
                        recordType = ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(start, end),
                        pageToken = pageToken
                    )
                )
                records += response.records
                pageToken = response.pageToken
            } while (pageToken != null)

            val sessions = records.map { record ->
                CardioSession(
                    id = record.metadata.id,
                    date = record.startTime.atZone(zone).toLocalDate().toString(),
                    startEpochMs = record.startTime.toEpochMilli(),
                    minutes = Duration.between(record.startTime, record.endTime).toMinutes().toInt(),
                    activity = record.title?.takeIf { it.isNotBlank() } ?: exerciseTypeLabel(record.exerciseType),
                    source = sourceAppLabel(record.metadata.dataOrigin.packageName)
                )
            }
            Log.d("HealthConnect", "Exercise query returned ${sessions.size} sessions")
            HealthResult.Success(sessions)
        } catch (e: Exception) {
            Log.e("HealthConnect", "Exercise query failed: ${e.message}", e)
            HealthResult.Failure(HealthDataError.QueryError(e))
        }
    }

    override suspend fun writeNutritionRecord(entry: FoodDiaryEntry) {
        val healthClient = client ?: return

        val required = setOf(HealthPermission.getWritePermission(NutritionRecord::class))
        val granted = try {
            healthClient.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            Log.w("HealthConnect", "writeNutritionRecord: permission check failed: ${e.message}")
            return
        }
        if (!granted.containsAll(required)) {
            Log.w("HealthConnect", "writeNutritionRecord: WRITE_NUTRITION not granted")
            return
        }

        try {
            val zone = ZoneId.systemDefault()
            val date = LocalDate.parse(entry.date)
            val startInstant = date.atStartOfDay(zone).toInstant()
            val endInstant = startInstant.plusSeconds(60)
            val zoneOffset = zone.rules.getOffset(date.atStartOfDay())

            val record = NutritionRecord(
                startTime = startInstant,
                startZoneOffset = zoneOffset,
                endTime = endInstant,
                endZoneOffset = zoneOffset,
                energy = Energy.kilocalories(entry.calories),
                protein = Mass.grams(entry.proteinG),
                totalCarbohydrate = Mass.grams(entry.carbG),
                totalFat = Mass.grams(entry.fatG),
                name = entry.foodName,
                // FR-18: keyed by uuid so a re-write (edit or cloud update) replaces the record instead of duplicating it.
                metadata = if (entry.uuid.isBlank()) {
                    HCMetadata.manualEntry()
                } else {
                    HCMetadata.manualEntry(
                        clientRecordId = entry.uuid,
                        clientRecordVersion = System.currentTimeMillis()
                    )
                }
            )
            healthClient.insertRecords(listOf(record))
            Log.d("HealthConnect", "writeNutritionRecord: wrote ${entry.foodName} (${entry.calories.toInt()} kcal)")
        } catch (e: Exception) {
            Log.e("HealthConnect", "writeNutritionRecord: failed for ${entry.foodName}: ${e.message}", e)
        }
    }
}

private fun exerciseTypeLabel(type: Int): String = when (type) {
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Running"
    ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Treadmill run"
    ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walking"
    ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hiking"
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Cycling"
    ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Indoor cycling"
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Pool swim"
    ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Open water swim"
    ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL -> "Elliptical"
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE -> "Rowing machine"
    ExerciseSessionRecord.EXERCISE_TYPE_ROWING -> "Rowing"
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING,
    ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE -> "Stair climbing"
    ExerciseSessionRecord.EXERCISE_TYPE_HIGH_INTENSITY_INTERVAL_TRAINING -> "HIIT"
    ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Strength training"
    ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Weightlifting"
    ExerciseSessionRecord.EXERCISE_TYPE_YOGA -> "Yoga"
    ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON -> "Badminton"
    ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL -> "Basketball"
    ExerciseSessionRecord.EXERCISE_TYPE_SOCCER -> "Football"
    ExerciseSessionRecord.EXERCISE_TYPE_TENNIS -> "Tennis"
    else -> "Workout"
}

/** Known health apps by package; others resolve via PackageManager (may be hidden by package visibility). */
private val KNOWN_SOURCES = mapOf(
    "com.mi.health" to "Mi Fitness",
    "com.xiaomi.wearable" to "Mi Fitness",
    "com.xiaomi.hm.health" to "Zepp Life",
    "com.google.android.apps.fitness" to "Google Fit",
    "com.samsung.android.app.health" to "Samsung Health",
    "com.mettyoung.fitbro" to "FitBro"
)

private fun sourceAppLabel(packageName: String): String =
    KNOWN_SOURCES[packageName] ?: try {
        val pm = AndroidAppContext.context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        packageName
    }
