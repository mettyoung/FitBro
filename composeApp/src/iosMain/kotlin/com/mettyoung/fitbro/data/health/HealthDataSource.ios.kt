package com.mettyoung.fitbro.data.health

import com.mettyoung.fitbro.data.model.ActivityBurn
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuery
import platform.HealthKit.HKQueryOptionNone
import platform.HealthKit.HKQuantitySample
import platform.HealthKit.HKQuantityType
import platform.HealthKit.HKQuantityTypeIdentifierActiveEnergyBurned
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKUnit
import platform.HealthKit.kilocalorieUnit
import platform.HealthKit.predicateForSamplesWithStartDate
import kotlin.coroutines.resume

actual fun createHealthDataSource(): HealthDataSource = HealthKitDataSource()

private class HealthKitDataSource : HealthDataSource {
    private val healthStore = HKHealthStore()

    @Suppress("UNCHECKED_CAST")
    override suspend fun readActivityData(
        startDate: String,
        endDate: String
    ): HealthResult<List<ActivityBurn>> {
        if (!HKHealthStore.isHealthDataAvailable()) {
            return HealthResult.Failure(HealthDataError.NotAvailable)
        }

        val activeEnergyType = HKQuantityType.quantityTypeForIdentifier(
            HKQuantityTypeIdentifierActiveEnergyBurned
        ) ?: return HealthResult.Failure(HealthDataError.NotAvailable)

        val authorized = suspendCancellableCoroutine<Boolean> { cont ->
            healthStore.requestAuthorizationToShareTypes(
                typesToShare = null,
                readTypes = setOf(activeEnergyType as HKObjectType)
            ) { success, _ ->
                cont.resume(success)
            }
        }
        if (!authorized) return HealthResult.Failure(HealthDataError.PermissionDenied)

        val formatter = NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd"
            timeZone = NSTimeZone.localTimeZone
        }

        val cal = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)

        val startNSDate = formatter.dateFromString(startDate)
            ?: return HealthResult.Failure(HealthDataError.QueryError(Exception("Invalid date: $startDate")))
        val endNSDate = cal.dateByAddingUnit(
            unit = NSCalendarUnitDay,
            value = 1,
            toDate = formatter.dateFromString(endDate)
                ?: return HealthResult.Failure(HealthDataError.QueryError(Exception("Invalid date: $endDate"))),
            options = 0u
        ) ?: return HealthResult.Failure(HealthDataError.QueryError(Exception("Date arithmetic failed")))

        val predicate = HKQuery.predicateForSamplesWithStartDate(
            startDate = startNSDate,
            endDate = endNSDate,
            options = HKQueryOptionNone
        )

        return suspendCancellableCoroutine { cont ->
            val query = HKSampleQuery(
                sampleType = activeEnergyType,
                predicate = predicate,
                limit = 0u,
                sortDescriptors = null,
                resultsHandler = { _, samples, error ->
                    when {
                        error != null -> cont.resume(
                            HealthResult.Failure(HealthDataError.QueryError(
                                Exception(error.localizedDescription)
                            ))
                        )
                        else -> {
                            val dailyCalories = mutableMapOf<String, Double>()
                            samples?.forEach { sample ->
                                val quantitySample = sample as? HKQuantitySample ?: return@forEach
                                val kcal = quantitySample.quantity
                                    .doubleValueForUnit(HKUnit.kilocalorieUnit())
                                val dateStr = formatter.stringFromDate(quantitySample.startDate)
                                dailyCalories[dateStr] = (dailyCalories[dateStr] ?: 0.0) + kcal
                            }
                            val activities = dailyCalories.entries
                                .sortedBy { it.key }
                                .map { (date, kcal) -> ActivityBurn(date = date, neat = kcal, eat = 0.0) }
                            cont.resume(HealthResult.Success(activities))
                        }
                    }
                }
            )
            healthStore.executeQuery(query)
        }
    }
}
