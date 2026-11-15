package com.mettyoung.fitbro.ui.dashboard

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DashboardMissingDataTest {

    @Test
    fun noFailures_noWarnings() {
        val warnings = buildWarnings(
            healthMetabolismFailed = false,
            healthFailed = false
        )
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun healthFailed_activityWarning() {
        val warnings = buildWarnings(
            healthMetabolismFailed = false,
            healthFailed = true
        )
        assertEquals(1, warnings.size)
        assertTrue("Activity" in warnings[0])
    }

    @Test
    fun healthMetabolismFailed_bmrWarning() {
        val warnings = buildWarnings(
            healthMetabolismFailed = true,
            healthFailed = false
        )
        assertEquals(1, warnings.size)
        assertTrue("BMR" in warnings[0])
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
