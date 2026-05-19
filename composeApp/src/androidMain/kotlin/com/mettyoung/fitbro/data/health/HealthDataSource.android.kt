package com.mettyoung.fitbro.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.metadata.Metadata as HCMetadata
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import com.mettyoung.fitbro.AndroidAppContext
import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.Metabolism
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

    override suspend fun readDailyIntake(
        startDate: String,
        endDate: String
    ): HealthResult<List<DailyIntake>> {
        val healthClient = client ?: return HealthResult.Failure(HealthDataError.NotAvailable)

        val required = setOf(HealthPermission.getReadPermission(NutritionRecord::class))
        val granted = try {
            healthClient.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            Log.e("HealthConnect", "Failed to get granted permissions: ${e.message}")
            return HealthResult.Failure(HealthDataError.QueryError(e))
        }
        if (!granted.containsAll(required)) {
            Log.w("HealthConnect", "NutritionRecord permission not granted")
            return HealthResult.Failure(HealthDataError.PermissionDenied)
        }

        return try {
            val zone = ZoneId.systemDefault()
            val startDt = LocalDate.parse(startDate).atStartOfDay()
            val endDt = LocalDate.parse(endDate).plusDays(1).atStartOfDay()

            Log.d("HealthConnect", "Nutrition query: $startDate to $endDate (LocalDateTime: $startDt to $endDt)")

            val records = healthClient.readRecords(
                ReadRecordsRequest(
                    recordType = NutritionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startDt, endDt)
                )
            ).records

            val appPackageName = AndroidAppContext.context.packageName
            val externalRecords = records.filter { record ->
                record.metadata.dataOrigin.packageName != appPackageName
            }

            Log.d(
                "HealthConnect",
                "Nutrition query returned ${records.size} records (${externalRecords.size} external)"
            )
            if (externalRecords.isEmpty()) {
                Log.w("HealthConnect", "EMPTY: No nutrition data in HC. User must log food in connected app (Google Fit, Samsung Health, etc)")
            }

            val byDate = externalRecords.groupBy { record ->
                record.startTime.atZone(zone).toLocalDate().toString()
            }
            val intakes = byDate.entries.sortedBy { it.key }.map { (date, dayRecords) ->
                val kcal = dayRecords.sumOf { it.energy?.inKilocalories ?: 0.0 }
                val protein = dayRecords.sumOf { it.protein?.inGrams ?: 0.0 }
                val carbs = dayRecords.sumOf { it.totalCarbohydrate?.inGrams ?: 0.0 }
                val fat = dayRecords.sumOf { it.totalFat?.inGrams ?: 0.0 }
                Log.d("HealthConnect", "Nutrition on $date: ${kcal}kcal, protein=${protein}g, carbs=${carbs}g, fat=${fat}g")
                DailyIntake(date = date, totalCalories = kcal, proteinG = protein, carbG = carbs, fatG = fat)
            }

            Log.d("HealthConnect", "Nutrition intakes: ${intakes.size} days with data")
            HealthResult.Success(intakes)
        } catch (e: Exception) {
            Log.e("HealthConnect", "Nutrition query failed: ${e.message}", e)
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
                metadata = HCMetadata.manualEntry()
            )
            healthClient.insertRecords(listOf(record))
            Log.d("HealthConnect", "writeNutritionRecord: wrote ${entry.foodName} (${entry.calories.toInt()} kcal)")
        } catch (e: Exception) {
            Log.e("HealthConnect", "writeNutritionRecord: failed for ${entry.foodName}: ${e.message}", e)
        }
    }
}
