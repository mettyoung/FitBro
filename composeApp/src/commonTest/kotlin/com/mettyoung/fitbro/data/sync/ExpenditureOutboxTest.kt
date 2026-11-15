package com.mettyoung.fitbro.data.sync

import com.mettyoung.fitbro.data.model.DailyBalance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpenditureOutboxTest {

    private fun day(date: String, burn: Double = 2400.0) =
        DailyBalance(date = date, intake = 1800.0, burn = burn, balance = 1800.0 - burn, bmr = 1600.0)

    @Test
    fun offeredDaysArePendingWithTheirTimestamp() {
        val outbox = ExpenditureOutbox()
        outbox.offer(listOf(day("2026-11-13"), day("2026-11-14")), at = 10)
        assertEquals(listOf(day("2026-11-13") to 10L, day("2026-11-14") to 10L), outbox.pending())
    }

    @Test
    fun unchangedDaysAreNotResentAfterASuccessfulPush() {
        val outbox = ExpenditureOutbox()
        outbox.offer(listOf(day("2026-11-14")), at = 10)
        outbox.markSent(outbox.pending())
        assertTrue(outbox.pending().isEmpty())

        outbox.offer(listOf(day("2026-11-14")), at = 20)
        assertTrue(outbox.pending().isEmpty(), "same value as last push → skipped")

        outbox.offer(listOf(day("2026-11-14", burn = 2600.0)), at = 30)
        assertEquals(listOf(day("2026-11-14", burn = 2600.0) to 30L), outbox.pending())
    }

    @Test
    fun aNewerValueOfferedDuringAPushStaysPending() {
        val outbox = ExpenditureOutbox()
        outbox.offer(listOf(day("2026-11-14", burn = 2400.0)), at = 10)
        val inFlight = outbox.pending()
        outbox.offer(listOf(day("2026-11-14", burn = 2500.0)), at = 20)
        outbox.markSent(inFlight)
        assertEquals(listOf(day("2026-11-14", burn = 2500.0) to 20L), outbox.pending())
    }

    @Test
    fun failedPushKeepsDaysPending() {
        val outbox = ExpenditureOutbox()
        outbox.offer(listOf(day("2026-11-14")), at = 10)
        // No markSent (push failed) → still queued for the next sync.
        assertEquals(1, outbox.pending().size)
    }

    @Test
    fun dtoCarriesTheBreakdown() {
        val dto = day("2026-11-14").copy(tef = 180.0, neat = 400.0, eat = 220.0).toExpenditureDto(updatedAt = 42)
        assertEquals(
            ExpenditureDto("2026-11-14", 1800.0, 1600.0, 180.0, 400.0, 220.0, 2400.0, -600.0, 42),
            dto
        )
    }
}
