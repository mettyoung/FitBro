package com.mettyoung.fitbro.data.model

import kotlin.test.Test
import kotlin.test.assertEquals

class WorkoutDetailTest {
    private fun s(id: String, date: String, startMin: Long, minutes: Int, type: WorkoutType, source: String) =
        CardioSession(id, date, startMin * 60_000L, minutes, type.label, type, source)

    @Test
    fun overlapFindsSameVisitFromOtherApp() {
        val hevy = s("h", "2026-11-18", 1000, 60, WorkoutType.STRENGTH_TRAINING, "Hevy")
        val watch = s("w", "2026-11-18", 1010, 55, WorkoutType.STRENGTH_TRAINING, "Mi Fitness")
        val later = s("r", "2026-11-18", 1060, 30, WorkoutType.RUNNING, "Mi Fitness") // starts exactly at hevy's end
        assertEquals(listOf(watch), overlappingSessions(hevy, listOf(hevy, watch, later)))
    }

    @Test
    fun strengthSessionsIgnoreHiddenTypesAndSortNewestFirst() {
        val week = Week("2026-11-16")
        val a = s("a", "2026-11-16", 100, 45, WorkoutType.STRENGTH_TRAINING, "Hevy")
        val b = s("b", "2026-11-18", 200, 50, WorkoutType.WEIGHTLIFTING, "Mi Fitness")
        val run = s("c", "2026-11-17", 150, 30, WorkoutType.RUNNING, "Mi Fitness")
        val lastWeek = s("d", "2026-11-12", 50, 40, WorkoutType.STRENGTH_TRAINING, "Hevy")
        assertEquals(listOf(b, a), strengthSessions(listOf(a, b, run, lastWeek), week))
    }

    @Test
    fun excludedSourcesDropFromListCountButStayInSources() {
        val week = Week("2026-11-16")
        val hevy = s("h", "2026-11-18", 1000, 60, WorkoutType.STRENGTH_TRAINING, "Hevy")
        val watch = s("w", "2026-11-18", 1010, 55, WorkoutType.STRENGTH_TRAINING, "Mi Fitness")
        val all = listOf(hevy, watch)
        val excluded = setOf("Mi Fitness")
        assertEquals(listOf(hevy), strengthSessions(all, week, excluded))
        assertEquals(1, resistanceCount(all, week, excluded))
        assertEquals(listOf("Hevy", "Mi Fitness"), strengthSources(listOf(hevy), excluded))
    }
}
