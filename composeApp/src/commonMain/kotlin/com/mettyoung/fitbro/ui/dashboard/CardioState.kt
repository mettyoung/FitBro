package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.todayString

data class CardioState(
    val sessions: List<CardioSession> = emptyList(),
    val totalMinutes: Int = 0,
    val dateRange: DateRange = DateRange(todayString().minusDays(6), todayString())
)
