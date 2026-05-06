package com.mettyoung.fitbro.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardMissingDataTest {

    @Test
    fun noFailures_noWarnings() {
        val warnings = buildWarnings(
            cronometerIntakeFailed = false,
            cronometerMetabolismFailed = false,
            healthFailed = false
        )
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun healthFailed_activityWarning() {
        val warnings = buildWarnings(
            cronometerIntakeFailed = false,
            cronometerMetabolismFailed = false,
            healthFailed = true
        )
        assertEquals(1, warnings.size)
        assertTrue("Activity" in warnings[0])
    }

    @Test
    fun cronometerIntakeFailed_intakeWarning() {
        val warnings = buildWarnings(
            cronometerIntakeFailed = true,
            cronometerMetabolismFailed = false,
            healthFailed = false
        )
        assertEquals(1, warnings.size)
        assertTrue("intake" in warnings[0].lowercase())
    }

    @Test
    fun cronometerMetabolismFailed_metabolismWarning() {
        val warnings = buildWarnings(
            cronometerIntakeFailed = false,
            cronometerMetabolismFailed = true,
            healthFailed = false
        )
        assertEquals(1, warnings.size)
        assertTrue("etabolic" in warnings[0])
    }

    @Test
    fun bothCronometerFailed_singleCombinedWarning() {
        val warnings = buildWarnings(
            cronometerIntakeFailed = true,
            cronometerMetabolismFailed = true,
            healthFailed = false
        )
        // Combined into one Cronometer warning, not two separate ones
        assertEquals(1, warnings.size)
        assertTrue("Cronometer" in warnings[0])
    }

    @Test
    fun allSourcesFailed_threeWarnings() {
        val warnings = buildWarnings(
            cronometerIntakeFailed = true,
            cronometerMetabolismFailed = true,
            healthFailed = true
        )
        // Cronometer combined (1) + Health (1) = 2 warnings
        assertEquals(2, warnings.size)
        assertTrue(warnings.any { "Cronometer" in it })
        assertTrue(warnings.any { "Activity" in it })
    }

    @Test
    fun cronometerIntakeAndHealthFailed_twoWarnings() {
        val warnings = buildWarnings(
            cronometerIntakeFailed = true,
            cronometerMetabolismFailed = false,
            healthFailed = true
        )
        assertEquals(2, warnings.size)
        assertTrue(warnings.any { "intake" in it.lowercase() })
        assertTrue(warnings.any { "Activity" in it })
    }

    @Test
    fun successStateWithWarnings_warningsPreserved() {
        val state = DashboardUiState.Success(
            balances = emptyList(),
            warnings = listOf("Activity data unavailable")
        )
        assertFalse(state.warnings.isEmpty())
        assertEquals("Activity data unavailable", state.warnings[0])
    }

    @Test
    fun successStateDefaultWarnings_empty() {
        val state = DashboardUiState.Success(balances = emptyList())
        assertTrue(state.warnings.isEmpty())
    }
}
