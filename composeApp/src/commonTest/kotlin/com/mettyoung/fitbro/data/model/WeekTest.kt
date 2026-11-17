package com.mettyoung.fitbro.data.model

import com.mettyoung.fitbro.ui.dashboard.formatDelta
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WeekTest {

    // 2026-11-17 is a Tuesday.
    private val tuesday = "2026-11-17"

    @Test
    fun weekOfFindsTheConfiguredStartDay() {
        assertEquals(Week("2026-11-16"), Week.of(tuesday, WeekStart.MONDAY))
        assertEquals(Week("2026-11-15"), Week.of(tuesday, WeekStart.SUNDAY))
        assertEquals(Week("2026-11-11"), Week.of(tuesday, WeekStart.WEDNESDAY))
        assertEquals(Week("2026-11-17"), Week.of(tuesday, WeekStart.TUESDAY), "start day itself")
        assertEquals(Week("2026-11-16"), Week.of("2026-11-22", WeekStart.MONDAY), "Sunday belongs to Monday week")
        assertEquals(Week("2026-11-22"), Week.of("2026-11-22", WeekStart.SUNDAY))
    }

    @Test
    fun weekOfHandlesYearBoundaryAndLeapDay() {
        assertEquals(Week("2026-12-28"), Week.of("2027-01-01", WeekStart.MONDAY))
        val leap = Week.of("2028-02-29", WeekStart.MONDAY)
        assertEquals("2028-02-28", leap.start)
        assertEquals("2028-03-05", leap.end)
        assertTrue(leap.contains("2028-02-29"))
    }

    @Test
    fun navigationAndDays() {
        val week = Week("2026-11-16")
        assertEquals(Week("2026-11-09"), week.previous())
        assertEquals(Week("2026-11-23"), week.next())
        assertEquals(listOf("2026-11-16", "2026-11-17", "2026-11-18", "2026-11-19", "2026-11-20", "2026-11-21", "2026-11-22"), week.days())
        assertFalse(week.contains("2026-11-23"))
    }

    @Test
    fun averagesExcludeTodayForTheCurrentWeek() {
        val week = Week("2026-11-16")
        assertEquals(listOf("2026-11-16", "2026-11-17"), week.averagingDays("2026-11-18"))
        assertEquals(emptyList(), week.averagingDays("2026-11-16"), "first day: nothing completed yet")
        assertEquals(7, Week("2026-11-09").averagingDays("2026-11-18").size, "past week: all days")
    }

    @Test
    fun likeForLikeComparesCompletedDaysByDefault() {
        val week = Week("2026-11-16")
        // Wednesday: Mon–Tue completed → compare Mon–Tue last week.
        assertEquals(listOf("2026-11-09", "2026-11-10"), comparisonDays(week, "2026-11-18"))
        assertEquals("Mon–Tue", partialWeekSpan(week, "2026-11-18", WeekStart.MONDAY))
        assertEquals("Sun–Mon", partialWeekSpan(Week("2026-11-15"), tuesday, WeekStart.SUNDAY))
        // First day of the week: nothing to compare yet.
        assertEquals(emptyList(), comparisonDays(week, "2026-11-16"))
        assertEquals(0, comparedDayCount(week, "2026-11-16"))
        assertNull(partialWeekSpan(week, "2026-11-16", WeekStart.MONDAY))
        // Past week compares full weeks.
        assertEquals(7, comparisonDays(Week("2026-11-09"), "2026-11-18").size)
        assertNull(partialWeekSpan(Week("2026-11-09"), tuesday, WeekStart.MONDAY))
    }

    @Test
    fun runningTotalsCompareThroughToday() {
        val week = Week("2026-11-16")
        assertEquals(listOf("2026-11-09", "2026-11-10", "2026-11-11"), comparisonDays(week, "2026-11-18", includeToday = true))
        assertEquals("Mon–Wed", partialWeekSpan(week, "2026-11-18", WeekStart.MONDAY, includeToday = true))
        assertEquals("Mon", partialWeekSpan(week, "2026-11-16", WeekStart.MONDAY, includeToday = true))
    }

    @Test
    fun cardioMinutesOnDays() {
        fun s(date: String, min: Int) = CardioSession("id$date", date, 0, min, "Run", WorkoutType.RUNNING, "Mi Fitness")
        val sessions = listOf(s("2026-11-16", 30), s("2026-11-17", 20), s("2026-11-18", 45))
        assertEquals(50, sessions.minutesOn(Week("2026-11-16").averagingDays("2026-11-18")))
    }

    @Test
    fun labels() {
        assertEquals("This week", Week("2026-11-16").label(tuesday, WeekStart.MONDAY))
        assertEquals("Last week", Week("2026-11-09").label(tuesday, WeekStart.MONDAY))
        assertEquals("Nov 2 – Nov 8", Week("2026-11-02").label(tuesday, WeekStart.MONDAY))
        assertEquals("Dec 29 – Jan 4, 2026", Week("2025-12-29").label(tuesday, WeekStart.MONDAY))
    }

    @Test
    fun weekStartParseFallsBackToMonday() {
        assertEquals(WeekStart.SUNDAY, WeekStart.parse("SUNDAY"))
        assertEquals(WeekStart.MONDAY, WeekStart.parse(null))
        assertEquals(WeekStart.MONDAY, WeekStart.parse("FUNDAY"))
    }

    private fun day(date: String, intake: Double, burn: Double = 2400.0, protein: Double = 150.0) =
        DailyBalance(date = date, intake = intake, burn = burn, balance = intake - burn, proteinG = protein, carbG = 200.0, fatG = 60.0)

    @Test
    fun summaryAveragesLoggedDaysOnly() {
        val week = Week("2026-11-16")
        val balances = listOf(
            day("2026-11-16", 2000.0),
            day("2026-11-17", 2200.0, protein = 170.0),
            day("2026-11-18", 0.0), // nothing logged → excluded
            day("2026-11-09", 9999.0) // other week → excluded
        )
        val s = summarizeWeek(balances, week.days(), cardioMinutes = 45)
        assertEquals(2, s.loggedDays)
        assertEquals(2100.0, s.avgIntake)
        assertEquals(2400.0, s.avgBurn)
        assertEquals(-300.0, s.avgBalance)
        assertEquals(-600.0, s.netBalance)
        assertEquals(160.0, s.avgProteinG)
        assertEquals(200.0, s.avgCarbG)
        assertEquals(45, s.cardioMinutes)
    }

    @Test
    fun emptyWeekHasNullAverages() {
        val s = summarizeWeek(emptyList(), Week("2026-11-16").days(), cardioMinutes = 30)
        assertEquals(0, s.loggedDays)
        assertNull(s.avgIntake)
        assertNull(s.netBalance)
        assertEquals(30, s.cardioMinutes)
    }

    @Test
    fun comparisonDeltas() {
        val current = summarizeWeek(listOf(day("2026-11-16", 2100.0)), Week("2026-11-16").days(), 60)
        val previous = summarizeWeek(listOf(day("2026-11-09", 2000.0)), Week("2026-11-09").days(), 90)
        val rows = compareWeeks(current, previous).associateBy { it.metric }
        assertEquals(WeekMetric.entries.size, rows.size)
        assertEquals(100.0, rows.getValue(WeekMetric.AVG_INTAKE).delta)
        assertEquals(-30.0, rows.getValue(WeekMetric.CARDIO_MINUTES).delta)

        val none = compareWeeks(current, WeekSummary.EMPTY).associateBy { it.metric }
        assertNull(none.getValue(WeekMetric.AVG_INTAKE).delta, "no data last week → no delta")
        assertEquals(60.0, none.getValue(WeekMetric.CARDIO_MINUTES).delta, "cardio 0 is real data")
    }

    @Test
    fun carbsAndFatAreTheOnlyOptionalMetrics() {
        assertEquals(setOf(WeekMetric.AVG_CARBS, WeekMetric.AVG_FAT), WeekMetric.entries.filter { it.optional }.toSet())
    }

    @Test
    fun deltaFormatting() {
        assertEquals("▲ 1,200 kcal", formatDelta(1200.4, "kcal"))
        assertEquals("▼ 8 g", formatDelta(-8.0, "g"))
        assertEquals("same", formatDelta(0.3, "min"))
        assertEquals("—", formatDelta(null, "kcal"))
    }
}
