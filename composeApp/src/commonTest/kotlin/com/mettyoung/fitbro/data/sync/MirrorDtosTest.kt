package com.mettyoung.fitbro.data.sync

import com.mettyoung.fitbro.data.model.BodyMeasurement
import com.mettyoung.fitbro.data.model.CardioSession
import com.mettyoung.fitbro.data.model.CustomMeal
import com.mettyoung.fitbro.data.model.CustomMealItem
import com.mettyoung.fitbro.data.model.HeartRateSummary
import com.mettyoung.fitbro.data.model.Measured
import com.mettyoung.fitbro.data.model.WorkoutDetail
import com.mettyoung.fitbro.data.model.WorkoutSegment
import com.mettyoung.fitbro.data.model.WorkoutType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MirrorDtosTest {
    private val session = CardioSession(
        id = "hc-1",
        date = "2026-11-17",
        startEpochMs = 1_000L,
        minutes = 38,
        activity = "Group 3: Pull",
        type = WorkoutType.STRENGTH_TRAINING,
        source = "Hevy"
    )

    @Test
    fun bodyKeepsOneRowPerDateInRangeAndDropsEmptyDays() {
        val body = bodyBody(
            "2026-11-01", "2026-11-30",
            listOf(
                BodyMeasurement("2026-10-31", weightKg = 66.0),
                BodyMeasurement("2026-11-02", weightKg = 66.0),
                BodyMeasurement("2026-11-02", weightKg = 65.8, bodyFatPct = 20.0),
                BodyMeasurement("2026-11-03")
            )
        )
        assertEquals(listOf(BodyDto("2026-11-02", 65.8, 20.0, null)), body.days)
    }

    @Test
    fun workoutCarriesTypeCategoryAndDetail() {
        val detail = WorkoutDetail(
            session = session,
            endEpochMs = 2_000L,
            title = "Group 3: Pull",
            notes = null,
            sourceId = "com.hevy",
            recordingMethod = "Tracked in app",
            device = null,
            lastModifiedEpochMs = null,
            activeKcal = Measured(210.0, listOf("Hevy")),
            heartRate = Measured(HeartRateSummary(121, 80, 160, 300), listOf("Mi Fitness")),
            heartRateAllowed = true,
            segments = listOf(WorkoutSegment("Pull-up", 0, 120_000, 10)),
            laps = 0,
            hasRoute = false
        )
        val dto = session.toDto(detail)
        assertEquals("STRENGTH_TRAINING", dto.type)
        assertEquals("STRENGTH", dto.category)
        assertEquals(listOf("Mi Fitness"), dto.detail?.heartRate?.sources)
        assertEquals(210.0, dto.detail?.activeKcal)
        assertEquals(SegmentDto("Pull-up", 0, 120_000, 10), dto.detail?.segments?.single())
        assertNull(session.toDto(null).detail)
    }

    @Test
    fun mealItemsKeepTheirOrder() {
        val item = { name: String, order: Long ->
            CustomMealItem(foodName = name, calories = 1.0, proteinG = 0.0, carbG = 0.0, fatG = 0.0, servingSizeG = 1.0, servingUnit = "g", sortOrder = order)
        }
        val lib = libraryBody(emptyList(), listOf(CustomMeal(name = "Usual", createdAt = "", items = listOf(item("B", 1), item("A", 0)))))
        assertEquals(listOf("A", "B"), lib.customMeals.single().items.map { it.name })
    }
}
