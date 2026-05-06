package com.mettyoung.fitbro.data.health

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.mettyoung.fitbro.AndroidAppContext
import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyIntake
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

            val buckets = healthClient.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(NutritionRecord.ENERGY_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(startDt, endDt),
                    timeRangeSlicer = Period.ofDays(1)
                )
            )

            Log.d("HealthConnect", "Nutrition query returned ${buckets.size} days")
            if (buckets.isEmpty()) {
                Log.w("HealthConnect", "EMPTY: No nutrition data in HC. User must log food in connected app (Google Fit, Samsung Health, etc)")
            }

            val intakes = buckets.mapNotNull { bucket ->
                val energy = bucket.result[NutritionRecord.ENERGY_TOTAL] ?: return@mapNotNull null
                val kcal = energy.inKilocalories
                val date = bucket.startTime.atZone(zone).toLocalDate().toString()
                Log.d("HealthConnect", "Nutrition on $date: ${kcal}kcal")
                DailyIntake(date = date, totalCalories = kcal)
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
}
