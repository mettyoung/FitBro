package com.mettyoung.fitbro.data.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WorkoutTypeTest {

    private fun session(id: String, type: WorkoutType, minutes: Int = 30) = CardioSession(
        id = id, date = "2026-11-17", startEpochMs = 0, minutes = minutes,
        activity = type.label, type = type, source = "Mi Fitness"
    )

    @Test
    fun visibleRemovesHiddenTypesOnly() {
        val all = listOf(
            session("1", WorkoutType.RUNNING),
            session("2", WorkoutType.STRENGTH_TRAINING),
            session("3", WorkoutType.CYCLING)
        )
        assertEquals(listOf("1", "3"), all.visible(setOf(WorkoutType.STRENGTH_TRAINING)).map { it.id })
    }

    @Test
    fun emptyHiddenSetKeepsEverything() {
        val all = listOf(session("1", WorkoutType.YOGA), session("2", WorkoutType.OTHER))
        assertEquals(all, all.visible(emptySet()))
    }

    @Test
    fun neverSavedFallsBackToStrengthAndMindBodyDefaults() {
        val hidden = WorkoutType.parseHidden(null)
        assertEquals(WorkoutType.DEFAULT_HIDDEN, hidden)
        assertTrue(WorkoutType.STRENGTH_TRAINING in hidden)
        assertTrue(WorkoutType.YOGA in hidden)
        assertTrue(WorkoutType.RUNNING !in hidden)
        assertTrue(hidden.all { it.category == WorkoutCategory.STRENGTH || it.category == WorkoutCategory.MIND_BODY })
    }

    @Test
    fun savedEmptyMeansNothingHidden() {
        assertEquals(emptySet(), WorkoutType.parseHidden(""))
    }

    @Test
    fun roundTripsAndIgnoresUnknownNames() {
        val types = setOf(WorkoutType.WALKING, WorkoutType.OTHER)
        assertEquals(types, WorkoutType.parseHidden(WorkoutType.serializeHidden(types)))
        assertEquals(setOf(WorkoutType.YOGA), WorkoutType.parseHidden("YOGA,PADEL_FROM_THE_FUTURE"))
    }
}
