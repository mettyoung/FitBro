package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.WorkoutType
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.todayString

data class CardioState(
    /** Visible sessions only (hidden workout types removed). */
    val sessions: List<CardioSession> = emptyList(),
    val totalMinutes: Int = 0,
    /** Sessions in range filtered out by [hiddenTypes]. */
    val hiddenCount: Int = 0,
    val hiddenTypes: Set<WorkoutType> = emptySet(),
    val dateRange: DateRange = DateRange(todayString().minusDays(6), todayString()),
    val isLoading: Boolean = false,
    val error: String? = null
)
