package com.mettyoung.fitbro.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardMissingDataTest {

    @Test
    fun noFailures_noWarnings() {
        val warnings = buildWarnings(
            healthIntakeFailed = false,
            healthMetabolismFailed = false,
            healthFailed = false
        )
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun healthFailed_activityWarning() {
        val warnings = buildWarnings(
            healthIntakeFailed = false,
            healthMetabolismFailed = false,
            healthFailed = true
        )
        assertEquals(1, warnings.size)
        assertTrue("Activity" in warnings[0])
    }

    @Test
    fun healthIntakeFailed_intakeWarning() {
        val warnings = buildWarnings(
            healthIntakeFailed = true,
            healthMetabolismFailed = false,
            healthFailed = false
        )
        assertEquals(1, warnings.size)
        assertTrue("intake" in warnings[0].lowercase())
    }

    @Test
    fun healthMetabolismFailed_bmrWarning() {
        val warnings = buildWarnings(
            healthIntakeFailed = false,
            healthMetabolismFailed = true,
            healthFailed = false
        )
        assertEquals(1, warnings.size)
        assertTrue("BMR" in warnings[0])
    }

    @Test
    fun bothHealthDataFailed_twoWarnings() {
        val warnings = buildWarnings(
            healthIntakeFailed = true,
            healthMetabolismFailed = true,
            healthFailed = false
        )
        assertEquals(2, warnings.size)
        assertTrue(warnings.any { "intake" in it.lowercase() })
        assertTrue(warnings.any { "BMR" in it })
    }

    @Test
    fun allSourcesFailed_threeWarnings() {
        val warnings = buildWarnings(
            healthIntakeFailed = true,
            healthMetabolismFailed = true,
            healthFailed = true
        )
        assertEquals(3, warnings.size)
        assertTrue(warnings.any { "intake" in it.lowercase() })
        assertTrue(warnings.any { "BMR" in it })
        assertTrue(warnings.any { "Activity" in it })
    }

    @Test
    fun healthIntakeAndHealthFailed_twoWarnings() {
        val warnings = buildWarnings(
            healthIntakeFailed = true,
            healthMetabolismFailed = false,
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
