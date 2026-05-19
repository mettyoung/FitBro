package com.mettyoung.fitbro.data.health

import com.mettyoung.fitbro.data.model.ActivityBurn
import com.mettyoung.fitbro.data.model.DailyIntake
import com.mettyoung.fitbro.data.model.FoodDiaryEntry
import com.mettyoung.fitbro.data.model.Metabolism
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarIdentifierGregorian
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSPredicate
import platform.Foundation.NSTimeZone
import platform.Foundation.localTimeZone
import platform.HealthKit.HKHealthStore
import platform.HealthKit.HKObjectType
import platform.HealthKit.HKQuery
import platform.HealthKit.HKQueryOptionNone
import platform.HealthKit.HKQuantity
import platform.HealthKit.HKQuantitySample
import platform.HealthKit.HKQuantityType
import platform.HealthKit.HKQuantityTypeIdentifierActiveEnergyBurned
import platform.HealthKit.HKQuantityTypeIdentifierBasalEnergyBurned
import platform.HealthKit.HKQuantityTypeIdentifierDietaryEnergyConsumed
import platform.HealthKit.HKQuantityTypeIdentifierDietaryProtein
import platform.HealthKit.HKQuantityTypeIdentifierDietaryCarbohydrates
import platform.HealthKit.HKQuantityTypeIdentifierDietaryFatTotal
import platform.HealthKit.HKSampleQuery
import platform.HealthKit.HKUnit
import platform.HealthKit.kilocalorieUnit
import platform.HealthKit.gramUnit
import platform.HealthKit.predicateForSamplesWithStartDate
import kotlin.coroutines.resume

actual fun createHealthDataSource(): HealthDataSource = HealthKitDataSource()

private class HealthKitDataSource : HealthDataSource {
    private val healthStore = HKHealthStore()

    @Suppress("UNCHECKED_CAST")
    private suspend fun requestAuthorization(types: Set<HKQuantityType>): Boolean =
        suspendCancellableCoroutine { cont ->
            healthStore.requestAuthorizationToShareTypes(
                typesToShare = null,
                readTypes = types.map { it as HKObjectType }.toSet()
            ) { success, _ ->
                cont.resume(success)
            }
        }

    private fun buildDateRange(
        startDate: String,
        endDate: String,
        formatter: NSDateFormatter
    ): Pair<NSDate, NSDate>? {
        val cal = NSCalendar(calendarIdentifier = NSCalendarIdentifierGregorian)
        val startNSDate = formatter.dateFromString(startDate) ?: return null
        val endNSDate = cal.dateByAddingUnit(
            unit = NSCalendarUnitDay,
            value = 1,
            toDate = formatter.dateFromString(endDate) ?: return null,
            options = 0u
        ) ?: return null
        return startNSDate to endNSDate
    }

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

        val dietaryType = HKQuantityType.quantityTypeForIdentifier(
            HKQuantityTypeIdentifierDietaryEnergyConsumed
        ) ?: return HealthResult.Failure(HealthDataError.NotAvailable)

        val basalType = HKQuantityType.quantityTypeForIdentifier(
            HKQuantityTypeIdentifierBasalEnergyBurned
        ) ?: return HealthResult.Failure(HealthDataError.NotAvailable)

        val authorized = requestAuthorization(setOf(activeEnergyType, dietaryType, basalType))
        if (!authorized) return HealthResult.Failure(HealthDataError.PermissionDenied)

