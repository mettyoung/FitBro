package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.Week
import com.mettyoung.fitbro.data.model.WeekStart
import com.mettyoung.fitbro.data.model.WorkoutType
import com.mettyoung.fitbro.util.todayString

data class CardioState(
    /** Selected week's visible sessions (hidden workout types removed). */
    val sessions: List<CardioSession> = emptyList(),
    val totalMinutes: Int = 0,
    /** Previous week's visible minutes, like-for-like with the selected week (FR-9). */
    val previousWeekMinutes: Int = 0,
    /** Sessions in range filtered out by [hiddenTypes]. */
    val hiddenCount: Int = 0,
    val hiddenTypes: Set<WorkoutType> = emptySet(),
    val week: Week = Week.of(todayString(), WeekStart.MONDAY),
    val isLoading: Boolean = false,
    val error: String? = null
)
