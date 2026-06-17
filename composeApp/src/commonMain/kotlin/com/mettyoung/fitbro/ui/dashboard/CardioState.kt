package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.model.CardioSession

data class CardioState(
    val sessions: List<CardioSession> = emptyList(),
    val weeklyTotalMinutes: Int = 0
)
