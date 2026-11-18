package com.mettyoung.fitbro.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata as HCMetadata
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.request.AggregateRequest
import com.mettyoung.fitbro.data.model.HeartRateSummary
import com.mettyoung.fitbro.data.model.Measured
import com.mettyoung.fitbro.data.model.WorkoutDetail
import com.mettyoung.fitbro.data.model.WorkoutSegment
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.mettyoung.fitbro.AndroidAppContext
import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.BodyMeasurement
import java.time.Instant
import java.time.LocalDateTime
import kotlin.reflect.KClass
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

    override suspend fun readBodyMeasurements(
        startDate: String,
        endDate: String
    ): HealthResult<List<BodyMeasurement>> {
        val healthClient = client ?: return HealthResult.Failure(HealthDataError.NotAvailable)
        val granted = try {
            healthClient.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            return HealthResult.Failure(HealthDataError.QueryError(e))
        }
        fun allowed(type: KClass<out Record>) = HealthPermission.getReadPermission(type) in granted
        if (!allowed(WeightRecord::class) && !allowed(BodyFatRecord::class) && !allowed(LeanBodyMassRecord::class)) {
            return HealthResult.Failure(HealthDataError.PermissionDenied)
        }

        return try {
            val zone = ZoneId.systemDefault()
            val start = LocalDate.parse(startDate).atStartOfDay()
            val end = LocalDate.parse(endDate).plusDays(1).atStartOfDay()
            fun Instant.day() = atZone(zone).toLocalDate().toString()

            /** Last value of the day per date. */
            fun <T> lastPerDay(records: List<T>, time: (T) -> Instant, value: (T) -> Double): Map<String, Double> =
                records.sortedBy(time).associate { time(it).day() to value(it) }

            val weights = if (allowed(WeightRecord::class))
                lastPerDay(readAll(healthClient, WeightRecord::class, start, end), { it.time }, { it.weight.inKilograms }) else emptyMap()
            val fat = if (allowed(BodyFatRecord::class))
                lastPerDay(readAll(healthClient, BodyFatRecord::class, start, end), { it.time }, { it.percentage.value }) else emptyMap()
            val lean = if (allowed(LeanBodyMassRecord::class))
                lastPerDay(readAll(healthClient, LeanBodyMassRecord::class, start, end), { it.time }, { it.mass.inKilograms }) else emptyMap()

            val measurements = (weights.keys + fat.keys + lean.keys).sorted().map { d ->
                BodyMeasurement(date = d, weightKg = weights[d], bodyFatPct = fat[d], leanMassKg = lean[d])
            }
            HealthResult.Success(measurements)
        } catch (e: Exception) {
            HealthResult.Failure(HealthDataError.QueryError(e))
        }
    }

    private suspend fun <T : Record> readAll(
        healthClient: HealthConnectClient,
        type: KClass<T>,
        start: LocalDateTime,
        end: LocalDateTime
    ): List<T> {
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = healthClient.readRecords(
                ReadRecordsRequest(recordType = type, timeRangeFilter = TimeRangeFilter.between(start, end), pageToken = pageToken)
            )
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
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
                val type = exerciseTypeToWorkoutType(record.exerciseType)
                CardioSession(
                    id = record.metadata.id,
                    date = record.startTime.atZone(zone).toLocalDate().toString(),
                    startEpochMs = record.startTime.toEpochMilli(),
                    minutes = Duration.between(record.startTime, record.endTime).toMinutes().toInt(),
                    activity = record.title?.takeIf { it.isNotBlank() } ?: type.label,
                    type = type,
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

    override suspend fun readWorkoutDetail(session: CardioSession): HealthResult<WorkoutDetail> {
        val healthClient = client ?: return HealthResult.Failure(HealthDataError.NotAvailable)
        return try {
            val granted = healthClient.permissionController.getGrantedPermissions()
            if (HealthPermission.getReadPermission(ExerciseSessionRecord::class) !in granted) {
                return HealthResult.Failure(HealthDataError.PermissionDenied)
            }
            val record = healthClient.readRecord(ExerciseSessionRecord::class, session.id).record
            val window = TimeRangeFilter.between(record.startTime, record.endTime)

            val calories = if (HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class) in granted) {
                val agg = healthClient.aggregate(
                    AggregateRequest(setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL), window)
                )
                agg[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.let { energy ->
                    Measured(energy.inKilocalories, agg.dataOrigins.map { sourceAppLabel(it.packageName) }.distinct())
                }
            } else null

            val hrAllowed = HealthPermission.getReadPermission(HeartRateRecord::class) in granted
            val heartRate = if (hrAllowed) {
                val agg = healthClient.aggregate(
                    AggregateRequest(
                        setOf(HeartRateRecord.BPM_AVG, HeartRateRecord.BPM_MIN, HeartRateRecord.BPM_MAX, HeartRateRecord.MEASUREMENTS_COUNT),
                        window
                    )
                )
                val avg = agg[HeartRateRecord.BPM_AVG]
                if (avg != null) Measured(
                    HeartRateSummary(
                        avgBpm = avg,
                        minBpm = agg[HeartRateRecord.BPM_MIN] ?: avg,
                        maxBpm = agg[HeartRateRecord.BPM_MAX] ?: avg,
                        samples = agg[HeartRateRecord.MEASUREMENTS_COUNT] ?: 0
                    ),
                    agg.dataOrigins.map { sourceAppLabel(it.packageName) }.distinct()
                ) else null
            } else null

            val meta = record.metadata
            HealthResult.Success(
                WorkoutDetail(
                    session = session,
                    endEpochMs = record.endTime.toEpochMilli(),
                    title = record.title?.takeIf { it.isNotBlank() },
                    notes = record.notes?.takeIf { it.isNotBlank() },
                    sourceId = meta.dataOrigin.packageName,
                    recordingMethod = when (meta.recordingMethod) {
                        HCMetadata.RECORDING_METHOD_ACTIVELY_RECORDED -> "Tracked in app"
                        HCMetadata.RECORDING_METHOD_AUTOMATICALLY_RECORDED -> "Auto-detected"
                        HCMetadata.RECORDING_METHOD_MANUAL_ENTRY -> "Manual entry"
                        else -> null
                    },
                    device = meta.device?.let { d ->
                        listOfNotNull(d.manufacturer, d.model).joinToString(" ").ifBlank { null }
                            ?.let { name -> deviceTypeLabel(d.type)?.let { "$name ($it)" } ?: name }
                            ?: deviceTypeLabel(d.type)
                    },
                    lastModifiedEpochMs = meta.lastModifiedTime.toEpochMilli(),
                    activeKcal = calories,
                    heartRate = heartRate,
                    heartRateAllowed = hrAllowed,
                    segments = record.segments.map { seg ->
                        WorkoutSegment(
                            name = segmentName(seg.segmentType),
                            startEpochMs = seg.startTime.toEpochMilli(),
                            endEpochMs = seg.endTime.toEpochMilli(),
                            repetitions = seg.repetitions
                        )
                    },
                    laps = record.laps.size,
                    hasRoute = record.exerciseRouteResult !is ExerciseRouteResult.NoData
                )
            )
        } catch (e: Exception) {
            Log.e("HealthConnect", "Workout detail failed: ${e.message}", e)
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

/** Known health apps by package; others resolve via PackageManager (may be hidden by package visibility). */
private fun deviceTypeLabel(type: Int): String? = when (type) {
    Device.TYPE_WATCH -> "watch"
    Device.TYPE_PHONE -> "phone"
    Device.TYPE_SCALE -> "scale"
    Device.TYPE_RING -> "ring"
    Device.TYPE_FITNESS_BAND -> "band"
    Device.TYPE_CHEST_STRAP -> "chest strap"
    else -> null
}

/** Health Connect segment types are 0..67 in declaration order; names derived from the constants. */
private val SEGMENT_NAMES = listOf(
    "Unknown", "Arm curl", "Back extension", "Ball slam", "Barbell shoulder press", "Bench press",
    "Bench sit-up", "Biking", "Stationary biking", "Burpee", "Crunch", "Deadlift",
    "Double-arm triceps extension", "Dumbbell curl (left)", "Dumbbell curl (right)", "Dumbbell front raise",
    "Dumbbell lateral raise", "Dumbbell row", "Dumbbell triceps extension (left)",
    "Dumbbell triceps extension (right)", "Dumbbell triceps extension", "Elliptical", "Forward twist",
    "Front raise", "HIIT", "Hip thrust", "Hula hoop", "Jumping jack", "Jump rope", "Kettlebell swing",
    "Lateral raise", "Lat pull-down", "Leg curl", "Leg extension", "Leg press", "Leg raise", "Lunge",
    "Mountain climber", "Other workout", "Pause", "Pilates", "Plank", "Pull-up", "Punch", "Rest",
    "Rowing machine", "Running", "Treadmill", "Shoulder press", "Single-arm triceps extension", "Sit-up",
    "Squat", "Stair climbing", "Stair machine", "Stretching", "Backstroke", "Breaststroke", "Butterfly",
    "Freestyle", "Mixed swimming", "Open-water swimming", "Swimming", "Pool swimming", "Upper twist",
    "Walking", "Weightlifting", "Wheelchair", "Yoga"
)

private fun segmentName(type: Int): String = SEGMENT_NAMES.getOrNull(type) ?: "Exercise $type"

private val KNOWN_SOURCES = mapOf(
    "com.mi.health" to "Mi Fitness",
    "com.xiaomi.wearable" to "Mi Fitness",
    "com.xiaomi.hm.health" to "Zepp Life",
    "com.google.android.apps.fitness" to "Google Fit",
    "com.samsung.android.app.health" to "Samsung Health",
    "com.mettyoung.fitbro" to "FitBro",
    "com.hevy" to "Hevy"
)

private fun sourceAppLabel(packageName: String): String =
    KNOWN_SOURCES[packageName] ?: try {
        val pm = AndroidAppContext.context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    } catch (e: Exception) {
        packageName
    }
