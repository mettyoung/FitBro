package com.mettyoung.fitbro.data.sync

import com.mettyoung.fitbro.data.model.DailyBalance

/**
 * Days of computed expenditure waiting to be pushed, newest value per date. Skips days whose value is
 * unchanged since the last successful push, so frequent dashboard refreshes don't cause redundant uploads.
 * In-memory only: the dashboard recomputes on every open, so nothing is lost across process death.
 * Not thread-safe; callers serialize access (SyncRepository holds its mutex).
 */
internal class ExpenditureOutbox {
    private val pending = LinkedHashMap<String, Pair<DailyBalance, Long>>()
    private val lastSent = HashMap<String, DailyBalance>()

    fun offer(balances: List<DailyBalance>, at: Long) {
        for (b in balances) {
            if (lastSent[b.date] == b) pending.remove(b.date) else pending[b.date] = b to at
        }
    }

    fun pending(): List<Pair<DailyBalance, Long>> = pending.values.toList()

    /** Records a successful push; a newer value offered meanwhile stays pending. */
    fun markSent(sent: List<Pair<DailyBalance, Long>>) {
        for ((b, _) in sent) {
            lastSent[b.date] = b
            if (pending[b.date]?.first == b) pending.remove(b.date)
        }
    }
}
