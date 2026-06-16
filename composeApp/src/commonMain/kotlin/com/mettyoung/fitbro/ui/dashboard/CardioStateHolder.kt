package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.repository.CardioRepository
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CardioStateHolder(
    private val repository: CardioRepository,
    private val scope: CoroutineScope
) {
    private val today = todayString()
    private val startDate = today.minusDays(6)

    val state: StateFlow<CardioState> = repository.sessionsForRange(startDate, today)
        .map { sessions ->
            CardioState(
                sessions = sessions,
                weeklyTotalMinutes = sessions.sumOf { it.minutes }
            )
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CardioState()
        )
}
