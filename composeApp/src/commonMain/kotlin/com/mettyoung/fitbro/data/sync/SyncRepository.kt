package com.mettyoung.fitbro.data.sync

import com.mettyoung.fitbro.data.cache.UserSettingsDataSource
import com.mettyoung.fitbro.data.health.HealthDataSource
import com.mettyoung.fitbro.data.health.HealthResult
import com.mettyoung.fitbro.data.model.WorkoutDetail
import com.mettyoung.fitbro.data.repository.CustomFoodRepository
import com.mettyoung.fitbro.data.repository.CustomMealRepository
import kotlinx.coroutines.flow.first
import com.mettyoung.fitbro.data.model.DailyBalance
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
    private val appSettings: UserSettingsDataSource,
    private val customFoodRepository: CustomFoodRepository,
    private val customMealRepository: CustomMealRepository,
    private val scope: CoroutineScope,
    private val now: () -> Long = ::currentEpochMs
) {
    private val mutex = Mutex()
    private val _status = MutableStateFlow<SyncStatus>(
        settings.getLastSyncedAt()?.let { SyncStatus.Synced(it) } ?: SyncStatus.Idle
    )
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val expenditureOutbox = ExpenditureOutbox() // guarded by mutex
    /** Workout ids whose detail the server already has (this process); guarded by mutex. */
    private val detailSent = HashSet<String>()

    /**
     * Queues the dashboard's computed expenditure and pushes it on its own (no full diary sync), so Grok sees
     * calories burned. Unchanged days are skipped; a failed push stays queued for the next sync.
     */
    fun publishExpenditure(balances: List<DailyBalance>) {
        if (balances.isEmpty() || settings.getSyncUrl().isBlank() || settings.getSyncToken().isBlank()) return
        val at = now()
        scope.launch {
            mutex.withLock {
                expenditureOutbox.offer(balances, at)
                flushExpenditure()
            }
        }
    }

    /** Caller holds [mutex]. */
    private suspend fun flushExpenditure(): SyncResult<Unit> {
        val batch = expenditureOutbox.pending()
        if (batch.isEmpty()) return SyncResult.Success(Unit)
        return api.putExpenditure(batch).also {
            if (it is SyncResult.Success) expenditureOutbox.markSent(batch)
        }
    }

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

        // Everything below is read-only context for Grok; failures here shouldn't fail the diary sync.
        api.putSettings(goalsByWeekday(), preferences())
        // Retry anything a standalone expenditure publish couldn't push.
        flushExpenditure()
        mirrorHealth()
        api.putLibrary(libraryBody(customFoodRepository.getAllCustomFoods().first(), customMealRepository.getAllCustomMeals().first()))
        return SyncResult.Success(Unit)
    }

    private fun preferences() = PreferencesDto(
        weekStart = appSettings.getWeekStart().name,
        resistanceSessionsPerWeek = appSettings.getWeeklyResistanceTarget(),
        cardioMinutesPerWeek = appSettings.getWeeklyCardioMinutesTarget(),
        hiddenWorkoutTypes = appSettings.getHiddenWorkoutTypes().map { it.name }.sorted(),
        excludedResistanceSources = appSettings.getExcludedResistanceSources().sorted()
    )

    /**
     * Mirrors body readings and exercise sessions so MCP clients can query them. Each push replaces its date
     * range on the server, so it's skipped entirely when the read fails (e.g. permission revoked) rather than
     * wiping the cloud copy. Workout detail costs three Health Connect reads per session, so only sessions the
     * server lacks detail for are read, newest first, a few per sync. Caller holds [mutex].
     */
    private suspend fun mirrorHealth() {
        val today = todayString()
        val bodyStart = today.plusDays(-BODY_DAYS)
        val body = healthDataSource.readBodyMeasurements(bodyStart, today)
        if (body is HealthResult.Success) api.putBody(bodyBody(bodyStart, today, body.value))

        val workoutStart = today.plusDays(-WORKOUT_DAYS)
        val sessions = (healthDataSource.readExerciseSessions(workoutStart, today) as? HealthResult.Success)?.value ?: return
        val details = HashMap<String, WorkoutDetail>()
        sessions.filter { it.id !in detailSent }
            .sortedByDescending { it.startEpochMs }
            .take(DETAILS_PER_SYNC)
            .forEach { s ->
                (healthDataSource.readWorkoutDetail(s) as? HealthResult.Success)?.let { details[s.id] = it.value }
            }
        val pushed = api.putWorkouts(WorkoutsBody(workoutStart, today, sessions.map { it.toDto(details[it.id]) }))
        if (pushed is SyncResult.Success) detailSent += details.keys
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
        const val BODY_DAYS = 120
        const val WORKOUT_DAYS = 90
        const val DETAILS_PER_SYNC = 15
    }
}
