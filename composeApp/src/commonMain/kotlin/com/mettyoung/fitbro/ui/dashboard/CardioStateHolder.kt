package com.mettyoung.fitbro.ui.dashboard

import com.mettyoung.fitbro.data.repository.CardioRepository
import com.mettyoung.fitbro.util.minusDays
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CardioStateHolder(
    private val repository: CardioRepository,
    private val scope: CoroutineScope
) {
    private val _dateRange = MutableStateFlow(
        DateRange(todayString().minusDays(6), todayString())
    )

    val state: StateFlow<CardioState> = _dateRange
        .flatMapLatest { range ->
            repository.sessionsForRange(range.startDate, range.endDate)
                .map { sessions ->
                    CardioState(
                        sessions = sessions,
                        totalMinutes = sessions.sumOf { it.minutes }
                    )
                }
        }
        .stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CardioState()
        )

    fun setDateRange(dateRange: DateRange) {
        _dateRange.value = dateRange
    }

    fun logSession(date: String, minutes: Int, note: String?): Job = scope.launch {
        repository.logSession(date, minutes, note)
    }

    fun updateSession(id: Long, date: String, minutes: Int, note: String?): Job = scope.launch {
        repository.updateSession(id, date, minutes, note)
    }

    fun deleteSession(id: Long): Job = scope.launch {
        repository.deleteSession(id)
    }
}
