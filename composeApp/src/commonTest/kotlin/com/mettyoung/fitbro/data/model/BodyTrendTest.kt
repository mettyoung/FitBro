package com.mettyoung.fitbro.data.model

import com.mettyoung.fitbro.util.plusDays
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BodyTrendTest {

    private fun days(start: String, n: Int) = (0 until n).map { start.plusDays(it) }

    @Test
    fun trendSeedsWithFirstReadingAndCarriesForward() {
        val d = days("2026-11-01", 5)
        val t = trendSeries(mapOf("2026-11-02" to 70.0, "2026-11-04" to 60.0), d)
        assertNull(t[0], "before first weigh-in")
        assertEquals(70.0, t[1])
        assertEquals(70.0, t[2], "gap carries forward")
        assertEquals(69.0, t[3]!!, 1e-9) // 70 + 0.1 × (60 − 70)
        assertEquals(69.0, t[4]!!, 1e-9)
    }

    @Test
    fun bodyFatKgNeedsWeightAndPercent() {
        assertEquals(12.0, bodyFatKg(BodyMeasurement("d", weightKg = 80.0, bodyFatPct = 15.0))!!, 1e-9)
        assertNull(bodyFatKg(BodyMeasurement("d", bodyFatPct = 15.0)))
        assertNull(bodyFatKg(BodyMeasurement("d", weightKg = 80.0)))
    }

    @Test
    fun trendWindowEndsOnLastCompletedDay() {
        val week = Week("2026-11-16")
        val current = trendWindow(week, "2026-11-18")
        assertEquals(28, current.size)
        assertEquals("2026-11-17", current.last())
        assertEquals("2026-10-21", current.first())
        assertEquals("2026-11-15", trendWindow(week, "2026-11-16").last(), "first day → day before the week")
        assertEquals("2026-11-22", trendWindow(week, "2026-12-01").last(), "past week → its last day")
        val read = trendReadDays(week, "2026-11-18")
        assertEquals(week.start.plusDays(-56), read.first())
        assertEquals("2026-11-17", read.last())
        assertTrue(read.indexOf(current.first()) >= 28, "≥ 28 days of warm-up")
    }

    @Test
    fun summaryValueDeltaAverage() {
        val read = days("2026-10-01", 10)
        val window = read.takeLast(3)
        val m = listOf(
            BodyMeasurement("2026-10-01", weightKg = 70.0),
            BodyMeasurement("2026-10-09", weightKg = 60.0),
        )
        val s = summarizeTrend(m, BodyMetric.WEIGHT, read, window)
        assertEquals(listOf(70.0, 69.0, 69.0), s.trend.map { it!! })
        assertEquals(69.0, s.value!!, 1e-9)
        assertEquals(-1.0, s.delta!!, 1e-9)
        assertEquals(listOf("2026-10-09" to 60.0), s.weighIns)
        assertNull(summarizeTrend(emptyList(), BodyMetric.LEAN_MASS, read, window).value)
    }

    @Test
    fun estimatedExpenditureFromIntakeAndTrend() {
        val window = days("2026-10-01", 28)
        val intake = window.associateWith { 2000.0 }
        // Lose 1 kg of trend over 21 days: EE = 2000 + 7700 / 21.
        val points = mapOf(window[0] to 80.0, window[21] to 79.0)
        val trend = mapOf(window[0] to 80.0, window[21] to 79.0)
        val ee = assertNotNull(estimateExpenditure(intake, points, trend, window))
        assertEquals(2000.0 + 7700.0 / 21, ee.kcalPerDay, 1e-6)
        assertEquals(21, ee.spanDays)
        assertEquals(28, ee.loggedDays)
    }

    @Test
    fun estimatedExpenditureNeedsEnoughData() {
        val window = days("2026-10-01", 28)
        val points = mapOf(window[0] to 80.0, window[21] to 79.0)
        val tooFewLogs = window.take(13).associateWith { 2000.0 }
        assertNull(estimateExpenditure(tooFewLogs, points, points, window))
        val full = window.associateWith { 2000.0 }
        assertNull(estimateExpenditure(full, mapOf(window[0] to 80.0), points, window), "single weigh-in")
        val close = mapOf(window[0] to 80.0, window[10] to 79.0)
        assertNull(estimateExpenditure(full, close, close, window), "weigh-ins < 14 days apart")
    }

    @Test
    fun weightGainMeansExpenditureBelowIntake() {
        val window = days("2026-10-01", 28)
        val pts = mapOf(window[0] to 70.0, window[27] to 71.0)
        val ee = assertNotNull(estimateExpenditure(window.associateWith { 2500.0 }, pts, pts, window))
        assertTrue(ee.kcalPerDay < 2500.0)
    }

    @Test
    fun weekToDateBalanceSumsLoggedDays() {
        fun b(d: String, intake: Double, burn: Double) = DailyBalance(d, intake, burn, intake - burn, 0.0, 0.0, 0.0)
        val bal = listOf(b("2026-11-16", 2000.0, 2700.0), b("2026-11-17", 0.0, 2500.0), b("2026-11-18", 2200.0, 2740.0))
        val w = assertNotNull(weekToDateBalance(bal, listOf("2026-11-16", "2026-11-17")))
        assertEquals(-700.0, w.total)
        assertEquals(1, w.loggedDays)
        assertNull(weekToDateBalance(bal, emptyList()), "first day of week")
    }

    @Test
    fun resistanceCountsStrengthOnlyInWeek() {
        fun s(date: String, type: WorkoutType) = CardioSession("id$date$type", date, 0, 45, type.label, type, "Mi Fitness")
        val sessions = listOf(
            s("2026-11-16", WorkoutType.STRENGTH_TRAINING),
            s("2026-11-18", WorkoutType.WEIGHTLIFTING),
            s("2026-11-18", WorkoutType.HIIT),
            s("2026-11-09", WorkoutType.STRENGTH_TRAINING)
        )
        assertEquals(2, resistanceCount(sessions, Week("2026-11-16")))
    }
}
