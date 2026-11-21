package com.mettyoung.fitbro.data.sync

import com.mettyoung.fitbro.data.model.BodyMeasurement
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.CustomFood
import com.mettyoung.fitbro.data.model.CustomMeal
import com.mettyoung.fitbro.data.model.WorkoutDetail
import kotlinx.serialization.Serializable

/*
 * Wire format for the read-only mirror of the app's health data and preferences, shared with
 * mcp-server/src/sync.ts (`PUT /sync/body`, `/sync/workouts`, `/sync/library`, `preferences` in
 * `/sync/settings`). The app is the only writer; body and workout pushes replace their date range.
 */

@Serializable
internal data class PreferencesDto(
    /** WeekStart enum name. */
    val weekStart: String,
    val resistanceSessionsPerWeek: Int,
    val cardioMinutesPerWeek: Int,
    /** WorkoutType enum names. */
    val hiddenWorkoutTypes: List<String>,
    val excludedResistanceSources: List<String>
)

@Serializable
internal data class SettingsBodyV2(val goalsByWeekday: Map<String, GoalsDto>, val preferences: PreferencesDto)

@Serializable
internal data class BodyDto(val date: String, val weightKg: Double?, val bodyFatPct: Double?, val leanMassKg: Double?)

@Serializable
internal data class BodyBody(val start: String, val end: String, val days: List<BodyDto>)

@Serializable
internal data class HeartRateDto(val avgBpm: Long, val minBpm: Long, val maxBpm: Long, val samples: Long, val sources: List<String>)

@Serializable
internal data class SegmentDto(val name: String, val startMs: Long, val endMs: Long, val repetitions: Int)

@Serializable
internal data class WorkoutDetailDto(
    val title: String?,
    val notes: String?,
    val recordingMethod: String?,
    val device: String?,
    val activeKcal: Double?,
    val activeKcalSources: List<String>,
    val heartRate: HeartRateDto?,
    val heartRateAllowed: Boolean,
    val segments: List<SegmentDto>,
    val laps: Int,
    val hasRoute: Boolean
)

@Serializable
internal data class WorkoutDto(
    val id: String,
    val date: String,
    val startMs: Long,
    val minutes: Int,
    val activity: String,
    /** WorkoutType enum name. */
    val type: String,
    /** WorkoutCategory enum name. */
    val category: String,
    val source: String,
    /** Null = not read yet; the server keeps any detail it already has. */
    val detail: WorkoutDetailDto?
)

@Serializable
internal data class WorkoutsBody(val start: String, val end: String, val workouts: List<WorkoutDto>)

@Serializable
internal data class FoodDto(
    val name: String,
    val brandName: String?,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val servingSizeG: Double
)

@Serializable
internal data class MealItemDto(
    val name: String,
    val brandName: String?,
    val calories: Double,
    val proteinG: Double,
    val carbG: Double,
    val fatG: Double,
    val servingSizeG: Double,
    val servingUnit: String
)

@Serializable
internal data class MealDto(val name: String, val items: List<MealItemDto>)

@Serializable
internal data class LibraryBody(val customFoods: List<FoodDto>, val customMeals: List<MealDto>)

/** One row per date (the last reading of a day wins, like the app's trend maths), within [start]..[end]. */
internal fun bodyBody(start: String, end: String, measurements: List<BodyMeasurement>) = BodyBody(
    start = start,
    end = end,
    days = measurements.filter { it.date in start..end }
        .associateBy { it.date }.values
        .filter { it.weightKg != null || it.bodyFatPct != null || it.leanMassKg != null }
        .map { BodyDto(it.date, it.weightKg, it.bodyFatPct, it.leanMassKg) }
)

internal fun CardioSession.toDto(detail: WorkoutDetail?) = WorkoutDto(
    id = id,
    date = date,
    startMs = startEpochMs,
    minutes = minutes,
    activity = activity,
    type = type.name,
    category = type.category.name,
    source = source,
    detail = detail?.toDto()
)

internal fun WorkoutDetail.toDto() = WorkoutDetailDto(
    title = title,
    notes = notes,
    recordingMethod = recordingMethod,
    device = device,
    activeKcal = activeKcal?.value,
    activeKcalSources = activeKcal?.sources ?: emptyList(),
    heartRate = heartRate?.let { HeartRateDto(it.value.avgBpm, it.value.minBpm, it.value.maxBpm, it.value.samples, it.sources) },
    heartRateAllowed = heartRateAllowed,
    segments = segments.map { SegmentDto(it.name, it.startEpochMs, it.endEpochMs, it.repetitions) },
    laps = laps,
    hasRoute = hasRoute
)

internal fun libraryBody(foods: List<CustomFood>, meals: List<CustomMeal>) = LibraryBody(
    customFoods = foods.map { FoodDto(it.name, it.brandName, it.calories, it.proteinG, it.carbG, it.fatG, it.servingSizeG) },
    customMeals = meals.map { m ->
        MealDto(m.name, m.items.sortedBy { it.sortOrder }.map {
            MealItemDto(it.foodName, it.brandName, it.calories, it.proteinG, it.carbG, it.fatG, it.servingSizeG, it.servingUnit)
        })
    }
)
