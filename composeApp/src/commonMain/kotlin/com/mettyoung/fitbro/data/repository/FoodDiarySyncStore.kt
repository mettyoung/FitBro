package com.mettyoung.fitbro.data.repository

import com.mettyoung.fitbro.data.model.FoodDiaryEntry

/** A local change waiting to be pushed: the current row, or a tombstone when [entry] is null. */
data class PendingChange(
    val uuid: String,
    val queuedAt: Long,
    val entry: FoodDiaryEntry?
)

sealed interface RemoteChange {
    val uuid: String
    val updatedAt: Long

    data class Upsert(val entry: FoodDiaryEntry) : RemoteChange {
        override val uuid get() = entry.uuid
        override val updatedAt get() = entry.updatedAt
    }

    data class Delete(override val uuid: String, override val updatedAt: Long) : RemoteChange
}

/** Local side of cloud sync for the food diary. */
interface FoodDiarySyncStore {
    suspend fun pendingChanges(): List<PendingChange>

    /** Clears outbox rows whose queuedAt still matches, so edits made during a push are kept. */
    suspend fun markPushed(changes: List<PendingChange>)

    /** Queues every local entry; used for the first sync to backfill the cloud. */
    suspend fun enqueueAll()

    /**
     * Applies changes pulled from the cloud with last-write-wins on updatedAt, without queueing them
     * for push. Returns the entries that were inserted or updated locally.
     */
    suspend fun applyRemoteChanges(changes: List<RemoteChange>): List<FoodDiaryEntry>
}
