package com.mettyoung.fitbro.data.model

/**
 * Everything the platform health store holds about one workout, read on demand when its detail
 * page opens. Each related measurement carries the app(s) that wrote it, since e.g. a Hevy session
 * can get its heart rate from Mi Fitness.
 */
data class WorkoutDetail(
    val session: CardioSession,
    val endEpochMs: Long,
    val title: String?,
    val notes: String?,
    /** Raw source id: Android package name / HealthKit bundle id. */
    val sourceId: String?,
    /** "Auto-detected", "Tracked in app", "Manual entry", or null when unknown. */
    val recordingMethod: String?,
    /** e.g. "Xiaomi Smart Band 9 (watch)"; null when the source didn't say. */
    val device: String?,
    val lastModifiedEpochMs: Long?,
    val activeKcal: Measured<Double>?,
    val heartRate: Measured<HeartRateSummary>?,
    /** False when heart-rate read access isn't granted (so "none" ≠ "not allowed"). */
    val heartRateAllowed: Boolean,
    val segments: List<WorkoutSegment>,
    val laps: Int,
    val hasRoute: Boolean
)

/** A value plus the app(s) it came from. */
data class Measured<T>(val value: T, val sources: List<String>)

data class HeartRateSummary(val avgBpm: Long, val minBpm: Long, val maxBpm: Long, val samples: Long)

/** One exercise block inside a session, e.g. "Bench press × 12". */
data class WorkoutSegment(val name: String, val startEpochMs: Long, val endEpochMs: Long, val repetitions: Int)

val CardioSession.endEpochMs: Long get() = startEpochMs + minutes * 60_000L

/**
 * Other sessions whose time range overlaps [session] — typically the same gym visit recorded
 * by two apps (e.g. Hevy and the Mi Fitness watch).
 */
fun overlappingSessions(session: CardioSession, all: List<CardioSession>): List<CardioSession> =
    all.filter {
        it.id != session.id &&
            it.startEpochMs < session.endEpochMs &&
            session.startEpochMs < it.endEpochMs
    }

/** The selected week's strength sessions (any hidden-types setting) minus excluded apps, newest first. */
fun strengthSessions(
    sessions: List<CardioSession>,
    week: Week,
    excludedSources: Set<String> = emptySet()
): List<CardioSession> =
    sessions.filter { week.contains(it.date) && it.type.category == WorkoutCategory.STRENGTH && it.source !in excludedSources }
        .sortedByDescending { it.startEpochMs }

/** Apps that wrote strength sessions in [sessions], plus [alsoInclude] (e.g. excluded ones not seen lately). */
fun strengthSources(sessions: List<CardioSession>, alsoInclude: Set<String> = emptySet()): List<String> =
    (sessions.filter { it.type.category == WorkoutCategory.STRENGTH }.map { it.source } + alsoInclude)
        .distinct().sorted()