        val formatter = NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd"
            timeZone = NSTimeZone.localTimeZone
        }

        val (startNSDate, endNSDate) = buildDateRange(startDate, endDate, formatter)
            ?: return HealthResult.Failure(HealthDataError.QueryError(Exception("Invalid date range")))

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

    @Suppress("UNCHECKED_CAST")
    override suspend fun readDailyIntake(
        startDate: String,
        endDate: String
    ): HealthResult<List<DailyIntake>> {
        if (!HKHealthStore.isHealthDataAvailable()) {
            return HealthResult.Failure(HealthDataError.NotAvailable)
        }

        val energyType = HKQuantityType.quantityTypeForIdentifier(
            HKQuantityTypeIdentifierDietaryEnergyConsumed
        ) ?: return HealthResult.Failure(HealthDataError.NotAvailable)
        val proteinType = HKQuantityType.quantityTypeForIdentifier(
            HKQuantityTypeIdentifierDietaryProtein
        ) ?: return HealthResult.Failure(HealthDataError.NotAvailable)
        val carbsType = HKQuantityType.quantityTypeForIdentifier(
            HKQuantityTypeIdentifierDietaryCarbohydrates
        ) ?: return HealthResult.Failure(HealthDataError.NotAvailable)
        val fatType = HKQuantityType.quantityTypeForIdentifier(
            HKQuantityTypeIdentifierDietaryFatTotal
        ) ?: return HealthResult.Failure(HealthDataError.NotAvailable)

        val authorized = requestAuthorization(setOf(energyType, proteinType, carbsType, fatType))
        if (!authorized) return HealthResult.Failure(HealthDataError.PermissionDenied)

        val formatter = NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd"
            timeZone = NSTimeZone.localTimeZone
        }

        val (startNSDate, endNSDate) = buildDateRange(startDate, endDate, formatter)
            ?: return HealthResult.Failure(HealthDataError.QueryError(Exception("Invalid date range")))

        val predicate = HKQuery.predicateForSamplesWithStartDate(
            startDate = startNSDate,
            endDate = endNSDate,
            options = HKQueryOptionNone
        )

        return try {
            val dailyEnergy = queryDailyQuantity(energyType, predicate, HKUnit.kilocalorieUnit(), formatter)
            val dailyProtein = queryDailyQuantity(proteinType, predicate, HKUnit.gramUnit(), formatter)
            val dailyCarbs = queryDailyQuantity(carbsType, predicate, HKUnit.gramUnit(), formatter)
            val dailyFat = queryDailyQuantity(fatType, predicate, HKUnit.gramUnit(), formatter)

            val intakes = (dailyEnergy.keys + dailyProtein.keys + dailyCarbs.keys + dailyFat.keys)
                .distinct()
                .sorted()
                .map { date ->
                    DailyIntake(
                        date = date,
                        totalCalories = dailyEnergy[date] ?: 0.0,
                        proteinG = dailyProtein[date] ?: 0.0,
                        carbG = dailyCarbs[date] ?: 0.0,
                        fatG = dailyFat[date] ?: 0.0
                    )
                }
            HealthResult.Success(intakes)
        } catch (e: Exception) {
            HealthResult.Failure(HealthDataError.QueryError(e))
        }
    }

    private suspend fun queryDailyQuantity(
        quantityType: HKQuantityType,
        predicate: NSPredicate,
        unit: HKUnit,
        formatter: NSDateFormatter
    ): Map<String, Double> = suspendCancellableCoroutine { cont ->
        val query = HKSampleQuery(
            sampleType = quantityType,
            predicate = predicate,
            limit = 0u,
            sortDescriptors = null,
            resultsHandler = { _, samples, error ->
                if (error != null) {
                    cont.resumeWith(Result.failure(Exception(error.localizedDescription)))
                } else {
                    val dailyValues = mutableMapOf<String, Double>()
                    samples?.forEach { sample ->
                        val quantitySample = sample as? HKQuantitySample ?: return@forEach
                        val value = quantitySample.quantity.doubleValueForUnit(unit)
                        val dateStr = formatter.stringFromDate(quantitySample.startDate)
                        dailyValues[dateStr] = (dailyValues[dateStr] ?: 0.0) + value
                    }
                    cont.resume(dailyValues)
                }
            }
        )
        healthStore.executeQuery(query)
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun readBasalMetabolicRate(
        startDate: String,
        endDate: String
    ): HealthResult<List<Metabolism>> {
        if (!HKHealthStore.isHealthDataAvailable()) {
            return HealthResult.Failure(HealthDataError.NotAvailable)
        }

        val basalType = HKQuantityType.quantityTypeForIdentifier(
            HKQuantityTypeIdentifierBasalEnergyBurned
        ) ?: return HealthResult.Failure(HealthDataError.NotAvailable)

        val authorized = requestAuthorization(setOf(basalType))
        if (!authorized) return HealthResult.Failure(HealthDataError.PermissionDenied)

        val formatter = NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd"
            timeZone = NSTimeZone.localTimeZone
        }

        val (startNSDate, endNSDate) = buildDateRange(startDate, endDate, formatter)
            ?: return HealthResult.Failure(HealthDataError.QueryError(Exception("Invalid date range")))

        val predicate = HKQuery.predicateForSamplesWithStartDate(
            startDate = startNSDate,
            endDate = endNSDate,
            options = HKQueryOptionNone
        )

        return suspendCancellableCoroutine { cont ->
            val query = HKSampleQuery(
                sampleType = basalType,
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
                            val dailyValues = mutableMapOf<String, MutableList<Double>>()
                            samples?.forEach { sample ->
                                val quantitySample = sample as? HKQuantitySample ?: return@forEach
                                val kcal = quantitySample.quantity
                                    .doubleValueForUnit(HKUnit.kilocalorieUnit())
                                val dateStr = formatter.stringFromDate(quantitySample.startDate)
                                dailyValues.getOrPut(dateStr) { mutableListOf() }.add(kcal)
                            }
                            val metabolisms = dailyValues.entries
                                .sortedBy { it.key }
                                .map { (date, values) ->
                                    Metabolism(date = date, bmr = values.average(), tef = 0.0)
                                }
                            cont.resume(HealthResult.Success(metabolisms))
                        }
                    }
                }
            )
            healthStore.executeQuery(query)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun writeNutritionRecord(entry: FoodDiaryEntry) {
        if (!HKHealthStore.isHealthDataAvailable()) return

        val energyType = HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierDietaryEnergyConsumed) ?: return
        val proteinType = HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierDietaryProtein) ?: return
        val carbType = HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierDietaryCarbohydrates) ?: return
        val fatType = HKQuantityType.quantityTypeForIdentifier(HKQuantityTypeIdentifierDietaryFatTotal) ?: return

        val shareTypes = setOf(energyType, proteinType, carbType, fatType).map { it as HKObjectType }.toSet()
        val authorized = suspendCancellableCoroutine<Boolean> { cont ->
            healthStore.requestAuthorizationToShareTypes(shareTypes, readTypes = null) { success, _ ->
                cont.resume(success)
            }
        }
        if (!authorized) return

        val formatter = NSDateFormatter().apply {
            dateFormat = "yyyy-MM-dd"
            timeZone = NSTimeZone.localTimeZone
        }
        val sampleDate = formatter.dateFromString(entry.date) ?: NSDate()
        val kcalUnit = HKUnit.kilocalorieUnit()
        val gramUnit = HKUnit.gramUnit()

        val samples = listOf(
            HKQuantitySample.quantitySampleWithType(
                energyType, HKQuantity.quantityWithUnit(kcalUnit, entry.calories), sampleDate, sampleDate
            ),
            HKQuantitySample.quantitySampleWithType(
                proteinType, HKQuantity.quantityWithUnit(gramUnit, entry.proteinG), sampleDate, sampleDate
            ),
            HKQuantitySample.quantitySampleWithType(
                carbType, HKQuantity.quantityWithUnit(gramUnit, entry.carbG), sampleDate, sampleDate
            ),
            HKQuantitySample.quantitySampleWithType(
                fatType, HKQuantity.quantityWithUnit(gramUnit, entry.fatG), sampleDate, sampleDate
            )
        )

        for (sample in samples) {
            healthStore.saveObject(sample) { _, _ -> }
        }
    }
}
