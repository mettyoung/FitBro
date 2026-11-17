package com.mettyoung.fitbro.data.model

/**
 * An exercise session read from the health platform (Health Connect / HealthKit), e.g. a run
 * recorded by Mi Fitness. Read-only: FitBro no longer stores cardio itself.
 */
data class CardioSession(
    /** Platform record id (Health Connect metadata id / HealthKit UUID). */
    val id: String,
    /** Local start date, "YYYY-MM-DD". */
    val date: String,
    val startEpochMs: Long,
    val minutes: Int,
    /** Human-readable activity, e.g. "Running" or the session title from the source app. */
    val activity: String,
    val type: WorkoutType,
    /** App that recorded it, e.g. "Mi Fitness". */
    val source: String
)
