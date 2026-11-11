package com.mettyoung.fitbro.data.sync

import com.mettyoung.fitbro.data.health.HealthDataSource
import com.mettyoung.fitbro.data.repository.FoodDiarySyncStore
import com.mettyoung.fitbro.data.repository.MacroGoalRepository
import com.mettyoung.fitbro.util.currentEpochMs
import com.mettyoung.fitbro.util.dayOfWeekMonBased
import com.mettyoung.fitbro.util.plusDays
import com.mettyoung.fitbro.util.toYMD
import com.mettyoung.fitbro.util.todayString
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncStatus {
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data class Synced(val atEpochMs: Long) : SyncStatus
    data class Failed(val reason: SyncResult.Failure) : SyncStatus
}

class SyncRepository(
    private val api: SyncApi,
    private val store: FoodDiarySyncStore,
    private val settings: SyncSettings,
    private val healthDataSource: HealthDataSource,
    private val macroGoalRepository: MacroGoalRepository,
    private val scope: CoroutineScope,
    private val now: () -> Long = ::currentEpochMs
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow<SyncStatus>(
        settings.getLastSyncedAt()?.let { SyncStatus.Synced(it) } ?: SyncStatus.Idle
    )
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    /** Fire-and-forget; runs are serialized so overlapping requests queue up (FR-15). */
    fun requestSync() {
        scope.launch { sync() }
    }

    /** FR-13: push outbox → pull since cursor → apply → persist cursor. */
    suspend fun sync(): SyncResult<Unit> {
        if (settings.getSyncUrl().isBlank() || settings.getSyncToken().isBlank()) {
            return SyncResult.Failure.NotConfigured
        }
        return mutex.withLock {
            _status.value = SyncStatus.Syncing
            val result = try {
                runSync()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                SyncResult.Failure.Unexpected(e)
            }
            _status.value = when (result) {
                is SyncResult.Success -> SyncStatus.Synced(now()).also { settings.setLastSyncedAt(it.atEpochMs) }
                is SyncResult.Failure -> SyncStatus.Failed(result)
            }
            result
        }
    }

    private suspend fun runSync(): SyncResult<Unit> {
        var cursor = settings.getSyncCursor()
        // FR-14: first sync backfills the cloud with the whole local diary.
        if (cursor == null) store.enqueueAll()

        val pending = store.pendingChanges()
        for (batch in pending.chunked(PUSH_BATCH)) {
            when (val pushed = api.push(batch)) {
                is SyncResult.Failure -> return pushed
                is SyncResult.Success -> store.markPushed(batch)
            }
        }

        var since = cursor ?: 0L
        while (true) {
            val page = when (val pulled = api.pull(since)) {
                is SyncResult.Failure -> return pulled
                is SyncResult.Success -> pulled.value
            }
            val changed = store.applyRemoteChanges(page.changes)
            // FR-16: mirror entries that arrived from the cloud into Health Connect / HealthKit.
            changed.forEach { entry ->
                try {
                    healthDataSource.writeNutritionRecord(entry)
                } catch (e: Exception) {
                    println("Sync: health write failed for ${entry.uuid}: ${e.message}")
                }
            }
            cursor = page.cursor
            settings.setSyncCursor(cursor)
            if (page.changes.isEmpty() || page.cursor == since) break
            since = page.cursor
        }

        // Goals are best-effort context for Grok; a failure here shouldn't fail the diary sync.
        api.putGoals(goalsByWeekday())
        return SyncResult.Success(Unit)
    }

    private suspend fun goalsByWeekday(): Map<Int, WeekdayGoals> {
        val today = todayString()
        return (0 until 7).associate { offset ->
            val date = today.plusDays(offset)
            val (y, m, d) = date.toYMD()
            val profile = macroGoalRepository.getActiveProfileForDate(date)
            dayOfWeekMonBased(y, m, d) to WeekdayGoals(
                calories = profile.caloriesKcal,
                proteinG = profile.proteinG,
                carbG = profile.carbsG,
                fatG = profile.fatG
            )
        }
    }

    private companion object {
        const val PUSH_BATCH = 200
    }
}
