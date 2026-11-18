package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.Week
import com.mettyoung.fitbro.data.model.WorkoutDetail
import com.mettyoung.fitbro.data.model.WeekStart
import com.mettyoung.fitbro.data.model.WorkoutType
import com.mettyoung.fitbro.util.todayString

data class CardioState(
    /** Selected week's visible sessions (hidden workout types removed). */
    val sessions: List<CardioSession> = emptyList(),
    val totalMinutes: Int = 0,
    /** Strength sessions in the selected week, ignoring hidden types (Dashboard resistance card). */
    val resistanceCount: Int = 0,
    /** Selected week's strength sessions regardless of hidden types, newest first (Resistance page). */
    val strengthSessions: List<CardioSession> = emptyList(),
    /** Apps that wrote strength sessions (loaded range) or are excluded; for the sources setting. */
    val resistanceSources: List<String> = emptyList(),
    /** Apps whose strength sessions don't count toward resistance. */
    val excludedResistanceSources: Set<String> = emptySet(),
    /** Workout whose detail page is open, or null. */
    val workoutDetail: WorkoutDetailState? = null,
    /** Previous week's visible minutes through the same weekday as today (FR-9). */
    val previousWeekMinutes: Int = 0,
    /** Previous week's visible sessions (all 7 days), for week-over-week comparisons. */
    val previousWeekSessions: List<CardioSession> = emptyList(),
    /** Sessions in range filtered out by [hiddenTypes]. */
    val hiddenCount: Int = 0,
    val hiddenTypes: Set<WorkoutType> = emptySet(),
    val week: Week = Week.of(todayString(), WeekStart.MONDAY),
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed interface WorkoutDetailState {
    val session: CardioSession

    data class Loading(override val session: CardioSession) : WorkoutDetailState
    data class Loaded(
        val detail: WorkoutDetail,
        /** Same time window recorded by another app (e.g. Hevy + Mi Fitness watch). */
        val overlapping: List<CardioSession>
    ) : WorkoutDetailState {
        override val session: CardioSession get() = detail.session
    }
    data class Failed(override val session: CardioSession, val message: String) : WorkoutDetailState
}
